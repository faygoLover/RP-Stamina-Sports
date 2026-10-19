package faygolover.rpsports.mixin;

import faygolover.rpsports.client.CameraControl;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Камера на тренировке вращается вокруг персонажа мышью ({@link CameraControl}): подменяем углы первого
 * {@code setRotation} в {@code Camera.setup}, дальше игра сама отодвигает камеру назад (и разворачивает для вида спереди).
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0), index = 0)
    private float rpsports$yaw(float yaw) {
        return CameraControl.orbiting() ? CameraControl.yaw() : yaw;
    }

    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0), index = 1)
    private float rpsports$pitch(float pitch) {
        return CameraControl.orbiting() ? CameraControl.pitch() : pitch;
    }
}
