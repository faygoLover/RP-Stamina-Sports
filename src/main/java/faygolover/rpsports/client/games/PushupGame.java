package faygolover.rpsports.client.games;

import faygolover.rpsports.network.SportsNet;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Отжимания: маятник. Точка катается по дуге под клавишей E туда-сюда, как на горке; зона успеха — всегда внизу по
 * центру. Нажать, когда точка проходит зону: одно нажатие — вниз, следующее (на обратном проходе) — вверх, две
 * удачные половинки — повтор.
 *
 * <p>С каждым повтором маятник чуть быстрее, а зона чуть уже. Ошибка (рано, поздно, пропустил проход) — на три
 * шага легче и сбрасывает серию.
 */
public class PushupGame extends Minigame {
    private static final float START_PERIOD = 2.2f, MIN_PERIOD = 1.0f, STEP = 0.97f;
    /** Дуга маятника: от правого края через низ к левому (доли круга, 0 — сверху). */
    private static final float ARC_FROM = 0.27f, ARC_TO = 0.73f;
    /** Сложность: сколько шагов прошли (каждый — быстрее и уже). */
    private int level;
    private float clock, down;
    /** Номер прохода через низ, в котором уже нажали / уже засчитан пропуск. */
    private int hitPass = 0, missPass = 0;
    private boolean half;

    public PushupGame(SportsNet.Open p) {
        super(p);
    }

    /** Полный размах туда-обратно, секунды; за него точка дважды проходит низ. */
    private float period() {
        return Math.max(MIN_PERIOD, START_PERIOD * (float) Math.pow(STEP, level));
    }

    /** Полуширина зоны в долях размаха (−1…1). */
    private float zone() {
        float r = Math.max(0.6f, params.ratio());
        float base = 0.3f * strengthEase() * params.ease() / r * (tremble ? 0.8f : 1f);
        return Math.max(0.08f, Math.min(0.4f, base * (float) Math.pow(0.96, level)));
    }

    /** Положение точки −1…1 (0 — низ). */
    private float swing() {
        return (float) Math.sin(clock / period() * Math.PI * 2);
    }

    /** Номер ближайшего прохода через низ (проходы — каждые полпериода, первый — в начале). */
    private int nearestPass() {
        return Math.round(clock / (period() / 2));
    }

    /** Сменить темп, не сбивая точку. */
    private void setLevel(int l) {
        float beats = clock / period();
        level = Math.max(0, l);
        clock = beats * period();
    }

    @Override
    protected void update(float dt) {
        clock += dt;
        down = approach(down, half ? 1 : 0, dt, 9);
        // Проход n кончился (точка вышла из зоны после него), а нажатия не было — пропустил.
        float halfPeriod = period() / 2;
        int n = (int) Math.floor(clock / halfPeriod);
        float since = clock - n * halfPeriod;
        boolean leftZone = Math.abs(swing()) > zone() && since < halfPeriod / 2;
        if (n >= 1 && leftZone && hitPass != n && missPass != n) {
            missPass = n;
            miss();
        }
    }

    private void miss() {
        rep(false, 0);
        half = false;
        setLevel(level - 3);
    }

    @Override
    public boolean owns(int key) {
        return key == GLFW.GLFW_KEY_E;
    }

    @Override
    protected void key(int key, int action) {
        if (key != GLFW.GLFW_KEY_E || action != GLFW.GLFW_PRESS) return;
        int n = nearestPass();
        if (n < 1 || hitPass == n || Math.abs(swing()) > zone()) {
            missPass = Math.max(missPass, n);
            miss();
            return;
        }
        hitPass = n;
        if (half) {
            half = false;
            rep(true, 0);
            setLevel(level + 1);
        } else {
            half = true;
        }
    }

    @Override
    public float poseA() {
        return down;
    }

    private static float onArc(float x) {
        return (ARC_FROM + ARC_TO) / 2 + (ARC_TO - ARC_FROM) / 2 * x;
    }

    @Override
    protected int draw(GuiGraphics g, Font font, int x, int y) {
        float cx = x + W / 2f, cy = y + 22, r = 16;
        Qte.small(g, font, Component.translatable(half ? "rpsports.ui.up" : "rpsports.ui.down"), cx, y, Qte.GOLD);
        Qte.arc(g, cx, cy, r, 2, ARC_FROM, ARC_TO, Qte.GOLD_DIM);
        float z = zone();
        Qte.arc(g, cx, cy, r, 4.5f, onArc(-z), onArc(z), flashColor(Qte.GOLD));
        Qte.dot(g, cx, cy, r, onArc(swing()), 3f, Qte.WHITE);
        Qte.key(g, font, cx, cy, 9, "E", press(GLFW.GLFW_KEY_E), true);
        return 42;
    }
}
