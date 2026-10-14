package faygolover.rpsports.client;

import faygolover.rpsports.client.games.*;
import faygolover.rpsports.core.Exercise;
import faygolover.rpsports.network.SportsNet;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Своё упражнение на клиенте: мини-игра, клавиши с абсолютным приоритетом (E, R, T, F, у приседаний A и D, у груши и
 * дорожки ЛКМ и ПКМ), Ctrl — закончить, камера и поза. Сервер решает, началось и кончилось ли упражнение.
 */
public final class ClientSession {
    private ClientSession() {}

    @Nullable private static Minigame game;
    private static long lastTick = -1;
    private static float sendTimer;
    private static boolean stopSent;
    /** Встряска камеры (груша по голове), секунды. */
    static float shake;

    @Nullable
    public static Minigame game() {
        return game;
    }

    public static boolean active() {
        return game != null;
    }

    /** Гантель в левой руке. */
    static boolean leftHand() {
        return game != null && game.exercise() == Exercise.DUMBBELL && game.poseB() >= 0.5f;
    }

    public static void open(SportsNet.Open p) {
        Exercise e = Exercise.VALUES[Math.max(0, Math.min(Exercise.VALUES.length - 1, p.exercise()))];
        game = switch (e) {
            case BENCH -> new BenchGame(p);
            case DUMBBELL -> new DumbbellGame(p);
            case PULLUP -> new PullupGame(p);
            case BAG -> new BagGame(p);
            case TREADMILL -> new TreadmillGame(p);
            case PUSHUP -> new PushupGame(p);
            case SQUAT -> new SquatGame(p);
            case SITUP -> new SitupGame(p);
        };
        stopSent = false;
        lastTick = -1;
        net.minecraft.client.KeyMapping.releaseAll();
        CameraControl.start(e);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            ExerciseAnimations.local(mc.player, e.ordinal(), 0, 0);
            mc.player.displayClientMessage(Component.translatable("rpsports.ui.hint_exit"), true);
        }
    }

    public static void status(SportsNet.Status s) {
        if (game != null) {
            game.setTremble(s.tremble());
            game.setReps(s.reps());
        }
        if (s.shake()) shake = 0.6f;
    }

    public static void stopped(String reason) {
        if (game != null) net.minecraft.client.KeyMapping.releaseAll();
        game = null;
        CameraControl.stop();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) ExerciseAnimations.local(mc.player, -1, 0, 0);
    }

    /** Ctrl или выход: попросить сервер закончить. */
    public static void requestStop() {
        if (game == null || stopSent) return;
        stopSent = true;
        SportsNet.toServer(new SportsNet.StopRequest());
    }

    /** Кадр: мини-игра идёт по реальному времени. */
    public static void frame() {
        if (shake > 0) shake = Math.max(0, shake - 0.016f);
        Minecraft mc = Minecraft.getInstance();
        if (game == null || mc.player == null) return;
        long now = net.minecraft.Util.getMillis();
        float dt = lastTick < 0 ? 0 : Math.min(0.1f, (now - lastTick) / 1000f);
        lastTick = now;
        if (mc.isPaused()) return;
        game.tick(dt);
        ExerciseAnimations.local(mc.player, game.exercise().ordinal(), game.poseA(), game.poseB());
        sendTimer += dt;
        if (sendTimer >= 0.1f) {
            sendTimer = 0;
            SportsNet.toServer(new SportsNet.Progress(game.poseA(), game.poseB()));
        }
    }

    /**
     * Клавиатура (из миксина KeyboardHandler, до назначений игры): true — съели. Съедаем только нажатия и повторы
     * своих клавиш; отпускания всегда пропускаем дальше — иначе игра считает клавишу зажатой и после упражнения.
     */
    public static boolean onKey(int key, int action) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return false;
        if (ExerciseSelector.onKey(key, action)) return action != GLFW.GLFW_RELEASE;
        if (game == null) return false;
        boolean release = action == GLFW.GLFW_RELEASE;
        if (key == GLFW.GLFW_KEY_LEFT_CONTROL || key == GLFW.GLFW_KEY_RIGHT_CONTROL) {
            if (action == GLFW.GLFW_PRESS) requestStop();
            return !release;
        }
        // Присесть (Shift) на тренажёре нельзя — и спешиться со скамьи тоже: выход только Ctrl.
        if (locksMovement() && mc.options.keyShift.matches(key, 0)) return !release;
        if (!game.owns(key)) return false;
        game.key(key, action);
        return !release;
    }

    /** Кнопка мыши: true — съели (только нажатие; отпускание всегда идёт в игру, чтобы кнопка не «залипла»). */
    public static boolean onMouse(int button, int action) {
        if (game == null || Minecraft.getInstance().screen != null || action != GLFW.GLFW_PRESS) return false;
        if (game.ownsMouse()) {
            game.mouse(button);
            return true;
        }
        return game.exercise() != Exercise.DUMBBELL;
    }

    /** На тренажёре и в упражнениях без снаряда игрок стоит на месте; с гантелями можно ходить. */
    public static boolean locksMovement() {
        return game != null && game.exercise() != Exercise.DUMBBELL;
    }

    /** Первые 1,5 с — подсказка «Ctrl — закончить» поверх ванильной «Нажмите Shift, чтобы спешиться». */
    static void tickHint() {
        if (game == null || game.elapsed() > 1.5f) return;
        Minecraft mc = Minecraft.getInstance();
        mc.gui.setOverlayMessage(Component.translatable("rpsports.ui.hint_exit"), false);
    }
}
