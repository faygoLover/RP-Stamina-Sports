package faygolover.rpsports.client.games;

import faygolover.rpsports.item.PlateItem;
import faygolover.rpsports.network.SportsNet;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Штанга: долбить E, шкала подъёма растёт (от Силы и веса) и пустеет; вторая полоска — время на это поднятие. Не
 * успел — ошибка. С каждым поднятием труднее. После поднятия штанга плавно опускается.
 */
public class BenchGame extends Minigame {
    private static final float REP_TIME = 4.0f, LOWER_TIME = 1.0f;
    private float fill, shown, left = REP_TIME, lower;
    private int lifted;

    public BenchGame(SportsNet.Open p) {
        super(p);
    }

    private float perPress() {
        float r = Math.max(0.3f, params.ratio());
        return 0.055f * strengthEase() / (float) Math.pow(r, 0.8) * (tremble ? 0.75f : 1f);
    }

    private float drain() {
        return 0.26f * (1 + 0.05f * lifted);
    }

    @Override
    protected void update(float dt) {
        if (lower > 0) {
            lower -= dt;
            fill = Math.max(0, fill - dt / LOWER_TIME);
            if (lower <= 0) {
                fill = 0;
                left = REP_TIME;
            }
        } else {
            fill = Math.max(0, fill - drain() * dt);
            left -= dt;
            if (left <= 0) {
                rep(false, 0);
                lifted++;
                lower = LOWER_TIME;
            }
        }
        shown = approach(shown, fill, dt, 18);
    }

    @Override
    public boolean owns(int key) {
        return key == GLFW.GLFW_KEY_E;
    }

    @Override
    public void key(int key, int action) {
        if (key != GLFW.GLFW_KEY_E || action != GLFW.GLFW_PRESS || lower > 0) return;
        fill += perPress();
        if (fill >= 1) {
            fill = 1;
            rep(true, 0);
            lifted++;
            lower = LOWER_TIME;
        }
    }

    @Override
    public float poseA() {
        return shown;
    }

    @Override
    public float poseB() {
        return lower > 0 ? 1 : 0;
    }

    @Override
    protected int draw(GuiGraphics g, Font font, int x, int y) {
        bar(g, x, y, W, 8, shown, C_FILL);
        bar(g, x, y + 9, W, 3, lower > 0 ? 0 : left / REP_TIME, C_TIME);
        flashFrame(g, x, y, W, 12);
        label(g, font, Component.translatable("rpsports.ui.bench", reps, PlateItem.format(params.weightKg())), x, y + 15);
        return 24;
    }
}
