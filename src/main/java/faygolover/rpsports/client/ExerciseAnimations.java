package faygolover.rpsports.client;

import faygolover.rpsports.RpSports;
import faygolover.rpsports.core.Exercise;
import faygolover.rpsports.network.SportsNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;

/**
 * Позы на тренировке у всех игроков рядом. player-animation-lib в сборке есть, но аддон без него тоже работает —
 * просто без поз: все обращения к библиотеке — в {@link Lib}, который загружается, только если она стоит.
 */
public final class ExerciseAnimations {
    private ExerciseAnimations() {}

    static final ResourceLocation LAYER = new ResourceLocation(RpSports.MODID, "exercise");
    private static boolean enabled;

    static void init() {
        enabled = ModList.get().isLoaded("playeranimator");
        if (enabled) Lib.register();
    }

    /** Своя поза (из мини-игры, каждый кадр). {@code exercise} −1 — упражнение кончилось. */
    public static void local(AbstractClientPlayer p, int exercise, float a, float b) {
        if (enabled) Lib.set(p, exercise, a, b, ClientSession.leftHand());
    }

    /** Поза другого игрока (сервер пересылает ~10 раз в секунду). */
    public static void remote(SportsNet.Anim m) {
        if (!enabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(m.entityId());
        if (!(e instanceof AbstractClientPlayer p) || p == mc.player) return;
        // У гантели b — рука (0 правая, 1 левая).
        boolean left = m.exercise() == Exercise.DUMBBELL.ordinal() && m.b() >= 0.5f;
        Lib.set(p, m.exercise(), m.a(), m.b(), left);
    }

    private static final class Lib {
        static void register() {
            dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 1000,
                    ExerciseAnimation::new);
        }

        static void set(AbstractClientPlayer p, int exercise, float a, float b, boolean leftHand) {
            var data = dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess.getPlayerAssociatedData(p).get(LAYER);
            if (data instanceof ExerciseAnimation anim) {
                anim.leftHand = leftHand;
                anim.set(exercise, a, b);
            }
        }
    }
}
