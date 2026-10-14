package faygolover.rpsports.client.games;

import faygolover.rpsports.network.SportsNet;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Беговая дорожка: шаги по очереди — ЛКМ и ПКМ или Q и E (левая, правая); чем чаще, тем выше темп. Две одинаковые
 * подряд — споткнулся. Чем выше темп, тем быстрее он сам падает (ползунок сильнее тянет влево). Программа просит
 * темп отрезками по 20 с; почти весь отрезок в зоне — повтор засчитан; вне зоны дольше 3 с — сбился.
 */
public class TreadmillGame extends Minigame {
    private static final float[][] PROGRAMS = {
            {0.3f, 0.3f, 0.35f, 0.3f, 0.35f, 0.3f},
            {0.3f, 0.5f, 0.8f, 0.5f, 0.9f, 0.3f},
            {0.6f, 0.7f, 0.8f, 0.85f, 0.7f, 0.6f}};
    private static final float SEGMENT = 20f, STEP = 0.11f;
    private final float[] program;
    private float tempo, shown, target, segTime, inZone, outZone;
    private int segment, lastFoot = -1;

    public TreadmillGame(SportsNet.Open p) {
        super(p);
        program = PROGRAMS[Math.max(0, Math.min(2, p.program()))];
        target = program[0];
    }

    private float zoneHalf() {
        return Math.min(0.2f, 0.1f * fitnessEase() * params.ease());
    }

    @Override
    protected void update(float dt) {
        // Темп падает тем быстрее, чем он выше.
        tempo = Math.max(0, tempo - (0.12f + 0.5f * tempo) * dt);
        shown = approach(shown, tempo, dt, 8);
        float wanted = program[segment % program.length];
        target = approach(target, wanted, dt, 0.5f);
        float drift = tremble ? 0.05f * wobble(2.2f) : 0;
        boolean in = Math.abs(shown + drift - target) <= zoneHalf();
        segTime += dt;
        if (in) {
            inZone += dt;
            outZone = 0;
        } else {
            outZone += dt;
        }
        if (outZone > 3) {
            rep(false, Math.round(tempo * 100));
            restartSegment();
            return;
        }
        if (segTime >= SEGMENT) {
            rep(inZone / SEGMENT >= 0.8f, Math.round(tempo * 100));
            segment++;
            restartSegment();
        }
    }

    private void restartSegment() {
        segTime = 0;
        inZone = 0;
        outZone = 0;
    }

    private void step(int foot) {
        if (foot == lastFoot) tempo = Math.max(0, tempo - 0.3f); // споткнулся
        else tempo = Math.min(1, tempo + STEP);
        lastFoot = foot;
    }

    @Override
    public boolean ownsMouse() {
        return true;
    }

    @Override
    public boolean owns(int key) {
        return key == GLFW.GLFW_KEY_Q || key == GLFW.GLFW_KEY_E;
    }

    @Override
    public void key(int key, int action) {
        if (action != GLFW.GLFW_PRESS) return;
        if (key == GLFW.GLFW_KEY_Q) step(0);
        else if (key == GLFW.GLFW_KEY_E) step(1);
    }

    @Override
    public void mouse(int button) {
        if (button == 0 || button == 1) step(button);
    }

    @Override
    public float poseA() {
        return shown;
    }

    @Override
    protected int draw(GuiGraphics g, Font font, int x, int y) {
        float cx = x + W / 2f;
        Qte.small(g, font, Component.translatable("rpsports.ui.treadmill", segment % program.length + 1, program.length), cx, y, Qte.GOLD);
        float h = zoneHalf();
        Qte.track(g, x + 6, y + 10, W - 12, target - h, target + h, shown, flashColor(Qte.WHITE));
        // Время отрезка — тонкая линия под шкалой.
        g.fill(x + 6, y + 15, x + 6 + Math.round((W - 12) * (1 - segTime / SEGMENT)), y + 16, Qte.WHITE_DIM);
        // Шаги: следующая нога подсвечена.
        String lmb = Component.translatable("rpsports.ui.lmb").getString(), rmb = Component.translatable("rpsports.ui.rmb").getString();
        Qte.key(g, font, cx - 16, y + 30, 8.5f, "Q", Math.max(press(GLFW.GLFW_KEY_Q), press(-1)), lastFoot != 0);
        Qte.key(g, font, cx + 16, y + 30, 8.5f, "E", Math.max(press(GLFW.GLFW_KEY_E), press(-2)), lastFoot != 1);
        Qte.small(g, font, Component.literal(lmb), cx - 16, y + 41, Qte.WHITE_DIM);
        Qte.small(g, font, Component.literal(rmb), cx + 16, y + 41, Qte.WHITE_DIM);
        return 47;
    }
}
