package faygolover.rpsports.server;

import faygolover.rpcore.api.BodyProvider;
import faygolover.rpcore.api.NutritionProvider;
import faygolover.rpcore.api.RpCoreAPI;
import faygolover.rpcore.api.RpIds;
import faygolover.rpsports.SportsConfig;
import faygolover.rpsports.capability.SportsData;
import faygolover.rpsports.core.Zone;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Силы зон мышц: усталость, стадии, перегрузка, восстановление (docs/00 «Силы зоны»). Восстановление тратит калории
 * и белок (RP Culinary): 6 ккал и 0,1 белка за каждый 1 %.
 */
public final class Muscles {
    private Muscles() {}

    /** Мод тела ведёт мышцы (боль, растяжение, надрыв). */
    static boolean medicine() {
        BodyProvider b = RpCoreAPI.body();
        return b != null && b.handles(RpIds.BODY_MUSCLES);
    }

    /** Ощущение: через мод тела, иначе над панелью. */
    static void sense(ServerPlayer sp, Component text) {
        BodyProvider b = RpCoreAPI.body();
        if (b != null && b.handles(RpIds.BODY_SENSATIONS) && text.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc
                && tc.getArgs().length == 0 && b.sensation(sp, tc.getKey())) return;
        sp.displayClientMessage(text.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    /** Отнять силы зоны; стадии и перегрузка. */
    static void fatigue(ServerPlayer sp, SportsData d, Zone z, double amount, long now) {
        if (amount <= 0) return;
        int i = z.ordinal();
        d.lastWorked[i] = now;
        d.strength[i] -= amount;
        if (d.strength[i] < SportsConfig.get(SportsConfig.FATIGUE_STAGE) && !d.fatigued[i]) {
            d.fatigued[i] = true;
            sense(sp, Component.translatable("rpsports.sense.tired." + z.id));
        }
        if (d.strength[i] < SportsConfig.get(SportsConfig.PAIN_STAGE) && !d.pain[i]) {
            d.pain[i] = true;
            if (medicine()) strain(sp, d, z, 1);
            else SportsService.painEffect(sp, true);
            sense(sp, Component.translatable("rpsports.sense.pain." + z.id));
        }
        if (d.strength[i] < 0) {
            d.overload[i] += -d.strength[i];
            d.strength[i] = 0;
            if (medicine()) {
                // Хрупкий получает растяжение и надрыв раньше.
                double k = RpCoreAPI.hasTrait(sp, RpIds.FRAGILE) ? 0.6 : 1.0;
                if (d.overload[i] >= SportsConfig.get(SportsConfig.STRAIN_3) * k) strain(sp, d, z, 3);
                else if (d.overload[i] >= SportsConfig.get(SportsConfig.STRAIN_2) * k) strain(sp, d, z, 2);
            } else {
                // Без мода тела травм нет: мышцы просто отказывают, упражнение кончается.
                SportsService.stop(sp, null);
                sp.displayClientMessage(Component.translatable("rpsports.msg.muscle_fail", Component.translatable(z.key()))
                        .withStyle(ChatFormatting.RED), true);
            }
        }
    }

    private static void strain(ServerPlayer sp, SportsData d, Zone z, int stage) {
        int i = z.ordinal();
        if (d.strainSent[i] >= stage) return;
        d.strainSent[i] = stage;
        BodyProvider b = RpCoreAPI.body();
        if (b == null) return;
        if (stage >= 2) {
            // Растяжение и надрыв — одна часть тела (рука, на которой нагрузка; у ног — случайная нога).
            ResourceLocation part = z.parts.get(sp.getRandom().nextInt(z.parts.size()));
            b.strain(sp, part, stage);
            sense(sp, Component.translatable(stage >= 3 ? "rpsports.sense.tear" : "rpsports.sense.strain"));
        } else {
            for (ResourceLocation part : z.parts) b.strain(sp, part, stage);
        }
    }

    /**
     * Восстановление за {@code seconds}: зона, которая не работала последние 5 секунд, растёт на 100 % за
     * {@code recovery_hours}; во сне и у Любителя спорта — быстрее, с белком — быстрее, без белка — медленнее.
     */
    public static void recover(ServerPlayer sp, SportsData d, double seconds, boolean sleeping, long now) {
        double rate = 100.0 / (SportsConfig.get(SportsConfig.RECOVERY_HOURS) * 3600.0);
        if (sleeping) rate *= SportsConfig.get(SportsConfig.SLEEP_MULT);
        if (RpCoreAPI.hasTrait(sp, RpIds.GYM_LOVER)) rate *= SportsConfig.get(SportsConfig.GYM_LOVER_RECOVERY);
        NutritionProvider n = RpCoreAPI.nutrition();
        if (n != null) {
            double protein = n.nutrient(sp, NutritionProvider.PROTEIN);
            if (Double.isFinite(protein)) {
                if (protein < 20) rate *= SportsConfig.get(SportsConfig.PROTEIN_LOW_RECOVERY);
                else if (protein >= 35) rate *= SportsConfig.get(SportsConfig.PROTEIN_GOOD_RECOVERY);
            }
            // Голодный не восстанавливается: очень голоден — вдвое медленнее.
            if (n.hungerStage(sp) >= 2) rate *= 0.5;
        }
        double recovered = 0;
        boolean anyPain = false;
        for (Zone z : Zone.VALUES) {
            int i = z.ordinal();
            if (now - d.lastWorked[i] < 100 && seconds <= 5) continue;
            if (d.strength[i] < 100) {
                double add = Math.min(100 - d.strength[i], rate * seconds);
                d.strength[i] += add;
                recovered += add;
                if (d.overload[i] > 0) d.overload[i] = Math.max(0, d.overload[i] - rate * seconds);
            }
            if (d.strength[i] >= 100) {
                d.strength[i] = 100;
                d.fatigued[i] = false;
                d.pain[i] = false;
                d.overload[i] = 0;
                d.strainSent[i] = 0;
            }
            anyPain |= d.pain[i];
        }
        if (!medicine()) SportsService.painEffect(sp, anyPain);
        // Восстановление мышц — калории и белок.
        d.recoveredPercent += recovered;
        if (n != null && d.recoveredPercent >= 1) {
            double pct = d.recoveredPercent;
            d.recoveredPercent = 0;
            n.burnKcal(sp, pct * SportsConfig.get(SportsConfig.KCAL_PER_PERCENT));
            n.consumeNutrient(sp, NutritionProvider.PROTEIN, pct * SportsConfig.get(SportsConfig.PROTEIN_PER_PERCENT));
        } else if (n == null) {
            d.recoveredPercent = 0;
        }
    }

    /** Обычная нагрузка (RP Stamina): бег и лестницы — ноги, плавание — всё, добыча — руки и пресс, удары — рука. */
    public static void ordinary(ServerPlayer sp, ResourceLocation source, float amount) {
        if (amount <= 0 || !"rpstamina".equals(source.getNamespace())) return;
        SportsData d = SportsData.get(sp);
        if (d == null) return;
        double k = amount / 100.0 * SportsConfig.get(SportsConfig.ORDINARY_FATIGUE)
                * SportsConfig.fitnessFatigue(SportsService.level(sp, RpIds.FITNESS)) / 1.2;
        long now = sp.level().getGameTime();
        Zone hand = sp.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? Zone.RIGHT_ARM : Zone.LEFT_ARM;
        switch (source.getPath()) {
            case "sprint", "sprint_jump", "jump", "climb" -> quiet(d, Zone.LEGS, k, now);
            case "swim", "swim_sprint" -> {
                quiet(d, Zone.LEGS, k * 0.6, now);
                quiet(d, Zone.LEFT_ARM, k * 0.6, now);
                quiet(d, Zone.RIGHT_ARM, k * 0.6, now);
            }
            case "mine_tick", "mine_break" -> {
                quiet(d, hand, k, now);
                quiet(d, Zone.ABS, k * 0.3, now);
            }
            case "melee_hit" -> quiet(d, hand, k * 1.3, now);
            default -> { }
        }
    }

    /** Тихая усталость от обычной нагрузки: не ниже стадии «Усталость», без сообщений (её почти не заметно). */
    private static void quiet(SportsData d, Zone z, double amount, long now) {
        int i = z.ordinal();
        double floor = SportsConfig.get(SportsConfig.FATIGUE_STAGE) + 5;
        if (d.strength[i] <= floor) return;
        d.strength[i] = Math.max(floor, d.strength[i] - amount);
        d.lastWorked[i] = now;
    }
}
