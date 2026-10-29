package faygolover.rpsports.server;

import faygolover.rpcore.api.BodyProvider;
import faygolover.rpcore.api.NutritionProvider;
import faygolover.rpcore.api.RpCoreAPI;
import faygolover.rpcore.api.RpIds;
import faygolover.rpcore.api.StaminaProvider;
import faygolover.rpsports.RpSports;
import faygolover.rpsports.SportsConfig;
import faygolover.rpsports.block.BenchBlock;
import faygolover.rpsports.block.BenchBlockEntity;
import faygolover.rpsports.capability.SportsData;
import faygolover.rpsports.core.Exercise;
import faygolover.rpsports.core.Zone;
import faygolover.rpsports.entity.SeatEntity;
import faygolover.rpsports.item.PlateItem;
import faygolover.rpsports.network.SportsNet;
import faygolover.rpsports.registry.SportsRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import faygolover.rpsports.block.MachineBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Тренировки на сервере (RP Stamina Sports docs/00):
 * <ul>
 *   <li>начать — ПКМ по тренажёру, ПКМ гантелью, клавиша «Упражнения»; закончить — Ctrl, отойти, слезть;</li>
 *   <li>повтор: выносливость (RP Stamina), усталость зон, опыт Силы и Телосложения за каждый повтор (RP Core → Perks);</li>
 *   <li>силы зон: скрытая «Усталость» ниже 30 %, «Боль в мышцах» ниже 10 % (держатся до полного восстановления),
 *       перегрузка «сверх нуля» — растяжение и надрыв (RP Medicine);</li>
 *   <li>кончилась выносливость — плохое событие ({@link BadEvents}).</li>
 * </ul>
 */
public final class SportsService {
    private SportsService() {}

    static final Map<UUID, Session> SESSIONS = new HashMap<>();
    /** Выбранная программа дорожки (Shift+ПКМ по дорожке). */
    private static final Map<UUID, Integer> PROGRAMS = new HashMap<>();

    public static ResourceLocation source(Exercise e) {
        return new ResourceLocation(RpSports.MODID, e.id);
    }

    // ------------------------------------------------------------------ уровни и вес

    static int level(ServerPlayer sp, ResourceLocation skill) {
        return RpCoreAPI.skill(sp, skill).orElse(3);
    }

    static double bodyKg(ServerPlayer sp) {
        double w = RpCoreAPI.weightKg(sp);
        return Double.isFinite(w) && w > 20 ? w : 75;
    }

    /** «Свой» вес штанги или гантели по Силе. */
    static double ownWeight(Exercise e, int strength) {
        if (e == Exercise.BENCH) return SportsConfig.get(SportsConfig.BAR_BASE) + SportsConfig.get(SportsConfig.BAR_PER_STRENGTH) * strength;
        if (e == Exercise.DUMBBELL) return SportsConfig.get(SportsConfig.DUMBBELL_BASE) + SportsConfig.get(SportsConfig.DUMBBELL_PER_STRENGTH) * strength;
        return 1;
    }

    /** Польза от отношения веса (docs/00, «Вес снаряда»). */
    static double weightEfficiency(double ratio) {
        if (ratio < 0.5) return 0.2;
        if (ratio < 0.9) return 0.4 + (ratio - 0.5) / 0.4 * 0.5;
        if (ratio <= 1.4) return 1.0;
        return 0.6;
    }

    /** Перегруз жима и гантелей (отношение выше 1): выносливость сверху за каждую попытку, ~квадрат перегруза (docs/10). */
    static double overloadStamina(Exercise e, double ratio) {
        if (!e.weighted() || ratio <= 1) return 0;
        return Math.min(SportsConfig.get(SportsConfig.OVERLOAD_MAX), SportsConfig.get(SportsConfig.OVERLOAD_STAMINA)
                * Math.pow(ratio - 1, SportsConfig.get(SportsConfig.OVERLOAD_EXP)));
    }

    // ------------------------------------------------------------------ начало

    public static boolean benchBusy(Level level, BlockPos pos) {
        for (Session s : SESSIONS.values()) if (s.exercise == Exercise.BENCH && pos.equals(s.pos)) return true;
        return false;
    }

