package faygolover.rpsports.mixin;

import faygolover.rpsports.client.ClientSession;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Клавиши мини-игры (E, R, T, F, A/D, Ctrl) — с абсолютным приоритетом: забираем их до всех назначений игры и
 * других модов (инвентарь на E, бег на Ctrl и т. п.), пока идёт упражнение или открыт выбор упражнений.
 */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void rpsports$keyPress(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (window != Minecraft.getInstance().getWindow().getWindow()) return;
        if (ClientSession.onKey(key, action)) ci.cancel();
    }
}
