package faygolover.rpsports.client.games;

import faygolover.rpsports.core.Exercise;
import faygolover.rpsports.network.SportsNet;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

/**
 * Мини-игра упражнения (клиент). Маленький виджет внизу по центру, без отдельного окна (docs/00 «Интерфейс»). Сама
 * решает, удался ли повтор, и сообщает серверу; отдаёт позу для анимации ({@link #poseA}, {@link #poseB}).
 *
 * <p>Под виджетом — подсказка клавиш. Дрожь (мало выносливости, устала зона) — весь виджет плавно и чуть-чуть
 * покачивается целиком, на целые пиксели.
 */
public abstract class Minigame {
    public static final int W = 120;
    protected static final int C_BG = 0xA0000000, C_FILL = 0xFFE3B341, C_TIME = 0xFF6BA8E0, C_ZONE = 0xFF4CB050,
            C_ZONE_SOFT = 0x904CB050, C_BAD = 0xFFE06C6C, C_TEXT = 0xFFE6E6E6, C_DIM = 0xFF9AA0AA, C_MARK = 0xFFFFFFFF;

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

    /** Секунд с начала упражнения. */
    public float elapsed() {
        return time;
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

    /** Клавиша (GLFW): нажатие, повтор или отпускание. */
    public void key(int key, int action) {}

    /** Кнопка мыши (0 — левая, 1 — правая), только нажатие. */
    public void mouse(int button) {}

    /** Какие клавиши мини-игра забирает себе (абсолютный приоритет на время упражнения). */
    public abstract boolean owns(int key);

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
    public final void render(GuiGraphics g, Font font, int x, int y) {
        int dx = shake();
        g.pose().pushPose();
        g.pose().translate(dx, 0, 0);
        int h = draw(g, font, x, y);
        g.pose().popPose();
        Component hint = Component.translatable("rpsports.hint." + exercise().id);
        int hy = y + h + 2;
        for (var line : font.split(hint, W + 60)) {
            g.drawCenteredString(font, line, x + W / 2, hy, C_DIM);
            hy += 9;
        }
    }

    /** Нарисовать содержимое; вернуть его высоту (подсказка — ниже). */
    protected abstract int draw(GuiGraphics g, Font font, int x, int y);

    // ------------------------------------------------------------------ плавность

    /** Сдвиг дрожи: медленное покачивание на 1–2 пикселя (целые пиксели — без полупрозрачных краёв). */
    private int shake() {
        if (!tremble) return 0;
        return Math.round((float) (Math.sin(time * 5.0) * 1.1 + Math.sin(time * 2.3 + 1) * 0.7));
    }

    /** Плавная «дрожь» значения: медленный шум −1…1 (сумма синусов). */
    protected float wobble(float speed) {
        return (float) (Math.sin(time * speed) * 0.6 + Math.sin(time * speed * 1.7 + 2) * 0.4);
    }

    /** Догнать цель плавно: {@code rate} — во сколько быстрее (≈ 1/время). */
    protected static float approach(float cur, float target, float dt, float rate) {
        return cur + (target - cur) * (1 - (float) Math.exp(-rate * dt));
    }

    // ------------------------------------------------------------------ рисование

    protected void bar(GuiGraphics g, int x, int y, int w, int h, float frac, int color) {
        g.fill(x, y, x + w, y + h, C_BG);
        int f = Math.round(Math.max(0, Math.min(1, frac)) * (w - 2));
        if (f > 0) g.fill(x + 1, y + 1, x + 1 + f, y + h - 1, color);
    }

    protected void zone(GuiGraphics g, int x, int y, int w, int h, float from, float to) {
        int a = x + 1 + Math.round(Math.max(0, from) * (w - 2)), b = x + 1 + Math.round(Math.min(1, to) * (w - 2));
        if (b > a) g.fill(a, y + 1, b, y + h - 1, C_ZONE_SOFT);
    }

    /** Маркер на полосе: {@code frac} 0…1. */
    protected void marker(GuiGraphics g, int x, int y, int w, int h, float frac) {
        int mx = x + 1 + Math.round(clamp(frac) * (w - 4));
        g.fill(mx, y - 1, mx + 2, y + h + 1, C_MARK);
    }

    protected void flashFrame(GuiGraphics g, int x, int y, int w, int h) {
        if (flash == 0) return;
        g.renderOutline(x - 1, y - 1, w + 2, h + 2, flash > 0 ? C_ZONE : C_BAD);
    }

    protected void label(GuiGraphics g, Font font, Component text, int x, int y) {
        g.drawString(font, text, x, y, C_TEXT, true);
    }

    protected static float clamp(float v) {
        return Math.max(0, Math.min(1, v));
    }
}
