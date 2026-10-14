package faygolover.rpsports.client.games;

import faygolover.rpsports.network.SportsNet;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Беговая дорожка: ЛКМ и ПКМ по очереди — шаги; чем чаще, тем выше темп. Две одинаковые подряд — споткнулся.
 * Программа просит темп отрезками по 20 с (разминка → ускорение → рывок → отдых). Почти весь отрезок в зоне — повтор
 * засчитан; вне зоны дольше 3 с — сбился.
 */
public class TreadmillGame extends Minigame {
    private static final float[][] PROGRAMS = {
            {0.3f, 0.3f, 0.35f, 0.3f, 0.35f, 0.3f},
            {0.3f, 0.5f, 0.8f, 0.5f, 0.9f, 0.3f},
            {0.6f, 0.7f, 0.8f, 0.85f, 0.7f, 0.6f}};
    private static final float SEGMENT = 20f;
    private final float[] program;
    private float tempo, target, segTime, inZone, outZone;
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
        tempo = Math.max(0, tempo - 0.35f * dt);
        float wanted = program[segment % program.length];
        target += (wanted - target) * Math.min(1, dt * 0.5f);
        float drift = tremble ? (random.nextFloat() - 0.5f) * 0.08f : 0;
        boolean in = Math.abs(tempo + drift - target) <= zoneHalf();
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

    @Override
    public boolean ownsMouse() {
        return true;
    }

    @Override
    public boolean owns(int key) {
        return false;
    }

    @Override
    public boolean mouse(int button) {
        if (button != 0 && button != 1) return false;
        if (button == lastFoot) tempo = Math.max(0, tempo - 0.3f); // споткнулся
        else tempo = Math.min(1, tempo + 0.12f);
        lastFoot = button;
        return true;
    }

    @Override
    public float poseA() {
        return tempo;
    }

    @Override
    public float poseB() {
        return time;
    }

    @Override
    public void render(GuiGraphics g, Font font, int x, int y) {
        g.fill(x, y, x + W, y + 7, C_BG);
        float h = zoneHalf();
        zone(g, x, y, W, 7, target - h, target + h);
        int mx = x + 1 + (int) (clamp(tempo) * (W - 3)) + jitter();
        g.fill(mx, y - 1, mx + 2, y + 8, 0xFFFFFFFF);
        bar(g, x, y + 8, W, 3, 1 - segTime / SEGMENT, C_TIME);
        flashFrame(g, x, y, W, 11);
        label(g, font, Component.translatable("rpsports.ui.treadmill", segment % program.length + 1, program.length), x, y + 14);
    }
}
