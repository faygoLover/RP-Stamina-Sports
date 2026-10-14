package faygolover.rpsports.server;

import faygolover.rpcore.api.BodyProvider;
import faygolover.rpcore.api.RpCoreAPI;
import faygolover.rpcore.api.RpIds;
import faygolover.rpsports.capability.SportsData;
import faygolover.rpsports.core.Zone;
import faygolover.rpsports.network.SportsNet;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Плохие события, когда тренировался до нуля выносливости (docs/00): гантель на ногу, штанга придавила (если нет
 * страхующего), сорвался с турника, лента выкинула, груша по голове, носом в пол, ноги подкосились, свело живот.
 */
final class BadEvents {
    private BadEvents() {}

    /** Ушиб: через мод тела, иначе обычный урон. */
    private static void bruise(ServerPlayer sp, ResourceLocation part, double severity, float fallbackDamage) {
        BodyProvider b = RpCoreAPI.body();
        if (b != null && b.handles(RpIds.BODY_BRUISES) && b.bruise(sp, part, Math.max(0.05, Math.min(1, severity)))) return;
        sp.hurt(sp.damageSources().generic(), fallbackDamage);
    }

    private static void say(ServerPlayer sp, String key) {
        sp.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
    }

    /** Время события: для «застрял под штангой» и «сел на пол» — когда кончить упражнение. */
    private static final java.util.Map<java.util.UUID, Long> UNTIL = new java.util.HashMap<>();

    static void trigger(ServerPlayer sp, Session s, SportsData d) {
        long now = sp.level().getGameTime();
        double body = SportsService.bodyKg(sp);
        switch (s.exercise) {
            case BENCH -> {
                if (SportsService.benchHasSpotter(s, sp)) {
                    ServerPlayer spotter = sp.server.getPlayerList().getPlayer(s.spotter);
                    if (spotter != null) spotter.displayClientMessage(Component.translatable("rpsports.msg.you_caught").withStyle(ChatFormatting.GREEN), true);
                    SportsService.stop(sp, "rpsports.msg.spotter_saved");
                    return;
                }
                double sev = s.weightKg / 150.0;
                bruise(sp, RpIds.PART_CHEST, sev, (float) (2 + s.weightKg / 40.0));
                bruise(sp, RpIds.PART_LEFT_ARM, sev / 2, 0);
                bruise(sp, RpIds.PART_RIGHT_ARM, sev / 2, 0);
                Muscles.fatigue(sp, d, Zone.CHEST, 40, now);
                Muscles.fatigue(sp, d, Zone.LEFT_ARM, 40, now);
                Muscles.fatigue(sp, d, Zone.RIGHT_ARM, 40, now);
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3, false, false, false));
                say(sp, "rpsports.bad.bench");
                UNTIL.put(sp.getUUID(), now + 80);
            }
            case DUMBBELL -> {
                InteractionHand h = s.workHand();
                boolean right = h == InteractionHand.MAIN_HAND;
                double kg = s.dual && !right ? s.weightKg2 : s.weightKg;
                bruise(sp, right ? RpIds.PART_RIGHT_FOOT : RpIds.PART_LEFT_FOOT, kg / 25.0, (float) (1 + kg / 8.0));
                ItemStack st = sp.getItemInHand(h);
                if (!st.isEmpty()) {
                    sp.drop(st.copy(), true, false);
                    sp.setItemInHand(h, ItemStack.EMPTY);
                }
                say(sp, "rpsports.bad.dumbbell");
                SportsService.stop(sp, null);
            }
            case PULLUP -> {
                double sev = body / 150.0;
                bruise(sp, RpIds.PART_LEFT_LEG, sev, (float) (1 + body / 60.0));
                bruise(sp, RpIds.PART_RIGHT_FOOT, sev / 2, 0);
                say(sp, "rpsports.bad.pullup");
                SportsService.stop(sp, null);
            }
            case TREADMILL -> {
                Vec3 back = sp.getLookAngle().multiply(-0.8, 0, -0.8).add(0, 0.25, 0);
                sp.setDeltaMovement(back);
                sp.hurtMarked = true;
                double sev = body / 200.0;
                bruise(sp, RpIds.PART_LEFT_LEG, sev, (float) (1 + body / 80.0));
                bruise(sp, RpIds.PART_RIGHT_ARM, sev / 2, 0);
                say(sp, "rpsports.bad.treadmill");
                SportsService.stop(sp, null);
            }
            case BAG -> {
                bruise(sp, RpIds.PART_HEAD, 0.2, 1);
                SportsNet.send(sp, new SportsNet.Status(false, true, s.reps));
                say(sp, "rpsports.bad.bag");
                SportsService.stop(sp, null);
            }
            case PUSHUP -> {
                bruise(sp, RpIds.PART_HEAD, 0.15, 0.5f);
                say(sp, "rpsports.bad.pushup");
                SportsService.stop(sp, null);
            }
            case SQUAT -> {
                Muscles.fatigue(sp, d, Zone.LEGS, 30, now);
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4, false, false, false));
                say(sp, "rpsports.bad.squat");
                UNTIL.put(sp.getUUID(), now + 100);
            }
            case SITUP -> {
                BodyProvider b = RpCoreAPI.body();
                boolean done = b != null && b.handles(RpIds.BODY_MUSCLES) && b.strain(sp, RpIds.PART_ABDOMEN, 1);
                if (!done) sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1, false, false, false));
                say(sp, "rpsports.bad.situp");
                SportsService.stop(sp, null);
            }
        }
    }

    /** true — пора закончить упражнение (застрял под штангой, сидит на полу). */
    static boolean tick(ServerPlayer sp, Session s, SportsData d, long now) {
        Long until = UNTIL.get(sp.getUUID());
        if (until == null) return false;
        if (now < until) return false;
        UNTIL.remove(sp.getUUID());
        if (s.exercise == faygolover.rpsports.core.Exercise.BENCH)
            sp.displayClientMessage(Component.translatable("rpsports.msg.bar_racked").withStyle(ChatFormatting.GRAY), true);
        return true;
    }
}
