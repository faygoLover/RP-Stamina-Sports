package faygolover.rpsports.client.games;

import faygolover.rpsports.core.Exercise;
import faygolover.rpsports.network.SportsNet;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import org.lwjgl.glfw.GLFW;

/**
 * Мини-игра упражнения (клиент). Маленький виджет внизу по центру, без отдельного окна (docs/00 «Интерфейс»). Сама
 * решает, удался ли повтор, и сообщает серверу; отдаёт позу для анимации ({@link #poseA}, {@link #poseB}).
 */
public abstract class Minigame {
    public static final int W = 120;
    protected static final int C_BG = 0xA0000000, C_FILL = 0xFFE3B341, C_TIME = 0xFF6BA8E0, C_ZONE = 0xFF4CB050,
            C_BAD = 0xFFE06C6C, C_TEXT = 0xFFE6E6E6, C_DIM = 0xFF9AA0AA;

    protected final SportsNet.Open params;
    protected final RandomSource random = RandomSource.create();
    protected boolean tremble;
    protected int reps;
    /** Вспышка после повтора: >0 — успех, <0 — ошибка (секунды). */
    protected float flash;
    protected float time;

    protected Minigame(SportsNet.Open params) {
        this.params = params;
    }

    public Exercise exercise() {
        return Exercise.VALUES[params.exercise()];
    }

    /** Сила и Телосложение выше обычного (3) — легче. */
    protected float strengthEase() {
        return 1f + 0.04f * (params.strength() - 3);
    }

    protected float fitnessEase() {
        return 1f + 0.04f * (params.fitness() - 3);
    }

    public void setTremble(boolean t) {
        tremble = t;
    }

    public void setReps(int r) {
        reps = r;
    }

    /** Шаг, {@code dt} — секунды. */
    public void tick(float dt) {
        time += dt;
        if (flash > 0) flash = Math.max(0, flash - dt);
        else if (flash < 0) flash = Math.min(0, flash + dt);
        update(dt);
    }

    protected abstract void update(float dt);

    /** Клавиша (GLFW): true — съели. */
    public boolean key(int key, int action) {
        return false;
    }

    /** Кнопка мыши (0 — левая, 1 — правая), только нажатие: true — съели. */
    public boolean mouse(int button) {
        return false;
    }

    /** Какие клавиши мини-игра забирает себе (абсолютный приоритет на время упражнения). */
    public boolean owns(int key) {
        return key == GLFW.GLFW_KEY_E || key == GLFW.GLFW_KEY_R || key == GLFW.GLFW_KEY_T || key == GLFW.GLFW_KEY_F;
    }

    public boolean ownsMouse() {
        return false;
    }

    public abstract float poseA();

    public float poseB() {
        return 0;
    }

    /** Повтор окончен: сообщить серверу. */
    protected void rep(boolean success, int extra) {
        flash = success ? 0.35f : -0.5f;
        if (success) reps++;
        SportsNet.toServer(new SportsNet.Rep(success, extra));
    }

    /** Нарисовать виджет: (x, y) — верхний левый угол. */
    public abstract void render(GuiGraphics g, Font font, int x, int y);

    // ------------------------------------------------------------------ рисование

    protected int jitter() {
        return tremble ? random.nextInt(3) - 1 : 0;
    }

    protected void bar(GuiGraphics g, int x, int y, int w, int h, float frac, int color) {
        g.fill(x, y, x + w, y + h, C_BG);
        int f = (int) (Math.max(0, Math.min(1, frac)) * (w - 2));
        if (f > 0) g.fill(x + 1, y + 1, x + 1 + f, y + h - 1, color);
    }

    protected void zone(GuiGraphics g, int x, int y, int w, int h, float from, float to) {
        int a = x + 1 + (int) (Math.max(0, from) * (w - 2)), b = x + 1 + (int) (Math.min(1, to) * (w - 2));
        if (b > a) g.fill(a, y, b, y + h, 0x704CB050);
    }

    protected void flashFrame(GuiGraphics g, int x, int y, int w, int h) {
        if (flash == 0) return;
        int c = flash > 0 ? 0xFF4CB050 : 0xFFE06C6C;
        g.renderOutline(x - 1, y - 1, w + 2, h + 2, c);
    }

    protected void label(GuiGraphics g, Font font, Component text, int x, int y) {
        g.drawString(font, text, x, y, C_TEXT, true);
    }

    protected static float clamp(float v) {
        return Math.max(0, Math.min(1, v));
    }
}
