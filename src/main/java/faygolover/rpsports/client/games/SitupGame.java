package faygolover.rpsports.client.games;

import faygolover.rpsports.network.SportsNet;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Пресс: нажать по порядку показанные клавиши из E, R, T, F, пока не кончилось время. Каждые 5 повторов — на одну
 * длиннее (3 → 6). Не та клавиша или не успел — ошибка. С каждой нажатой клавишей корпус поднимается выше.
 */
public class SitupGame extends Minigame {
    private static final int[] KEYS = {GLFW.GLFW_KEY_E, GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_T, GLFW.GLFW_KEY_F};
    private static final String[] NAMES = {"E", "R", "T", "F"};
    private int[] seq = new int[0];
    private int idx;
    private float limit, left, curl;

    public SitupGame(SportsNet.Open p) {
        super(p);
        next();
    }

    private void next() {
        int len = Math.min(6, 3 + reps / 5);
        seq = new int[len];
        for (int i = 0; i < len; i++) seq[i] = random.nextInt(KEYS.length);
        idx = 0;
        limit = (2.5f + 0.5f * len) * strengthEase() * params.ease() * (tremble ? 0.8f : 1f) / Math.max(0.7f, params.ratio());
        left = limit;
    }

    @Override
    protected void update(float dt) {
        float target = seq.length == 0 ? 0 : (float) idx / seq.length;
        curl += (target - curl) * Math.min(1, dt * 10);
        left -= dt;
        if (left <= 0) {
            rep(false, 0);
            next();
        }
    }

    @Override
    public boolean key(int key, int action) {
        if (!owns(key)) return false;
        if (action != GLFW.GLFW_PRESS) return true;
        if (key == KEYS[seq[idx]]) {
            idx++;
            if (idx >= seq.length) {
                rep(true, 0);
                next();
            }
        } else {
            rep(false, 0);
            next();
        }
        return true;
    }

    @Override
    public float poseA() {
        return curl;
    }

    @Override
    public void render(GuiGraphics g, Font font, int x, int y) {
        int step = 16, sx = x + (W - seq.length * step) / 2 + jitter();
        for (int i = 0; i < seq.length; i++) {
            int c = i < idx ? C_ZONE : i == idx ? C_FILL : C_DIM;
            g.fill(sx + i * step, y, sx + i * step + 13, y + 11, C_BG);
            g.drawCenteredString(font, NAMES[seq[i]], sx + i * step + 7, y + 2, c);
        }
        bar(g, x, y + 13, W, 3, left / Math.max(0.1f, limit), C_TIME);
        flashFrame(g, x, y, W, 16);
        label(g, font, Component.translatable("rpsports.ui.reps", reps), x, y + 19);
    }
}
