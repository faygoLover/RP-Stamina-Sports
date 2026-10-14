package faygolover.rpsports.client;

import com.mojang.blaze3d.platform.InputConstants;
import faygolover.rpcore.client.hud.HudRegistry;
import faygolover.rpsports.registry.SportsRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Клиент: виджет мини-игры в общем HUD, клавиша «Упражнения», мышь у груши и дорожки, камера и позы каждый кадр.
 * Клавиатуру мини-игра забирает раньше всех назначений — миксин {@link faygolover.rpsports.mixin.KeyboardHandlerMixin}.
 */
public final class ClientSetup {
    private ClientSetup() {}

    public static final KeyMapping EXERCISES = new KeyMapping("key.rpsports.exercises", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.rpsports");

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientSetup::setup);
        modBus.addListener(ClientSetup::keys);
        modBus.addListener(ClientSetup::renderers);
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener(ClientSetup::onRenderTick);
        bus.addListener(ClientSetup::onClientTick);
        bus.addListener(EventPriority.HIGHEST, ClientSetup::onMouse);
        bus.addListener(EventPriority.HIGHEST, ClientSetup::onInteraction);
        bus.addListener(ClientSetup::onOverlay);
        bus.addListener(ClientSetup::onLogout);
    }

    private static void setup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            HudRegistry.register(new SportsHud());
            ExerciseAnimations.init();
        });
    }

    private static void keys(RegisterKeyMappingsEvent e) {
        e.register(EXERCISES);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(SportsRegistry.SEAT.get(), NoopRenderer::new);
    }

    /** Каждый кадр, до отрисовки: мини-игра по реальному времени, камера вокруг персонажа. */
    private static void onRenderTick(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        ClientSession.frame();
        CameraControl.frame();
    }

    private static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        while (EXERCISES.consumeClick()) {
            if (mc.player != null && mc.screen == null) ExerciseSelector.toggle();
        }
    }

    /** Груша и дорожка — ЛКМ и ПКМ; у остальных упражнений мышь не бьёт и не использует предмет. */
    private static void onMouse(InputEvent.MouseButton.Pre e) {
        if (ClientSession.onMouse(e.getButton(), e.getAction())) e.setCanceled(true);
    }

    /** Удержание кнопки мыши (повтор атаки/использования) тоже не должно проходить во время упражнения. */
    private static void onInteraction(InputEvent.InteractionKeyMappingTriggered e) {
        if (!ClientSession.active() || ClientSession.game() == null) return;
        if (ClientSession.game().exercise() == faygolover.rpsports.core.Exercise.DUMBBELL) return;
        e.setCanceled(true);
        e.setSwingHand(false);
    }

    /** Выбор упражнений без снаряда — под прицелом. */
    private static void onOverlay(RenderGuiOverlayEvent.Post e) {
        if (e.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) ExerciseSelector.render(e.getGuiGraphics());
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        ClientSession.stopped("");
    }
}