    public static void useMachine(ServerPlayer sp, BlockPos pos, BlockState state, Exercise e, boolean shift) {
        if (SESSIONS.containsKey(sp.getUUID())) return;
        if (e == Exercise.TREADMILL && shift) {
            int p = (PROGRAMS.getOrDefault(sp.getUUID(), 1) + 1) % 3;
            PROGRAMS.put(sp.getUUID(), p);
            sp.displayClientMessage(Component.translatable("rpsports.msg.program", Component.translatable("rpsports.program." + p)), true);
            return;
        }
        if (e == Exercise.BENCH && shift) {
            // Shift+ПКМ — окно блинов (пока на скамье никто не жмёт).
            if (benchBusy(sp.level(), pos)) {
                sp.displayClientMessage(Component.translatable("rpsports.msg.bench_busy").withStyle(ChatFormatting.GRAY), true);
            } else if (sp.level().getBlockEntity(pos) instanceof BenchBlockEntity be) {
                net.minecraftforge.network.NetworkHooks.openScreen(sp, be, pos);
            }
            return;
        }
        if (e == Exercise.BENCH) {
            for (var en : SESSIONS.entrySet()) {
                Session s = en.getValue();
                if (s.exercise == Exercise.BENCH && pos.equals(s.pos)) {
                    // Скамья занята — встать на страховку.
                    s.spotter = sp.getUUID();
                    sp.displayClientMessage(Component.translatable("rpsports.msg.spotting").withStyle(ChatFormatting.GREEN), true);
                    ServerPlayer lifter = sp.server.getPlayerList().getPlayer(en.getKey());
                    if (lifter != null) lifter.displayClientMessage(Component.translatable("rpsports.msg.spotted", sp.getDisplayName()), true);
                    return;
                }
            }
        }
        if (e == Exercise.BENCH && sp.level().getBlockEntity(pos) instanceof BenchBlockEntity be && !be.balanced()) {
            sp.displayClientMessage(Component.translatable("rpsports.msg.unbalanced").withStyle(ChatFormatting.GOLD), true);
            return;
        }
        double kg = e == Exercise.BENCH ? BenchBlock.weightAt(sp.level(), pos) : 0;
        start(sp, e, pos, InteractionHand.MAIN_HAND, kg, PROGRAMS.getOrDefault(sp.getUUID(), 1));
    }

    /** ПКМ гантелью: одна — той рукой; в обеих руках по гантели — обе (Q — левая, E — правая). */
    public static void useDumbbell(ServerPlayer sp, InteractionHand hand, int kg) {
        if (SESSIONS.containsKey(sp.getUUID())) return;
        if (sp.getMainHandItem().getItem() instanceof faygolover.rpsports.item.DumbbellItem main
                && sp.getOffhandItem().getItem() instanceof faygolover.rpsports.item.DumbbellItem off) {
            DUAL_OFF_KG.put(sp.getUUID(), (double) off.kg());
            start(sp, Exercise.DUMBBELL, null, InteractionHand.MAIN_HAND, main.kg(), 0);
            DUAL_OFF_KG.remove(sp.getUUID());
            return;
        }
        start(sp, Exercise.DUMBBELL, null, hand, kg, 0);
    }

    /** Вес второй гантели на время start() (две гантели). */
    private static final Map<UUID, Double> DUAL_OFF_KG = new HashMap<>();

