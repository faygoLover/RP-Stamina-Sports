package faygolover.rpsports.client.games;

import faygolover.rpsports.network.SportsNet;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Пресс — как стратагемы в Helldivers: показана цепочка стрелок, ввести её по порядку (стрелки или WASD), пока не
 * кончилось время. Каждые 4 повтора цепочка на одну длиннее (4 → 8). Не та стрелка или не успел — ошибка, новая
 * цепочка. С каждой верной стрелкой корпус поднимается выше.
 */
public class SitupGame extends Minigame {
    /** Направления: 0 — вверх, 1 — вправо, 2 — вниз, 3 — влево. */
    private int[] seq = new int[0];
    private int idx;
    private float limit, left, curl, wrong;

    public SitupGame(SportsNet.Open p) {
        super(p);
        next();
    }

    private void next() {
        int len = Math.min(8, 4 + reps / 4);
        seq = new int[len];
        for (int i = 0; i < len; i++) seq[i] = random.nextInt(4);
        idx = 0;
        limit = (1.6f + 0.45f * len) * strengthEase() * params.ease() * (tremble ? 0.8f : 1f) / Math.max(0.7f, params.ratio());
        left = limit;
    }

    private static int dir(int key) {
        return switch (key) {
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_W -> 0;
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_D -> 1;
            case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_S -> 2;
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_A -> 3;
            default -> -1;
        };
    }

    @Override
    protected void update(float dt) {
        float target = seq.length == 0 ? 0 : (float) idx / seq.length;
        curl = approach(curl, target, dt, 9);
        if (wrong > 0) wrong -= dt;
        left -= dt;
        if (left <= 0) {
            rep(false, 0);
            next();
        }
    }

    @Override
    public boolean owns(int key) {
        return dir(key) >= 0;
    }

    @Override
    protected void key(int key, int action) {
        int d = dir(key);
        if (d < 0 || action != GLFW.GLFW_PRESS) return;
        if (d == seq[idx]) {
            idx++;
            if (idx >= seq.length) {
                rep(true, 0);
                next();
            }
        } else {
            wrong = 0.3f;
            rep(false, 0);
            next();
        }
    }

    @Override
    public float poseA() {
        return curl;
    }

    @Override
    protected int draw(GuiGraphics g, Font font, int x, int y) {
        float cx = x + W / 2f;
        int step = 14;
        float sx = cx - (seq.length - 1) * step / 2f;
        g.fill(x + 4, y, x + W - 4, y + 16, Qte.SHADE);
        for (int i = 0; i < seq.length; i++) {
            int c = i < idx ? Qte.GOLD : i == idx ? (wrong > 0 ? Qte.BAD : Qte.WHITE) : Qte.WHITE_DIM;
            float size = i == idx ? 10 : 8;
            Qte.arrow(g, sx + i * step, y + 8, size, seq[i], c);
        }
        int w = W - 8;
        g.fill(x + 4, y + 18, x + 4 + Math.round(w * Math.max(0, left / Math.max(0.1f, limit))), y + 19, flashColor(Qte.GOLD));
        return 21;
    }
}
