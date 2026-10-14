package faygolover.rpsports.client.games;

import faygolover.rpsports.network.SportsNet;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Гантели — как мешок Амбу: держать клавишу, кольцо вокруг неё растёт; отпустить в светлой зоне. Зона всегда на одном
 * месте, чем тяжелее гантель относительно Силы — тем уже. Отпустил вне зоны (или перетянул) — ошибка. Ходить можно.
 * Одна гантель — клавиша E; в обеих руках — Q левая, E правая, руки независимы. Дрожь — кольцо растёт неровно.
 */
public class DumbbellGame extends Minigame {
    private static final float ZONE_START = 0.7f, SPEED = 0.75f, DOWN_TIME = 0.6f;

    /** Одна рука. {@code extra} — для сервера: 0 — основная рука, 1 — вторая. */
    private final class Arm {
        final int key, extra;
        final float ratio, kg, phase;
        float fill, down;
        boolean holding;

        Arm(int key, int extra, float ratio, float kg) {
            this.key = key;
            this.extra = extra;
            this.ratio = ratio;
            this.kg = kg;
            this.phase = random.nextFloat() * 5;
        }

        float zoneWidth() {
            float r = Math.max(0.3f, ratio);
            float w = 0.2f / (float) Math.pow(r, 1.2) * params.ease() * strengthEase();
            return Math.max(0.06f, Math.min(0.28f, w));
        }

        void update(float dt) {
            if (down > 0) {
                down -= dt;
                fill = Math.max(0, fill - dt / DOWN_TIME);
                return;
            }
            if (holding) {
                float uneven = tremble ? 0.45f * wobble(4.5f + phase) : 0;
                fill += dt * SPEED * (1 + uneven);
                if (fill > 1) {
                    holding = false;
                    rep(false, extra);
                    down = DOWN_TIME;
                }
            }
        }

        void key(int action) {
            if (down > 0) return;
            if (action == GLFW.GLFW_PRESS) holding = true;
            else if (action == GLFW.GLFW_RELEASE && holding) {
                holding = false;
                boolean ok = fill >= ZONE_START && fill <= ZONE_START + zoneWidth();
                rep(ok, extra, ok ? kg : 0);
                down = DOWN_TIME;
            }
        }

        void draw(GuiGraphics g, Font font, float cx, float cy, String label) {
            Qte.arc(g, cx, cy, 14, 3, 0, 1, Qte.SHADE);
            Qte.arc(g, cx, cy, 14, 4.5f, ZONE_START, ZONE_START + zoneWidth(), Qte.GOLD_DIM);
            boolean in = fill >= ZONE_START && fill <= ZONE_START + zoneWidth();
            Qte.arc(g, cx, cy, 14, 3, 0, Math.min(1, fill), flash != 0 && down > 0 ? flashColor(Qte.GOLD) : in ? Qte.GOLD_ZONE : Qte.GOLD);
            Qte.key(g, font, cx, cy, 9.5f, label, press(key), down <= 0);
            Qte.small(g, font, Component.translatable("rpsports.ui.kg", (int) kg), cx, cy + 18, Qte.WHITE_DIM);
        }
    }

    private final boolean dual;
    /** Правая (основная) и левая; у одной гантели — одна {@code right} (рука — та, в которой гантель). */
    private final Arm right, left;

    public DumbbellGame(SportsNet.Open p) {
        super(p);
        dual = p.hand() == 2;
        right = new Arm(GLFW.GLFW_KEY_E, 0, p.ratio(), p.weightKg());
        left = dual ? new Arm(GLFW.GLFW_KEY_Q, 1, p.ratio2(), p.weightKg2()) : null;
    }

    @Override
    protected void update(float dt) {
        right.update(dt);
        if (left != null) left.update(dt);
    }

    @Override
    public boolean owns(int key) {
        return key == GLFW.GLFW_KEY_E || dual && key == GLFW.GLFW_KEY_Q;
    }

    @Override
    protected void key(int key, int action) {
        if (key == GLFW.GLFW_KEY_E) right.key(action);
        else if (dual && key == GLFW.GLFW_KEY_Q) left.key(action);
    }

    @Override
    public float poseA() {
        return right.fill;
    }

    /** Для анимации: одна гантель — рука (0 правая, 1 левая); две — 2 + подъём левой. */
    @Override
    public float poseB() {
        return dual ? 2 + left.fill : params.hand();
    }

    @Override
    protected int draw(GuiGraphics g, Font font, int x, int y) {
        float cx = x + W / 2f;
        Qte.small(g, font, Component.translatable("rpsports.ui.hold"), cx, y, Qte.GOLD);
        if (dual) {
            left.draw(g, font, cx - 22, y + 24, "Q");
            right.draw(g, font, cx + 22, y + 24, "E");
        } else {
            right.draw(g, font, cx, y + 24, "E");
        }
        return 50;
    }
}
