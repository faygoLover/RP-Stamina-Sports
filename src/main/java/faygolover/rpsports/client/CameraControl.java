package faygolover.rpsports.client;

import faygolover.rpsports.core.Exercise;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

/**
 * Камера на время упражнения (docs/00): вид от 3-го лица спереди (штанга, турник) или сбоку (остальное), гантели —
 * без изменений. Камеру можно вращать вокруг персонажа мышью — как в Better Third Person. Если он стоит, вращение
 * ведёт он сам, а мы только включаем 3-е лицо.
 */
public final class CameraControl {
    private CameraControl() {}

    @Nullable private static CameraType saved;
    private static boolean orbit;
    private static float orbitYaw, orbitPitch, lockYaw, lockPitch;

    private static boolean betterThirdPerson() {
        return ModList.get().isLoaded("betterthirdperson");
    }

    public static void start(Exercise e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || e.camera == Exercise.Camera.NONE) return;
        saved = mc.options.getCameraType();
        mc.options.setCameraType(e.camera == Exercise.Camera.FRONT ? CameraType.THIRD_PERSON_FRONT : CameraType.THIRD_PERSON_BACK);
        orbit = !betterThirdPerson();
        lockYaw = p.getYRot();
        lockPitch = p.getXRot();
        orbitYaw = e.camera == Exercise.Camera.SIDE ? lockYaw + 90 : lockYaw;
        orbitPitch = 15;
    }

    public static void stop() {
        Minecraft mc = Minecraft.getInstance();
        if (saved != null) mc.options.setCameraType(saved);
        saved = null;
        orbit = false;
    }

    /** Вращает ли камеру наш код (тогда {@link faygolover.rpsports.mixin.CameraMixin} подставляет углы). */
    public static boolean orbiting() {
        return orbit && saved != null;
    }

    public static float yaw() {
        return orbitYaw + (ClientSession.shake > 0 ? (float) Math.sin(ClientSession.shake * 60) * 4 : 0);
    }

    public static float pitch() {
        return orbitPitch;
    }

    /** Каждый кадр: движение мыши крутит камеру, а персонаж смотрит туда же, куда смотрел. */
    public static void frame() {
        if (!orbiting()) return;
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        float dy = p.getYRot() - lockYaw, dp = p.getXRot() - lockPitch;
        if (dy != 0 || dp != 0) {
            orbitYaw += dy;
            orbitPitch = Math.max(-60, Math.min(80, orbitPitch + dp));
            p.setYRot(lockYaw);
            p.setXRot(lockPitch);
            p.yRotO = lockYaw;
            p.xRotO = lockPitch;
        }
    }
}
