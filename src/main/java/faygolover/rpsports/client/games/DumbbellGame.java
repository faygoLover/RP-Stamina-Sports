package faygolover.rpsports.client.games;

import faygolover.rpsports.network.SportsNet;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Гантели — как мешок Амбу: держать E, полоска растёт; отпустить в зелёной зоне. Зона всегда на одном месте, чем
 * тяжелее гантель относительно Силы — тем уже. Отпустил вне зоны (или перетянул) — ошибка. Ходить можно. Дрожь —
 * полоска растёт неровно (плавно то быстрее, то медленнее).
 */
public class DumbbellGame extends Minigame {
    private static final float ZONE_START = 0.7f, SPEED = 0.75f, DOWN_TIME = 0.6f;
    private float fill, down;
    private boolean holding;

    public DumbbellGame(SportsNet.Open p) {
        super(p);
    }

    private float zoneWidth() {
        float r = Math.max(0.3f, params.ratio());
        float w = 0.2f / (float) Math.pow(r, 1.2) * params.ease() * strengthEase();
        return Math.max(0.06f, Math.min(0.28f, w));
    }

    @Override
    protected void update(float dt) {
        if (down > 0) {
            down -= dt;
            fill = Math.max(0, fill - dt / DOWN_TIME);
            return;
        }
        if (holding) {
            float uneven = tremble ? 0.45f * wobble(4.5f) : 0;
            fill += dt * SPEED * (1 + uneven);
            if (fill > 1) {
                holding = false;
                rep(false, 0);
                down = DOWN_TIME;
            }
        }
    }

    @Override
    public boolean owns(int key) {
        return key == GLFW.GLFW_KEY_E;
    }

    @Override
    public void key(int key, int action) {
        if (key != GLFW.GLFW_KEY_E || down > 0) return;
        if (action == GLFW.GLFW_PRESS) holding = true;
        else if (action == GLFW.GLFW_RELEASE && holding) {
            holding = false;
            boolean ok = fill >= ZONE_START && fill <= ZONE_START + zoneWidth();
            rep(ok, 0);
            down = DOWN_TIME;
        }
    }

    @Override
    public float poseA() {
        return fill;
    }

    /** Рука для анимации у других: 0 — правая, 1 — левая. */
    @Override
    public float poseB() {
        return params.hand();
    }

    @Override
    protected int draw(GuiGraphics g, Font font, int x, int y) {
        bar(g, x, y, W, 8, fill, C_FILL);
        zone(g, x, y, W, 8, ZONE_START, ZONE_START + zoneWidth());
        flashFrame(g, x, y, W, 8);
        label(g, font, Component.translatable("rpsports.ui.dumbbell", reps, (int) params.weightKg()), x, y + 11);
        return 20;
    }
}
