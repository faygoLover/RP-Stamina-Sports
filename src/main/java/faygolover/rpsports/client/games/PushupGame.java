package faygolover.rpsports.client.games;

import faygolover.rpsports.network.SportsNet;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Отжимания: ритм, как тайминг в A Way Out — по дуге вокруг клавиши E за один такт бежит точка; нажать, когда она в
 * светлом отрезке. Одно нажатие — вниз, следующее — вверх, две удачные половинки — повтор.
 *
 * <p>Отрезок — всегда одной длины (доля такта), сложность растёт только темпом: каждый повтор чуть быстрее. Ошибка
 * (рано, поздно, пропустил) немного замедляет темп (на три шага назад) и сбрасывает серию.
 */
public class PushupGame extends Minigame {
    private static final float START_PERIOD = 1.1f, MIN_PERIOD = 0.5f, STEP = 0.97f, ZONE_AT = 0.75f,
            ARC_FROM = 0.06f, ARC_TO = 0.94f;
    private float period = START_PERIOD, clock, down;
    /** Номер такта, в котором уже нажали / уже засчитан пропуск. */
    private int hitCycle = -1, missCycle = -1;
    private boolean half;

    public PushupGame(SportsNet.Open p) {
        super(p);
    }

    /** Полуширина отрезка, доля такта. */
    private float window() {
        float r = Math.max(0.6f, params.ratio());
        return Math.max(0.05f, Math.min(0.2f, 0.13f * strengthEase() * params.ease() / r * (tremble ? 0.8f : 1f)));
    }

    private int cycle() {
        return (int) Math.floor(clock / period);
    }

    private float phase() {
        return clock / period - cycle();
    }

    /** Сменить темп, не сбивая точку на дуге. */
    private void setPeriod(float p) {
        float beats = clock / period;
        period = p;
        clock = beats * period;
    }

    @Override
    protected void update(float dt) {
        clock += dt;
        down = approach(down, half ? 1 : 0, dt, 9);
        int n = cycle();
        if (phase() > ZONE_AT + window() && hitCycle != n && missCycle != n) {
            missCycle = n;
            miss();
        }
    }

    private void miss() {
        rep(false, 0);
        half = false;
        setPeriod(Math.min(START_PERIOD, period / (STEP * STEP * STEP)));
    }

    @Override
    public boolean owns(int key) {
        return key == GLFW.GLFW_KEY_E;
    }

    @Override
    protected void key(int key, int action) {
        if (key != GLFW.GLFW_KEY_E || action != GLFW.GLFW_PRESS) return;
        int n = cycle();
        if (hitCycle == n || Math.abs(phase() - ZONE_AT) > window()) {
            missCycle = n;
            miss();
            return;
        }
        hitCycle = n;
        if (half) {
            half = false;
            rep(true, 0);
            setPeriod(Math.max(MIN_PERIOD, period * STEP));
        } else {
            half = true;
        }
    }

    @Override
    public float poseA() {
        return down;
    }

    private static float onArc(float f) {
        return ARC_FROM + (ARC_TO - ARC_FROM) * f;
    }

    @Override
    protected int draw(GuiGraphics g, Font font, int x, int y) {
        float cx = x + W / 2f, cy = y + 26, r = 15;
        Qte.small(g, font, Component.translatable(half ? "rpsports.ui.up" : "rpsports.ui.down"), cx, y, Qte.GOLD);
        Qte.arc(g, cx, cy, r, 2, ARC_FROM, ARC_TO, Qte.GOLD_DIM);
        float w = window();
        Qte.arc(g, cx, cy, r, 4.5f, onArc(ZONE_AT - w), onArc(ZONE_AT + w), flashColor(Qte.GOLD));
        Qte.dot(g, cx, cy, r, onArc(phase()), 4.5f, Qte.WHITE);
        Qte.key(g, font, cx, cy, 9, "E", press(GLFW.GLFW_KEY_E), true);
        return 46;
    }
}
