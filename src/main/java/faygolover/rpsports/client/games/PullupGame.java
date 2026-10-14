package faygolover.rpsports.client.games;

import faygolover.rpsports.network.SportsNet;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Турник: из центра круга расширяется кольцо (скорость каждый раз случайная, до ×2); нажать, когда оно в зелёной
 * кольцевой зоне. Каждый успех сужает зону. На самой тонкой — ещё 5 повторов так же, потом каждый раз случайная
 * клавиша из E, R, T, F. Промах — повтор не засчитан, зона не меняется. Подтягивание идёт, пока растёт кольцо.
 */
public class PullupGame extends Minigame {
    private static final int[] KEYS = {GLFW.GLFW_KEY_E, GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_T, GLFW.GLFW_KEY_F};
    private static final String[] NAMES = {"E", "R", "T", "F"};
    private static final float ZONE_START = 0.62f, W_MIN = 0.06f, JERK = 0.45f, LOWER = 0.6f;
    private float ring, speed, width, jerk, lower;
    private int atMin, keyIndex;
    private boolean pressed;

    public PullupGame(SportsNet.Open p) {
        super(p);
        width = Math.min(0.3f, 0.22f * p.ease() * strengthEase());
        next();
    }

    private void next() {
        ring = 0;
        pressed = false;
        // Сильнее — кольцо растёт медленнее; тяжелее тело — быстрее.
        float base = 1f / 1.4f * Math.max(0.6f, params.ratio()) / strengthEase();
        speed = base * (1 + random.nextFloat());
        keyIndex = atMin >= 5 ? random.nextInt(KEYS.length) : 0;
    }

    @Override
    protected void update(float dt) {
        if (jerk > 0) {
            jerk -= dt;
            if (jerk <= 0) lower = LOWER;
            return;
        }
        if (lower > 0) {
            lower -= dt;
            ring = Math.max(0, ring - dt / LOWER);
            if (lower <= 0) next();
            return;
        }
        float wobble = tremble ? (random.nextFloat() - 0.5f) * 0.02f : 0;
        ring += dt * speed + wobble;
        if (ring >= 1) {
            ring = 1;
            rep(false, 0);
            lower = LOWER;
        }
    }

    @Override
    public boolean key(int key, int action) {
        if (!owns(key)) return false;
        if (action != GLFW.GLFW_PRESS || jerk > 0 || lower > 0 || pressed) return true;
        pressed = true;
        boolean inZone = ring >= ZONE_START && ring <= ZONE_START + width;
        if (inZone && key == KEYS[keyIndex]) {
            rep(true, 0);
            if (width > W_MIN + 1e-4) width = Math.max(W_MIN, width - 0.025f);
            else atMin++;
            jerk = JERK;
        } else {
            rep(false, 0);
            lower = LOWER;
        }
        return true;
    }

    @Override
    public float poseA() {
        return jerk > 0 ? 1 : ring;
    }

    @Override
    public float poseB() {
        return jerk > 0 ? 1 : 0;
    }

    private void circle(GuiGraphics g, int cx, int cy, float r, int color, int dots) {
        for (int i = 0; i < dots; i++) {
            double a = i * Math.PI * 2 / dots;
            int px = cx + (int) Math.round(Math.cos(a) * r), py = cy + (int) Math.round(Math.sin(a) * r);
            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    @Override
    public void render(GuiGraphics g, Font font, int x, int y) {
        int cx = x + W / 2, cy = y + 24, max = 22;
        int j = jitter();
        g.fill(cx - max - 2, cy - max - 2, cx + max + 3, cy + max + 3, C_BG);
        for (float r = ZONE_START * max; r <= (ZONE_START + width) * max; r += 0.7f) circle(g, cx, cy, r, 0xA04CB050, 64);
        circle(g, cx, cy, max, C_DIM, 64);
        if (lower <= 0 || ring > 0) circle(g, cx + j, cy, ring * max, jerk > 0 ? 0xFF4CB050 : 0xFFFFFFFF, 72);
        String k = NAMES[keyIndex];
        g.drawCenteredString(font, k, cx, cy - 4, atMin >= 5 ? C_FILL : C_DIM);
        flashFrame(g, cx - max - 2, cy - max - 2, max * 2 + 5, max * 2 + 5);
        label(g, font, Component.translatable("rpsports.ui.pullup", reps), x, y + 52);
    }
}
