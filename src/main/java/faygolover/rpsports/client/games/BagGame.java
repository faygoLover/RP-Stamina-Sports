package faygolover.rpsports.client.games;

import faygolover.rpsports.network.SportsNet;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Груша — как Guitar Hero: четыре дорожки ударов, сверху падают ячейки, бить, когда ячейка на линии. Заранее видно,
 * что дальше. Дорожки: Q — левый боковой, ЛКМ — левый прямой, ПКМ — правый прямой, E — правый боковой. Иногда
 * «двойка» — две ячейки разными руками подряд, но не одновременно. Со временем ячейки падают быстрее и чаще.
 * Мимо (не та дорожка, пусто) или пропустил ячейку — ошибка, серия сначала. Два удара почти сразу — второй не идёт.
 */
public class BagGame extends Minigame {
    private static final int LANES = 4, LANE_W = 24, GAP = 4, FIELD_H = 40;
    /** Между ударами — не меньше этого (одновременно ударить двумя руками нельзя). */
    private static final float COOLDOWN = 0.12f, DOUBLE_GAP = 0.24f;

    private static final class Note {
        final int lane;
        final float at;
        boolean done;

        Note(int lane, float at) {
            this.lane = lane;
            this.at = at;
        }
    }

    private final List<Note> notes = new ArrayList<>();
    private float nextAt = 1.5f, lastPress = -1, punch;
    private int punchLane = 1;

    public BagGame(SportsNet.Open p) {
        super(p);
    }

    /** Сколько секунд ячейка падает сверху до линии: от 1,6 до 0,85 с за первые ~75 с. */
    private float fallTime() {
        return Math.max(0.85f, 1.6f - time * 0.01f);
    }

    /** Через сколько следующая ячейка: от 1,5 до 0,9 с. */
    private float interval() {
        return Math.max(0.9f, 1.5f - time * 0.008f);
    }

    /** Окно удара вокруг линии, с: Телосложение, Ловкий, броня; дрожь — уже. */
    private float window() {
        return Math.max(0.08f, Math.min(0.22f, 0.14f * fitnessEase() * params.ease() * (tremble ? 0.8f : 1f)));
    }

    private static int hand(int lane) {
        return lane < 2 ? 0 : 1;
    }

    @Override
    protected void update(float dt) {
        if (punch > 0) punch = Math.max(0, punch - dt * 4);
        // Новые ячейки появляются сверху за время падения до своей линии.
        while (nextAt <= time + fallTime()) {
            int lane = random.nextInt(LANES);
            notes.add(new Note(lane, nextAt));
            if (random.nextFloat() < 0.2f) {
                // «Двойка»: вторая — другой рукой, чуть позже.
                int other = hand(lane) == 0 ? 2 + random.nextInt(2) : random.nextInt(2);
                notes.add(new Note(other, nextAt + DOUBLE_GAP));
                nextAt += DOUBLE_GAP;
            }
            nextAt += interval() * (0.8f + random.nextFloat() * 0.4f);
        }
        // Пропустил — ячейка ушла ниже окна.
        float w = window();
        for (Note n : notes) {
            if (!n.done && n.at < time - w) {
                n.done = true;
                fail(hand(n.lane));
            }
        }
        notes.removeIf(n -> n.done && n.at < time - 0.3f);
    }

    private void fail(int hand) {
        rep(false, hand);
    }

    private void hit(int lane) {
        if (time - lastPress < COOLDOWN) return;
        lastPress = time;
        punch = 1;
        punchLane = lane;
        float w = window();
        Note best = null;
        for (Note n : notes) {
            if (n.done || n.lane != lane || Math.abs(n.at - time) > w) continue;
            if (best == null || n.at < best.at) best = n;
        }
        if (best == null) {
            fail(hand(lane));
            return;
        }
        best.done = true;
        rep(true, hand(lane));
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
        if (key == GLFW.GLFW_KEY_Q) hit(0);
        else if (key == GLFW.GLFW_KEY_E) hit(3);
    }

    @Override
    public void mouse(int button) {
        if (button == 0) hit(1);
        else if (button == 1) hit(2);
    }

    @Override
    public float poseA() {
        return punch;
    }

    @Override
    public float poseB() {
        return punchLane;
    }

    @Override
    protected int draw(GuiGraphics g, Font font, int x, int y) {
        int total = LANES * LANE_W + (LANES - 1) * GAP, x0 = x + (W - total) / 2;
        int lineY = y + FIELD_H - 6;
        float fall = fallTime(), w = window();
        String[] keys = {"Q", Component.translatable("rpsports.ui.lmb").getString(), Component.translatable("rpsports.ui.rmb").getString(), "E"};
        int[] codes = {GLFW.GLFW_KEY_Q, -1, -2, GLFW.GLFW_KEY_E};
        int half = Math.max(1, Math.round(w / fall * (FIELD_H - 6)));
        for (int l = 0; l < LANES; l++) {
            int lx = x0 + l * (LANE_W + GAP);
            g.fill(lx + LANE_W / 2, y, lx + LANE_W / 2 + 1, lineY, Qte.GOLD_DIM);
            // Окно удара вокруг линии.
            g.fill(lx + 2, lineY - half, lx + LANE_W - 2, Math.min(y + FIELD_H, lineY + half), 0x40E8C34A);
            g.fill(lx + 1, lineY, lx + LANE_W - 1, lineY + 1, Qte.GOLD);
        }
        for (Note n : notes) {
            if (n.done) continue;
            float k = (n.at - time) / fall; // 1 — наверху, 0 — на линии
            float ny = lineY - k * (FIELD_H - 6);
            if (ny < y || ny > y + FIELD_H) continue;
            float lx = x0 + n.lane * (LANE_W + GAP) + LANE_W / 2f;
            Qte.disk(g, lx, ny, 7, hand(n.lane) == 0 ? Qte.GOLD : Qte.WHITE);
        }
        for (int l = 0; l < LANES; l++) {
            float lx = x0 + l * (LANE_W + GAP) + LANE_W / 2f;
            boolean hit = punch > 0 && punchLane == l;
            Qte.key(g, font, lx, y + FIELD_H + 6, 7.5f, keys[l], press(codes[l]), true);
            if (hit && flash != 0) Qte.arc(g, lx, y + FIELD_H + 6, 9, 1, 0, 1, flashColor(Qte.GOLD));
        }
        return FIELD_H + 15;
    }
}
