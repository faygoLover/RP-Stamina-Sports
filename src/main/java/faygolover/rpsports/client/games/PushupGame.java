package faygolover.rpsports.client.games;

import faygolover.rpsports.network.SportsNet;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Отжимания: ритм. Пульсирует кружок-метроном; нажимать E в такт: одно нажатие — вниз, следующее — вверх, две
 * удачные половинки — повтор. Темп с каждым повтором чуть быстрее; мимо такта — ошибка, темп сначала.
 */
public class PushupGame extends Minigame {
    private static final float START_PERIOD = 1.1f, MIN_PERIOD = 0.55f;
    private float period = START_PERIOD, beat, down;
    private boolean half, hitThisBeat;

    public PushupGame(SportsNet.Open p) {
        super(p);
    }

    private float window() {
        float r = Math.max(0.6f, params.ratio());
        return Math.min(0.3f, 0.16f * strengthEase() * params.ease() / r * (tremble ? 0.8f : 1f));
    }

    @Override
    protected void update(float dt) {
        beat += dt;
        float target = half ? 1 : 0;
        down += (target - down) * Math.min(1, dt * 8);
        if (beat >= period) {
            if (!hitThisBeat && beat - period > window()) miss();
            if (beat >= period * 1.5f) {
                beat -= period;
                hitThisBeat = false;
            }
        }
    }

    private void miss() {
        rep(false, 0);
        period = START_PERIOD;
        half = false;
        beat = 0;
        hitThisBeat = false;
    }

    @Override
    public boolean owns(int key) {
        return key == GLFW.GLFW_KEY_E;
    }

    @Override
    public boolean key(int key, int action) {
        if (key != GLFW.GLFW_KEY_E) return false;
        if (action != GLFW.GLFW_PRESS) return true;
        float dist = Math.min(Math.abs(beat - period), Math.abs(beat));
        if (dist > window() || hitThisBeat) {
            miss();
            return true;
        }
        hitThisBeat = true;
        if (half) {
            half = false;
            rep(true, 0);
            period = Math.max(MIN_PERIOD, period * 0.97f);
        } else {
            half = true;
        }
        return true;
    }

    @Override
    public float poseA() {
        return down;
    }

    @Override
    public void render(GuiGraphics g, Font font, int x, int y) {
        int cx = x + W / 2, cy = y + 10;
        float k = 1 - Math.min(1, Math.abs(beat - period) / Math.max(0.05f, window() * 3));
        int r = 4 + (int) (k * 6);
        int c = k > 0.6f ? C_ZONE : C_DIM;
        g.fill(cx - r + jitter(), cy - r, cx + r, cy + r, c);
        flashFrame(g, cx - 11, cy - 11, 22, 22);
        g.drawCenteredString(font, Component.translatable(half ? "rpsports.ui.up" : "rpsports.ui.down"), cx, cy + 13, C_DIM);
        label(g, font, Component.translatable("rpsports.ui.reps", reps), x, y + 24);
    }
}
