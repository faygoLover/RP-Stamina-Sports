package faygolover.rpsports.client.games;

import faygolover.rpsports.network.SportsNet;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Отжимания: ритм. Ползунок идёт слева направо за один такт; в конце полосы — зелёное окно такта. Нажать E, когда
 * ползунок в нём: одно нажатие — вниз, следующее — вверх, две удачные половинки — повтор. Темп с каждым повтором
 * чуть быстрее; мимо такта (рано, поздно, пропустил) — ошибка, темп сначала.
 */
public class PushupGame extends Minigame {
    private static final float START_PERIOD = 1.1f, MIN_PERIOD = 0.55f;
    private float period = START_PERIOD, clock, down;
    /** Номер такта, на который уже нажали / который уже проверили на пропуск. */
    private int lastHit, lastChecked;
    private boolean half;

    public PushupGame(SportsNet.Open p) {
        super(p);
    }

    private float window() {
        float r = Math.max(0.6f, params.ratio());
        return Math.min(0.3f, 0.16f * strengthEase() * params.ease() / r * (tremble ? 0.8f : 1f));
    }

    @Override
    protected void update(float dt) {
        clock += dt;
        down = approach(down, half ? 1 : 0, dt, 9);
        // Такт n — в момент n·period; его окно кончилось, а нажатия не было — пропустил.
        int ended = (int) Math.floor((clock - window()) / period);
        if (ended >= 1 && ended > lastHit && ended > lastChecked) {
            lastChecked = ended;
            miss();
        }
    }

    private void miss() {
        rep(false, 0);
        period = START_PERIOD;
        half = false;
        clock = 0;
        lastHit = 0;
        lastChecked = 0;
    }

    @Override
    public boolean owns(int key) {
        return key == GLFW.GLFW_KEY_E;
    }

    @Override
    public void key(int key, int action) {
        if (key != GLFW.GLFW_KEY_E || action != GLFW.GLFW_PRESS) return;
        int n = Math.round(clock / period);
        if (n < 1 || n == lastHit || Math.abs(clock - n * period) > window()) {
            miss();
            return;
        }
        lastHit = n;
        if (half) {
            half = false;
            rep(true, 0);
            // Новый темп — с этого такта.
            float phase = clock - n * period;
            period = Math.max(MIN_PERIOD, period * 0.97f);
            clock = n * period + phase;
            lastHit = n;
            lastChecked = Math.max(lastChecked, n);
        } else {
            half = true;
        }
    }

    @Override
    public float poseA() {
        return down;
    }

    @Override
    protected int draw(GuiGraphics g, Font font, int x, int y) {
        g.drawCenteredString(font, Component.translatable(half ? "rpsports.ui.up" : "rpsports.ui.down"), x + W / 2, y, C_FILL);
        int by = y + 11;
        g.fill(x, by, x + W, by + 6, C_BG);
        float w = window() / period;
        zone(g, x, by, W, 6, 1 - w, 1);
        zone(g, x, by, W, 6, 0, w);
        float frac = (clock % period) / period;
        marker(g, x, by, W, 6, frac);
        flashFrame(g, x, by, W, 6);
        label(g, font, Component.translatable("rpsports.ui.reps", reps), x, by + 9);
        return 29;
    }
}
