package faygolover.rpsports.client;

import faygolover.rpsports.core.Exercise;
import faygolover.rpsports.network.SportsNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Маленький выбор упражнений без снаряда у прицела (клавиша «Упражнения»): 1 — отжимания, 2 — приседания, 3 — пресс.
 * Esc или та же клавиша — закрыть.
 */
public final class ExerciseSelector {
    private ExerciseSelector() {}

    private static final Exercise[] OPTIONS = {Exercise.PUSHUP, Exercise.SQUAT, Exercise.SITUP};
    private static boolean open;

    public static void toggle() {
        if (ClientSession.active()) return;
        open = !open;
    }

    static boolean onKey(int key, int action) {
        if (!open) return false;
        if (action != GLFW.GLFW_PRESS) return key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_3;
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            open = false;
            return true;
        }
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_3) {
            open = false;
            SportsNet.toServer(new SportsNet.Start(OPTIONS[key - GLFW.GLFW_KEY_1].ordinal()));
            return true;
        }
        return false;
    }

    public static void render(GuiGraphics g) {
        if (!open) return;
        Minecraft mc = Minecraft.getInstance();
        int cx = g.guiWidth() / 2, cy = g.guiHeight() / 2 + 14;
        g.fill(cx - 62, cy - 2, cx + 62, cy + 12 * OPTIONS.length + 2, 0xA0000000);
        for (int i = 0; i < OPTIONS.length; i++) {
            g.drawString(mc.font, Component.literal((i + 1) + " — ").append(Component.translatable(OPTIONS[i].key())),
                    cx - 58, cy + i * 12 + 1, 0xFFE6E6E6, true);
        }
    }
}
