package faygolover.rpsports.client.games;

import faygolover.rpsports.item.PlateItem;
import faygolover.rpsports.network.SportsNet;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Штанга: долбить E, шкала подъёма растёт (от Силы и веса) и медленно пустеет; вторая полоска — время на это
 * поднятие. Не успел — ошибка. С каждым поднятием труднее. После поднятия штанга плавно опускается.
 */
public class BenchGame extends Minigame {
    private static final float REP_TIME = 4.0f, LOWER_TIME = 1.0f;
    private float fill, left = REP_TIME, lower;
    private int lifted;

    public BenchGame(SportsNet.Open p) {
        super(p);
    }

    private float perPress() {
        float r = Math.max(0.3f, params.ratio());
        return 0.09f * strengthEase() / (float) Math.pow(r, 0.8) * (tremble ? 0.7f : 1f);
    }

    private float drain() {
        return 0.22f * (1 + 0.05f * lifted);
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
            return;
        }
        fill = Math.max(0, fill - drain() * dt);
        left -= dt;
        if (left <= 0) {
            rep(false, 0);
            lifted++;
            lower = LOWER_TIME;
        }
    }

    @Override
    public boolean key(int key, int action) {
        if (key != GLFW.GLFW_KEY_E) return owns(key);
        if (action == GLFW.GLFW_PRESS && lower <= 0) {
            fill += perPress();
            if (fill >= 1) {
                fill = 1;
                rep(true, 0);
                lifted++;
                lower = LOWER_TIME;
            }
        }
        return true;
    }

    @Override
    public float poseA() {
        return fill;
    }

    @Override
    public float poseB() {
        return lower > 0 ? 1 : 0;
    }

    @Override
    public void render(GuiGraphics g, Font font, int x, int y) {
        int j = jitter();
        bar(g, x + j, y, W, 8, fill, C_FILL);
        bar(g, x, y + 9, W, 3, lower > 0 ? 0 : left / REP_TIME, C_TIME);
        flashFrame(g, x, y, W, 12);
        label(g, font, Component.translatable("rpsports.ui.bench", reps, PlateItem.format(params.weightKg())), x, y + 15);
    }
}