    public static void startBodyweight(ServerPlayer sp, Exercise e) {
        if (e.kind != Exercise.Kind.BODYWEIGHT || SESSIONS.containsKey(sp.getUUID())) return;
        if (!sp.onGround() || sp.isPassenger() || sp.isInWater()) {
            sp.displayClientMessage(Component.translatable("rpsports.msg.need_ground").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        start(sp, e, null, InteractionHand.MAIN_HAND, 0, 0);
    }

    /** Зоны упражнения (с рабочей рукой гантели). */
    static Map<Zone, Double> zones(Session s) {
        Map<Zone, Double> m = new java.util.EnumMap<>(s.exercise.fatigue);
        if (s.exercise == Exercise.DUMBBELL) m.put(s.workHand() == InteractionHand.MAIN_HAND ? Zone.RIGHT_ARM : Zone.LEFT_ARM, 0.2);
        return m;
    }

    private static void start(ServerPlayer sp, Exercise e, @Nullable BlockPos pos, InteractionHand hand, double kg, int program) {
        if (sp.isSpectator()) return;
        // С модом тела: перелом, сильная боль, нет руки — нельзя.
        BodyProvider body = RpCoreAPI.body();
        Double dualKg = e == Exercise.DUMBBELL ? DUAL_OFF_KG.get(sp.getUUID()) : null;
        Map<Zone, Double> worked = e != Exercise.DUMBBELL ? e.fatigue : dualKg != null
                ? Map.of(Zone.RIGHT_ARM, 0.2, Zone.LEFT_ARM, 0.2)
                : Map.of(hand == InteractionHand.MAIN_HAND ? Zone.RIGHT_ARM : Zone.LEFT_ARM, 0.2);
        if (body != null) {
            for (Zone z : worked.keySet()) {
                for (ResourceLocation part : z.parts) {
                    if (!body.canExert(sp, part)) {
                        sp.displayClientMessage(Component.translatable("rpsports.msg.cannot_exert", Component.translatable(z.key()))
                                .withStyle(ChatFormatting.RED), true);
                        return;
                    }
                }
            }
        }
        SportsData d = SportsData.get(sp);
        if (d == null) return;
        // Без мода тела «Боль в мышцах» запрещает нагружать зону, пока не отдохнёт.
        if (body == null || !body.handles(RpIds.BODY_MUSCLES)) {
            for (Zone z : worked.keySet()) {
                if (d.pain[z.ordinal()] && d.strength[z.ordinal()] <= 0) {
                    sp.displayClientMessage(Component.translatable("rpsports.msg.muscle_fail", Component.translatable(z.key()))
                            .withStyle(ChatFormatting.RED), true);
                    return;
                }
            }
        }
        int str = level(sp, RpIds.STRENGTH), fit = level(sp, RpIds.FITNESS);
        double ratio = 1;
        if (e.weighted()) ratio = kg / Math.max(1, ownWeight(e, str));
        else if (e.bodyweight()) ratio = bodyKg(sp) / 75.0;
        int clothing = 0, heavy = 0;
        var list = SportsConfig.get(SportsConfig.CLOTHING);
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack st = sp.getItemBySlot(slot);
            if (st.isEmpty()) continue;
            var id = ForgeRegistries.ITEMS.getKey(st.getItem());
            if (id != null && list.contains(id.toString())) clothing++;
            else heavy++;
        }
        double ease = 1;
        if (RpCoreAPI.hasTrait(sp, RpIds.AGILE)) ease *= 1.1;
        if (RpCoreAPI.hasTrait(sp, RpIds.CLUMSY)) ease *= 0.9;
        ease *= Math.max(0.4, 1 - heavy * SportsConfig.get(SportsConfig.ARMOR_DIFFICULTY));
        boolean onMat = e.kind == Exercise.Kind.BODYWEIGHT && (sp.level().getBlockState(sp.blockPosition()).is(SportsRegistry.GYM_MAT.get())
                || sp.level().getBlockState(sp.blockPosition().below()).is(SportsRegistry.GYM_MAT.get()));
        Session s = new Session(e, pos, hand, kg, ratio, str, fit, ease, program, onMat, clothing, heavy);
        if (dualKg != null) {
            s.dual = true;
            s.weightKg2 = dualKg;
            s.ratio2 = dualKg / Math.max(1, ownWeight(e, str));
        }
        s.started = sp.level().getGameTime();
        s.lastRep = s.started;
        if (!place(sp, s)) {
            sp.displayClientMessage(Component.translatable("rpsports.msg.no_room").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        SESSIONS.put(sp.getUUID(), s);
        // В творческом упражнения тоже тратят выносливость (RP Stamina 1.4.1) — тег снимется, когда она восстановится.
        if (sp.isCreative()) sp.addTag(FORCE_STAMINA);
        SportsNet.send(sp, new SportsNet.Open(e.ordinal(), (float) ratio, str, fit, (float) ease, (float) kg,
                s.dual ? 2 : hand == InteractionHand.MAIN_HAND ? 0 : 1, program, (float) s.weightKg2, (float) s.ratio2,
                (float) d.record(e)));
        if (ratio > 1.4 && e.weighted())
            sp.displayClientMessage(Component.translatable("rpsports.msg.too_heavy").withStyle(ChatFormatting.GOLD), true);
        SportsNet.toTrackers(sp, new SportsNet.Anim(sp.getId(), e.ordinal(), 0, 0));
    }

    /**
     * Поставить игрока на место упражнения и повернуть как надо; дальше он стоит там, пока не закончит:
     * <ul>
     *   <li>скамья — сидит на невидимом сиденье на подушке, ногами от стойки (голова под грифом);</li>
     *   <li>турник — под перекладиной, лицом туда, откуда его ставили; дорожка — на ленте лицом к поручню;</li>
     *   <li>груша — на блок перед ней, лицом к ней; без снаряда — где стоял;</li>
     *   <li>гантели — ходить можно.</li>
     * </ul>
     */
    private static boolean place(ServerPlayer sp, Session s) {
        if (s.exercise == Exercise.DUMBBELL) return true;
        Level level = sp.level();
        BlockPos pos = s.pos;
        Direction facing = pos != null && level.getBlockState(pos).hasProperty(MachineBlock.FACING)
                ? level.getBlockState(pos).getValue(MachineBlock.FACING) : null;
        double x = sp.getX(), y = sp.getY(), z = sp.getZ();
        float yaw = sp.getYRot();
        if (pos != null && facing != null) {
            double cx = pos.getX() + 0.5, cz = pos.getZ() + 0.5;
            switch (s.exercise) {
                case BENCH -> {
                    // Скамья в два блока: основание — со стойкой, второй блок — под ноги (по часовой от FACING).
                    Direction feet = facing.getClockWise();
                    x = cx + feet.getStepX() * BENCH_SHIFT;
                    z = cz + feet.getStepZ() * BENCH_SHIFT;
                    yaw = feet.toYRot();
                }
                case PULLUP -> {
                    // Турник 2 блока в ширину — висит посередине, между блоками.
                    Direction right = facing.getClockWise();
                    x = cx + right.getStepX() * 0.5;
                    z = cz + right.getStepZ() * 0.5;
                    y = pos.getY();
                    yaw = facing.toYRot();
                }
                case TREADMILL -> {
                    // Дорожка 2 блока вдоль: бежит вправо (по часовой от FACING), к поручню на втором блоке.
                    Direction run = facing.getClockWise();
                    x = cx + run.getStepX() * 0.45;
                    z = cz + run.getStepZ() * 0.45;
                    y = pos.getY() + 3 / 16.0;
                    yaw = run.toYRot();
                }
                case BAG -> {
                    // Встать на блок перед грушей — только если там можно нормально стоять.
                    BlockPos spot = pos.relative(facing);
                    boolean free = level.getBlockState(spot).getCollisionShape(level, spot).isEmpty()
                            && level.getBlockState(spot.above()).getCollisionShape(level, spot.above()).isEmpty();
                    boolean floor = level.getBlockState(spot.below()).isFaceSturdy(level, spot.below(), Direction.UP);
                    if (!free || !floor) return false;
                    x = spot.getX() + 0.5;
                    z = spot.getZ() + 0.5;
                    y = spot.getY();
                    yaw = facing.getOpposite().toYRot();
                }
                default -> { }
            }
        }
        s.yaw = yaw;
        sp.connection.teleport(x, y, z, yaw, 0);
        sp.setYHeadRot(yaw);
        sp.setYBodyRot(yaw);
        if (s.exercise == Exercise.BENCH && pos != null) {
            SeatEntity seat = SeatEntity.create(level, x, pos.getY() + SEAT_Y, z);
            seat.setYRot(yaw);
            level.addFreshEntity(seat);
            sp.startRiding(seat, true);
            s.seatId = seat.getId();
        } else {
            s.anchor = new net.minecraft.world.phys.Vec3(x, y, z);
        }
        return true;
    }

    /** Повтор «упал» у приседаний (клиент — SquatGame.FELL). */
    private static final int SQUAT_FELL = 9;

    /** Тег RP Stamina: выносливость считается и в творческом. */
    private static final String FORCE_STAMINA = "rpstamina.force";

    /** Скамья: насколько сиденье сдвинуто от центра блока со стойкой к ногам (плечи — под грифом). */
    private static final double BENCH_SHIFT = 0.87;
    /** Скамья: высота сиденья над блоком — спина ложится на подушку (верх подушки на 8/16). */
    public static final double SEAT_Y = 0.267;

    /** Каждый тик: держать игрока на месте упражнения (отбросило, толкнули — вернуть). */
    public static void tick(ServerPlayer sp) {
        Session s = SESSIONS.get(sp.getUUID());
        if (s == null || s.failed) return;
        StaminaProvider stamina = RpCoreAPI.stamina();
        if (stamina != null) {
            // Траты повторов — плавно, примерно за секунду.
            if (s.pendingStamina > 0) {
                double chunk = s.pendingStamina < 0.5 ? s.pendingStamina : Math.max(0.5, s.pendingStamina * 0.12);
                s.pendingStamina -= chunk;
                stamina.spend(sp, (float) chunk, source(s.exercise));
            }
            // Дорожка: темп с клиента (0–1): ходьба → бег → рывок — каждый тик.
            if (s.exercise == Exercise.TREADMILL && s.a > 0.05) {
                double tempo = s.a;
                double perSecond = tempo < 0.45 ? SportsConfig.get(SportsConfig.TREAD_WALK)
                        : tempo < 0.8 ? SportsConfig.get(SportsConfig.TREAD_RUN) : SportsConfig.get(SportsConfig.TREAD_SPRINT);
                stamina.spend(sp, (float) (perSecond / 20 * s.armorMult(SportsConfig.get(SportsConfig.ARMOR_PENALTY))), source(Exercise.TREADMILL));
            }
        }
        if (s.anchor == null) return;
        if (sp.position().distanceToSqr(s.anchor) > 0.04)
            sp.connection.teleport(s.anchor.x, s.anchor.y, s.anchor.z, sp.getYRot(), sp.getXRot());
        if (sp.isShiftKeyDown()) sp.setShiftKeyDown(false);
    }

    // ------------------------------------------------------------------ повтор

    public static void onRep(ServerPlayer sp, boolean success, int extra) {
        Session s = SESSIONS.get(sp.getUUID());
        SportsData d = SportsData.get(sp);
        if (s == null || d == null || s.failed) return;
        long now = sp.level().getGameTime();
        // Груша: удары идут нотами (иногда «двойкой») — между ударами не меньше 3 тиков.
        double minTicks = s.exercise == Exercise.BAG ? 3
                : s.exercise.repSeconds * 20 * (s.exercise == Exercise.TREADMILL ? 0.8 : 0.35);
        // Две гантели: у каждой руки свой счёт (extra: 0 — основная, 1 — вторая).
        int handIdx = s.dual && extra == 1 ? 1 : 0;
        long last = s.dual ? s.lastRepHand[handIdx] : s.lastRep;
        if (now - last < minTicks) return; // слишком часто — не считаем (макросы)
        s.lastRep = now;
        s.lastRepHand[handIdx] = now;
        if (s.dual) s.lastHand = handIdx == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        double ratio = s.dual && handIdx == 1 ? s.ratio2 : s.ratio;
        double kg = s.dual && handIdx == 1 ? s.weightKg2 : s.weightKg;
        var cfg = SportsConfig.EXERCISES.get(s.exercise);
        double armor = s.armorMult(SportsConfig.get(SportsConfig.ARMOR_PENALTY));
        double load = s.exercise.weighted() || s.exercise.bodyweight() ? Math.max(0.3, ratio) : 1;

        // Выносливость: повтор (дорожка — по темпу, каждую секунду в тике).
        StaminaProvider stamina = RpCoreAPI.stamina();
        double cost = (cfg.stamina().get() * load + overloadStamina(s.exercise, ratio)) * armor;
        if (s.exercise == Exercise.BAG && RpCoreAPI.hasTrait(sp, RpIds.MELEE_FIGHTER)) cost *= 0.8;
        if (stamina != null && cost > 0) s.pendingStamina += cost;

        // Усталость зон.
        Map<Zone, Double> zones = zones(s);
        if (s.exercise == Exercise.BAG) zones.put(extra == 0 ? Zone.LEFT_ARM : Zone.RIGHT_ARM, 0.1);
        double fat = cfg.fatigue().get() * SportsConfig.fitnessFatigue(s.fitness) * armor
                * (s.exercise.weighted() ? Math.max(0.3, ratio) : 1) * (success ? 1 : SportsConfig.get(SportsConfig.ERROR_FATIGUE));
        double zoneXp = 1;
        for (var z : zones.entrySet()) {
            int i = z.getKey().ordinal();
            zoneXp = Math.min(zoneXp, d.pain[i] ? 0 : d.fatigued[i] ? SportsConfig.get(SportsConfig.FATIGUE_XP) : 1);
            Muscles.fatigue(sp, d, z.getKey(), z.getValue() * fat, now);
        }
        if (!SESSIONS.containsKey(sp.getUUID())) return; // мышцы отказали — упражнение уже закончено

        // Опыт за удачный повтор.
        if (success) {
            s.reps++;
            s.series++;
            s.bestSeries = Math.max(s.bestSeries, s.series);
            s.volume += s.exercise.weighted() ? kg : 1;
            double mult = zoneXp;
            if (s.exercise.weighted()) mult *= weightEfficiency(ratio);
            mult *= 1 + s.clothing * SportsConfig.get(SportsConfig.CLOTHING_XP);
            if (s.onMat) mult *= 1 + SportsConfig.get(SportsConfig.MAT_XP);
            if (RpCoreAPI.hasTrait(sp, RpIds.GYM_LOVER)) mult *= SportsConfig.get(SportsConfig.GYM_LOVER_XP);
            if (s.exercise == Exercise.TREADMILL) mult *= switch (s.program) { case 0 -> 0.4; case 2 -> 1.2; default -> 1.0; };
            if (mult > 0) {
                RpCoreAPI.grantXp(sp, RpIds.STRENGTH, cfg.xpStrength().get() * mult);
                RpCoreAPI.grantXp(sp, RpIds.FITNESS, cfg.xpFitness().get() * mult);
                if (s.exercise.xpMelee > 0 && level(sp, RpIds.MELEE) < SportsConfig.get(SportsConfig.MELEE_XP_MAX_LEVEL))
                    RpCoreAPI.grantXp(sp, RpIds.MELEE, s.exercise.xpMelee * mult);
            }
        } else {
            s.errors++;
            s.series = 0;
        }
        d.afterburnUntil = now + (long) (SportsConfig.get(SportsConfig.AFTERBURN_HOURS) * 72000);
        // Приседания: долго без равновесия — упал, упражнение кончилось.
        if (!success && s.exercise == Exercise.SQUAT && extra == SQUAT_FELL) {
            sp.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 3, false, false, false));
            stop(sp, "rpsports.bad.squat_fall");
            return;
        }

        // Выносливость кончилась — плохое событие.
        afterLoad(sp, s, d, now);
    }

    /** После нагрузки: дрожь, плохое событие при нуле. */
    static void afterLoad(ServerPlayer sp, Session s, SportsData d, long now) {
        StaminaProvider stamina = RpCoreAPI.stamina();
        double frac = stamina == null ? 1 : stamina.stamina(sp) / Math.max(1f, stamina.maxStamina(sp));
        double badAt = RpCoreAPI.hasTrait(sp, RpIds.UNLUCKY) ? SportsConfig.get(SportsConfig.UNLUCKY_STAMINA) : 0.005;
        if (stamina != null && (stamina.exhausted(sp) || frac <= badAt)) {
            s.failed = true;
            BadEvents.trigger(sp, s, d);
            return;
        }
        SportsNet.send(sp, new SportsNet.Status((float) trembleLevel(s, d, frac), false, s.reps));
    }

    /**
     * Сила дрожи 0…1: появляется, как только устал (выносливость ниже порога или зона ниже «Усталости»), и плавно
     * растёт; 1 — только когда выжат совсем (выносливость на нуле).
     */
    static double trembleLevel(Session s, SportsData d, double frac) {
        double t = SportsConfig.get(SportsConfig.TREMBLE_STAMINA);
        double level = frac < t ? (t - frac) / t : 0;
        double tired = SportsConfig.get(SportsConfig.FATIGUE_STAGE);
        for (Zone z : zones(s).keySet()) {
            double str = d.strength[z.ordinal()];
            if (str < tired) level = Math.max(level, 0.15 + 0.5 * (tired - str) / tired);
        }
        return Math.max(0, Math.min(1, level));
    }

    public static void onProgress(ServerPlayer sp, float a, float b) {
        Session s = SESSIONS.get(sp.getUUID());
        if (s == null) return;
        s.a = Math.max(0, Math.min(2, a));
        s.b = Math.max(-3, Math.min(3, b));
        long now = sp.level().getGameTime();
        if (now - s.lastRelay < 2) return;
        s.lastRelay = now;
        SportsNet.toTrackers(sp, new SportsNet.Anim(sp.getId(), s.exercise.ordinal(), s.a, s.b));
    }

    // ------------------------------------------------------------------ конец

    public static void stop(ServerPlayer sp, @Nullable String reason) {
        Session s = SESSIONS.remove(sp.getUUID());
        if (s == null) return;
        StaminaProvider st = RpCoreAPI.stamina();
        if (st != null && s.pendingStamina > 0) st.spend(sp, (float) s.pendingStamina, source(s.exercise));
        SportsData data = SportsData.get(sp);
        if (data != null) {
            // Рекорд — лучшая серия удачных повторов подряд за подход.
            double value = s.bestSeries;
            // Общий топ сервера (доска рекордов, /rpsports records).
            if (value > 0) Records.get(sp.server).submit(sp, s.exercise, value);
            if (value > 0 && value > data.record(s.exercise)) {
                boolean had = data.record(s.exercise) > 0;
                data.setRecord(s.exercise, value);
                if (had) sp.sendSystemMessage(Component.translatable("rpsports.msg.record",
                        Component.translatable(s.exercise.key()), PlateItem.format(value)).withStyle(ChatFormatting.GOLD));
            }
        }
        if (s.seatId != null) {
            Entity seat = sp.level().getEntity(s.seatId);
            if (sp.getVehicle() == seat) sp.stopRiding();
            if (seat != null) seat.discard();
        }
        SportsNet.send(sp, new SportsNet.Stopped(reason == null ? "" : reason));
        SportsNet.toTrackers(sp, new SportsNet.Anim(sp.getId(), -1, 0, 0));
        if (reason != null && !reason.isEmpty())
            sp.displayClientMessage(Component.translatable(reason).withStyle(ChatFormatting.GRAY), true);
    }

    @Nullable
    static Session session(ServerPlayer sp) {
        return SESSIONS.get(sp.getUUID());
    }

    // ------------------------------------------------------------------ тик раз в секунду

    public static void secondTick(ServerPlayer sp) {
        SportsData d = SportsData.get(sp);
        if (d == null) return;
        long now = sp.level().getGameTime();
        Session s = SESSIONS.get(sp.getUUID());
        if (s != null) sessionTick(sp, s, d, now);
        else if (sp.getTags().contains(FORCE_STAMINA)) {
            // Творческий после тренировки: тратить больше нечему (RP Stamina 1.4.2 пускает только траты тренировки),
            // выносливость быстро набирается — за несколько секунд (находка 110).
            StaminaProvider st = RpCoreAPI.stamina();
            if (st == null || !sp.isCreative() || st.stamina(sp) >= st.maxStamina(sp) - 0.5f) sp.removeTag(FORCE_STAMINA);
            else st.restore(sp, st.maxStamina(sp) * 0.2f, new ResourceLocation(RpSports.MODID, "rest"));
        }
        // Время RP стоит (/time pause) — мышцы не отходят, как и раны.
        if (RpCoreAPI.timeRunning()) Muscles.recover(sp, d, 1, sp.isSleeping(), now);
        // Дожигание после тренировки.
        NutritionProvider n = RpCoreAPI.nutrition();
        if (n != null && now < d.afterburnUntil) n.burnKcal(sp, SportsConfig.get(SportsConfig.AFTERBURN_KCAL));
    }

    private static void sessionTick(ServerPlayer sp, Session s, SportsData d, long now) {
        if (!sp.isAlive()) {
            stop(sp, null);
            return;
        }
        if (s.pos != null) {
            double dist = sp.position().distanceToSqr(s.pos.getX() + 0.5, s.pos.getY(), s.pos.getZ() + 0.5);
            boolean gone = !(sp.level().getBlockState(s.pos).getBlock() instanceof faygolover.rpsports.block.MachineBlock);
            if (gone || dist > 2.6 * 2.6 + (s.exercise == Exercise.BAG ? 2 : 0)) {
                stop(sp, "rpsports.msg.walked_away");
                return;
            }
        }
        if (s.seatId != null && sp.getVehicle() == null && !s.failed) {
            stop(sp, null);
            return;
        }
        if (s.exercise == Exercise.DUMBBELL && (!(sp.getItemInHand(s.hand).getItem() instanceof faygolover.rpsports.item.DumbbellItem)
                || s.dual && !(sp.getOffhandItem().getItem() instanceof faygolover.rpsports.item.DumbbellItem))) {
            stop(sp, null);
            return;
        }
        if (s.spotter != null) {
            ServerPlayer sp2 = sp.server.getPlayerList().getPlayer(s.spotter);
            if (sp2 == null || s.pos == null || sp2.position().distanceToSqr(s.pos.getX() + 0.5, s.pos.getY(), s.pos.getZ() + 0.5) > 9) {
                s.spotter = null;
                if (sp2 != null) sp2.displayClientMessage(Component.translatable("rpsports.msg.spot_left"), true);
            }
        }
        // Раз в секунду: дрожь и плохое событие по текущей выносливости (траты идут плавно, по тикам).
        if (!s.failed) afterLoad(sp, s, d, now);
        if (BadEvents.tick(sp, s, d, now)) stop(sp, null);
    }

    /** Выход игрока, смена измерения, смерть — упражнение кончается. */
    public static void forget(ServerPlayer sp) {
        stop(sp, null);
    }

    /** Эффект «Боль в мышцах» без мода тела — видимый (значок). */
    static void painEffect(ServerPlayer sp, boolean on) {
        var eff = SportsRegistry.MUSCLE_PAIN.get();
        if (on) {
            MobEffectInstance cur = sp.getEffect(eff);
            if (cur == null || cur.getDuration() < 100) sp.addEffect(new MobEffectInstance(eff, 400, 0, false, false, true));
        } else if (sp.hasEffect(eff)) {
            sp.removeEffect(eff);
        }
    }

    /** Отладка и ГМ: снять усталость. */
    public static void rest(ServerPlayer sp) {
        SportsData d = SportsData.get(sp);
        if (d == null) return;
        java.util.Arrays.fill(d.strength, 100);
        java.util.Arrays.fill(d.fatigued, false);
        java.util.Arrays.fill(d.pain, false);
        java.util.Arrays.fill(d.overload, 0);
        java.util.Arrays.fill(d.strainSent, 0);
        painEffect(sp, false);
    }

    static boolean benchHasSpotter(Session s, ServerPlayer sp) {
        return s.spotter != null && sp.server.getPlayerList().getPlayer(s.spotter) != null;
    }

    static double benchKg(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BenchBlockEntity b ? b.totalKg() : BenchBlock.BAR_KG;
    }
}
