package faygolover.rpsports.client.games;

import faygolover.rpsports.network.SportsNet;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Приседания: держишь E — опускаешься, отпускаешь — встаёшь; дошёл до нижней черты и встал — повтор. Всё время
 * плывёт равновесие — держать его в центре клавишами A и D. Вылетел — «потерял равновесие», ошибка.
 */
public class SquatGame extends Minigame {
    private float depth, balance, push, driftVel;
    private boolean holding, reachedBottom, left, right;

    public SquatGame(SportsNet.Open p) {
        super(p);
    }

    private float zoneHalf() {
        return Math.min(0.6f, 0.35f * params.ease());
    }

    @Override
    protected void update(float dt) {
        if (holding) depth = Math.min(1, depth + 1.1f * dt);
        else depth = Math.max(0, depth - 1.3f * strengthEase() * dt);
        if (depth >= 0.95f) reachedBottom = true;
        if (reachedBottom && depth <= 0.05f) {
            reachedBottom = false;
            rep(true, 0);
        }
        // Равновесие: случайный дрейф (тяжелее и слабее Телосложение — быстрее), A/D выравнивают.
        float driftScale = 0.35f * Math.max(0.6f, params.ratio()) / fitnessEase() * (tremble ? 1.4f : 1f);
        driftVel += (random.nextFloat() - 0.5f) * dt * 4 * driftScale;
        driftVel *= (float) Math.pow(0.6, dt);
        push = (right ? 1 : 0) - (left ? 1 : 0);
        balance += (driftVel + push * 1.2f * dt) * (0.3f + depth) ;
        if (Math.abs(balance) > zoneHalf()) {
            rep(false, 0);
            balance = 0;
            driftVel = 0;
            reachedBottom = false;
        }
    }

    @Override
    public boolean owns(int key) {
        return key == GLFW.GLFW_KEY_E || key == GLFW.GLFW_KEY_A || key == GLFW.GLFW_KEY_D;
    }

    @Override
    public boolean key(int key, int action) {
        boolean down = action != GLFW.GLFW_RELEASE;
        switch (key) {
            case GLFW.GLFW_KEY_E -> holding = down;
            case GLFW.GLFW_KEY_A -> left = down;
            case GLFW.GLFW_KEY_D -> right = down;
            default -> { return false; }
        }
        return true;
    }

    @Override
    public float poseA() {
        return depth;
    }

    @Override
    public float poseB() {
        return balance;
    }

    @Override
    public void render(GuiGraphics g, Font font, int x, int y) {
        g.fill(x, y, x + W, y + 6, C_BG);
        float h = zoneHalf();
        zone(g, x, y, W, 6, 0.5f - h / 2, 0.5f + h / 2);
        int mx = x + 1 + (int) (clamp(0.5f + balance / 2) * (W - 3)) + jitter();
        g.fill(mx, y - 1, mx + 2, y + 7, 0xFFFFFFFF);
        g.drawString(font, "A", x - 8, y - 1, left ? C_FILL : C_DIM, true);
        g.drawString(font, "D", x + W + 3, y - 1, right ? C_FILL : C_DIM, true);
        // Глубина — вертикальная полоска справа.
        int bx = x + W + 12;
        g.fill(bx, y - 10, bx + 4, y + 16, C_BG);
        int f = (int) (depth * 24);
        g.fill(bx + 1, y - 9, bx + 3, y - 9 + f, reachedBottom ? C_ZONE : C_FILL);
        flashFrame(g, x, y, W, 6);
        label(g, font, Component.translatable("rpsports.ui.reps", reps), x, y + 10);
    }
}
