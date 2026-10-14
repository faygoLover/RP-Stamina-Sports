package faygolover.rpsports.client.games;

import faygolover.rpsports.core.Exercise;
import faygolover.rpsports.item.PlateItem;
import faygolover.rpsports.network.SportsNet;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.Map;

/**
 * Мини-игра упражнения (клиент) — QTE в духе A Way Out: минимум текста, нарисованные клавиши, которые нажимаются,
 * кольца и дуги. Маленький виджет внизу по центру (docs/00 «Интерфейс»). Сама решает, удался ли повтор, и сообщает
 * серверу; отдаёт позу для анимации ({@link #poseA}, {@link #poseB}).
 *
 * <p>Под виджетом — мелко: повторы в подходе, серия, рекорд. Дрожь (мало выносливости, устала зона) — весь виджет
 * плавно дрожит по обеим осям (не скачками по пикселям).
 */
public abstract class Minigame {
    public static final int W = 120;

    protected final SportsNet.Open params;
    protected final RandomSource random = RandomSource.create();
    protected boolean tremble;
    /** Сила дрожи 0…1 от сервера: появляется, как устал, растёт до 1, когда выжат совсем. */
    private float trembleLevel;
    /** Удачных повторов в подходе (своих; сервер присылает свои — берём большее), серия удачных подряд, лучшая серия. */
    protected int reps, series, bestSeries;
    /** Объём подхода — повторы × вес (штанга, гантели). */
    protected float volume;
    /** Вспышка после повтора: >0 — успех, <0 — ошибка (секунды). */
    protected float flash;
    protected float time;
    private float trembleAmp;
    /** Нажатие клавиш для анимации значков: клавиша → оставшееся время «нажатости». */
    private final Map<Integer, Float> pressed = new HashMap<>();
    private final Map<Integer, Boolean> held = new HashMap<>();

    protected Minigame(SportsNet.Open params) {
        this.params = params;
    }

    public Exercise exercise() {
        return Exercise.VALUES[params.exercise()];
    }

    /** Секунд с начала упражнения. */
    public float elapsed() {
        return time;
    }

    /** Сила и Телосложение выше обычного (3) — легче. */
    protected float strengthEase() {
        return 1f + 0.04f * (params.strength() - 3);
    }

    protected float fitnessEase() {
        return 1f + 0.04f * (params.fitness() - 3);
    }

    public void setTremble(float level) {
        trembleLevel = Math.max(0, Math.min(1, level));
        tremble = trembleLevel > 0.01f;
    }

    public void setReps(int r) {
        reps = Math.max(reps, r);
    }

    /** Шаг, {@code dt} — секунды. */
    public void tick(float dt) {
        time += dt;
        if (flash > 0) flash = Math.max(0, flash - dt);
        else if (flash < 0) flash = Math.min(0, flash + dt);
        trembleAmp = approach(trembleAmp, trembleLevel, dt, 2);
        pressed.replaceAll((k, v) -> Math.max(0, v - dt));
        update(dt);
    }

    protected abstract void update(float dt);

    /** Клавиша (GLFW): нажатие, повтор или отпускание. */
    public final void keyEvent(int key, int action) {
        if (action == org.lwjgl.glfw.GLFW.GLFW_PRESS) pressed.put(key, 0.12f);
        held.put(key, action != org.lwjgl.glfw.GLFW.GLFW_RELEASE);
        key(key, action);
    }

    /** Кнопка мыши (0 — левая, 1 — правая), только нажатие. Для значков — как клавиши −1 и −2. */
    public final void mouseEvent(int button) {
        pressed.put(-1 - button, 0.12f);
        mouse(button);
    }

    protected void key(int key, int action) {}

    protected void mouse(int button) {}

    /** Насколько «нажат» значок клавиши: 1 — только что нажата (или держится), 0 — нет. Мышь: −1 левая, −2 правая. */
    protected float press(int key) {
        if (Boolean.TRUE.equals(held.get(key))) return 1;
        return Math.min(1, pressed.getOrDefault(key, 0f) / 0.12f);
    }

    /** Какие клавиши мини-игра забирает себе (абсолютный приоритет на время упражнения). */
    public abstract boolean owns(int key);

    public boolean ownsMouse() {
        return false;
    }

    public abstract float poseA();

    public float poseB() {
        return 0;
    }

    /** Повтор окончен: сообщить серверу. {@code kg} — вес этого повтора (для объёма), 0 — без веса. */
    protected void rep(boolean success, int extra, float kg) {
        flash = success ? 0.35f : -0.5f;
        if (success) {
            reps++;
            series++;
            bestSeries = Math.max(bestSeries, series);
            volume += kg;
        } else {
            series = 0;
        }
        SportsNet.toServer(new SportsNet.Rep(success, extra));
    }

    protected void rep(boolean success, int extra) {
        rep(success, extra, 0);
    }

    /** Нарисовать виджет: (x, y) — верхний левый угол, ширина {@link #W}. */
    public final void render(GuiGraphics g, Font font, int x, int y) {
        g.pose().pushPose();
        if (trembleAmp > 0.01f) {
            // Дрожь рук: быстрые мелкие колебания + медленный увод, по обеим осям.
            float dx = (float) (Math.sin(time * 41) * 0.55 + Math.sin(time * 23 + 1.3) * 0.35 + Math.sin(time * 7 + 0.4) * 0.5);
            float dy = (float) (Math.sin(time * 37 + 2.1) * 0.55 + Math.sin(time * 19 + 0.7) * 0.35 + Math.sin(time * 5 + 2.6) * 0.4);
            g.pose().translate(dx * 1.8f * trembleAmp, dy * 1.5f * trembleAmp, 0);
        }
        int h = draw(g, font, x, y);
        stats(g, font, x + W / 2f, y + h + 2);
        g.pose().popPose();
    }

    /** Нарисовать содержимое; вернуть его высоту (строка статистики — ниже). */
    protected abstract int draw(GuiGraphics g, Font font, int x, int y);

    /** Значение подхода для рекорда — лучшая серия. */
    protected float setValue() {
        return bestSeries;
    }

    private void stats(GuiGraphics g, Font font, float cx, float y) {
        float rec = Math.max(params.record(), setValue());
        boolean beaten = setValue() > params.record() && params.record() > 0;
        Component line = Component.translatable("rpsports.ui.stats", reps, series, PlateItem.format(rec));
        Qte.small(g, font, line, cx, y, beaten ? Qte.GOLD_ZONE : Qte.WHITE_DIM);
    }

    // ------------------------------------------------------------------ плавность

    /** Плавная «дрожь» значения: медленный шум −1…1 (сумма синусов). */
    protected float wobble(float speed) {
        return (float) (Math.sin(time * speed) * 0.6 + Math.sin(time * speed * 1.7 + 2) * 0.4);
    }

    /** Догнать цель плавно: {@code rate} — во сколько быстрее (≈ 1/время). */
    protected static float approach(float cur, float target, float dt, float rate) {
        return cur + (target - cur) * (1 - (float) Math.exp(-rate * dt));
    }

    /** Цвет значка после повтора: вспышка успеха/ошибки. */
    protected int flashColor(int normal) {
        if (flash > 0) return Qte.GOOD;
        if (flash < 0) return Qte.BAD;
        return normal;
    }

    protected static float clamp(float v) {
        return Math.max(0, Math.min(1, v));
    }
}
