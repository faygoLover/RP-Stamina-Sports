package faygolover.rpsports.client.games;

import faygolover.rpsports.network.SportsNet;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Турник: из центра круга расширяется кольцо (скорость каждый раз случайная, до ×2). Зелёная зона прижата к внешнему
 * краю круга; нажать показанную клавишу (Q или E), пока кольцо в ней. Каждый успех сужает зону — её внутренний край
 * отходит к внешнему. На самой тонкой — ещё 5 повторов на E, потом клавиша каждый раз случайная (Q или E).
 *
 * <p>Пока растёт кольцо, персонаж подтягивается; кольцо дошло до края — это верхняя точка «без рывка». Успел нажать —
 * рывок выше (подбородок над перекладиной). Не успел или промахнулся — выше не поднимется, опускается.
 */
public class PullupGame extends Minigame {
    private static final int[] KEYS = {GLFW.GLFW_KEY_Q, GLFW.GLFW_KEY_E};
    private static final String[] NAMES = {"Q", "E"};
    /** Высота позы, когда кольцо дошло до края (выше — только рывок). */
    private static final float EDGE_POSE = 0.8f;
    private static final float W_MIN = 0.1f, W_MAX = 0.32f, JERK = 0.45f, HOLD = 0.25f, LOWER = 0.7f;

    private enum Phase { GROW, JERK, HOLD, LOWER }

    private Phase phase = Phase.GROW;
    private float ring, speed, width, phaseTime, pose, poseFrom;
    private int atMin, keyIndex = 1;

    public PullupGame(SportsNet.Open p) {
        super(p);
        width = Math.max(W_MIN, Math.min(W_MAX, 0.24f * p.ease() * strengthEase()));
        next();
    }

    private void next() {
        ring = 0;
        phase = Phase.GROW;
        phaseTime = 0;
        // Сильнее — кольцо растёт медленнее; тяжелее тело — быстрее.
        float base = 1f / 1.4f * Math.max(0.6f, params.ratio()) / strengthEase();
        speed = base * (1 + random.nextFloat());
        keyIndex = atMin >= 5 ? random.nextInt(KEYS.length) : 1;
    }

    private void to(Phase p) {
        phase = p;
        phaseTime = 0;
        poseFrom = pose;
    }

    @Override
    protected void update(float dt) {
        phaseTime += dt;
        switch (phase) {
            case GROW -> {
                float uneven = tremble ? 0.25f * wobble(3.5f) : 0;
                ring = Math.min(1, ring + dt * speed * (1 + uneven));
                pose = approach(pose, ring * EDGE_POSE, dt, 20);
                if (ring >= 1) {
                    rep(false, 0); // не успел
                    to(Phase.HOLD);
                }
            }
            case JERK -> {
                pose = approach(pose, 1f, dt, 14);
                if (phaseTime >= JERK) to(Phase.LOWER);
            }
            case HOLD -> {
                if (phaseTime >= HOLD) to(Phase.LOWER);
            }
            case LOWER -> {
                float t = Math.min(1, phaseTime / LOWER);
                pose = poseFrom * (1 - t);
                ring = Math.max(0, ring - dt / LOWER);
                if (t >= 1) next();
            }
        }
    }

    @Override
    public boolean owns(int key) {
        return key == GLFW.GLFW_KEY_Q || key == GLFW.GLFW_KEY_E;
    }

    @Override
    public void key(int key, int action) {
        if (action != GLFW.GLFW_PRESS || phase != Phase.GROW || !owns(key)) return;
        boolean inZone = ring >= 1 - width;
        if (inZone && key == KEYS[keyIndex]) {
            rep(true, 0);
            if (width > W_MIN + 1e-4) width = Math.max(W_MIN, width - 0.02f);
            else atMin++;
            to(Phase.JERK);
        } else {
            rep(false, 0);
            to(Phase.LOWER);
        }
    }

    @Override
    public float poseA() {
        return pose;
    }

    @Override
    protected int draw(GuiGraphics g, Font font, int x, int y) {
        float cx = x + W / 2f, cy = y + 30, max = 21;
        Qte.small(g, font, Component.translatable("rpsports.ui.press"), cx, y, Qte.GOLD);
        Qte.disk(g, cx, cy, max + 2, Qte.SHADE);
        // Зона — у внешнего края: от (1 − ширина) до края.
        Qte.arc(g, cx, cy, max * (1 - width / 2), max * width, 0, 1, Qte.GOLD_DIM);
        Qte.arc(g, cx, cy, max, 1, 0, 1, Qte.GOLD);
        if (phase == Phase.GROW || ring > 0.02f) {
            boolean in = ring >= 1 - width;
            Qte.arc(g, cx, cy, Math.max(1, ring * max), 1.6f, 0, 1, phase == Phase.JERK ? Qte.GOOD : in ? Qte.GOLD_ZONE : Qte.WHITE);
        }
        Qte.key(g, font, cx, cy, 7.5f, NAMES[keyIndex], press(KEYS[keyIndex]), phase == Phase.GROW);
        return 56;
    }
}
