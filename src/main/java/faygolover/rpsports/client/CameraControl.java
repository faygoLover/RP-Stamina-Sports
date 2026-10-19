package faygolover.rpsports.client;

import faygolover.rpsports.core.Exercise;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Камера на время упражнения (docs/00): вид от 3-го лица спереди (штанга, турник) или сбоку (остальное), гантели —
 * без изменений. Камеру мышь вращает вокруг персонажа, а сам персонаж не поворачивается: движение мыши уходит в
 * углы камеры, поворот игрока каждый кадр возвращается на угол тренажёра.
 * <p>
 * Вращение всегда наше, и с Better Third Person тоже: на время упражнения ставим вид «спереди»
 * ({@link CameraType#THIRD_PERSON_FRONT}), а при нём Better Third Person сам выключается (зеркальная камера). Иначе он
 * доворачивал игрока к камере, и тело дрожало (находка 112). Вид «спереди» игра разворачивает на 180° и
 * переворачивает наклон — {@link #yaw()} и {@link #pitch()} отдают углы с поправкой на это, а здесь хранятся углы,
 * под которыми камера смотрит на самом деле.
 */
public final class CameraControl {
    private CameraControl() {}

    /** Вид камеры до упражнения; не {@code null} — идёт упражнение с камерой. */
    @Nullable private static CameraType saved;
    /** Куда на самом деле смотрит камера (градусы). */
    private static float viewYaw, viewPitch;
    /** Угол, на котором тренажёр поставил персонажа, и наклон головы. */
    private static float lockYaw, lockPitch;

    public static void start(Exercise e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || e.camera == Exercise.Camera.NONE) return;
        saved = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        lockYaw = p.getYRot();
        lockPitch = p.getXRot();
        // Как было раньше: штанга и турник — лицом к персонажу и чуть снизу, остальное — сбоку и чуть сверху.
        if (e.camera == Exercise.Camera.FRONT) {
            viewYaw = lockYaw + 180;
            viewPitch = -15;
        } else {
            viewYaw = lockYaw + 90;
            viewPitch = 15;
        }
    }

    public static void stop() {
        Minecraft mc = Minecraft.getInstance();
        if (saved != null) mc.options.setCameraType(saved);
        saved = null;
    }

    /** Камера наша (тогда {@link faygolover.rpsports.mixin.CameraMixin} подставляет углы). */
    public static boolean orbiting() {
        return saved != null;
    }

    /** Угол тренажёра для тела своего персонажа, пока идёт упражнение с камерой (иначе {@code null}). */
    @Nullable
    public static Float lockedYaw() {
        return saved != null ? lockYaw : null;
    }

    /** Поворот для первого {@code setRotation} в {@code Camera.setup}: игра потом добавит 180° для вида спереди. */
    public static float yaw() {
        return viewYaw - 180 + (ClientSession.shake > 0 ? (float) Math.sin(ClientSession.shake * 60) * 4 : 0);
    }

    /** Наклон для первого {@code setRotation}: игра его перевернёт для вида спереди. */
    public static float pitch() {
        return -viewPitch;
    }

    /** Каждый кадр до отрисовки: движение мыши — в камеру, персонаж остаётся на месте. */
    public static void frame() {
        if (!orbiting()) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        // F5 во время упражнения — вид остаётся нашим (иначе включился бы Better Third Person или вид от 1-го лица).
        if (mc.options.getCameraType() != CameraType.THIRD_PERSON_FRONT) mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        float dy = p.getYRot() - lockYaw, dp = p.getXRot() - lockPitch;
        if (dy != 0 || dp != 0) {
            viewYaw += dy;
            viewPitch = Math.max(-60, Math.min(80, viewPitch + dp));
            p.setYRot(lockYaw);
            p.setXRot(lockPitch);
            p.yRotO = lockYaw;
            p.xRotO = lockPitch;
        }
    }
}
