package faygolover.rpsports.client.games;

import faygolover.rpsports.network.SportsNet;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Приседания: держишь E — опускаешься, отпускаешь — встаёшь; дошёл до нижней черты и встал — повтор. Всё время
 * плывёт равновесие — держать маркер в зелёной зоне клавишами A и D.
 *
 * <p>Дрейф медленный и плавный (сумма медленных волн), меняет сторону постепенно, глубже присел — сильнее. A/D всегда
 * сильнее дрейфа. Маркер движется с инерцией. В серой зоне ничего не сбрасывается — выровнять надо самому; пробыл
 * там больше 1,2 с или упёрся в край — «потерял равновесие» (ошибка), повтор не засчитан.
 */
public class SquatGame extends Minigame {
    private static final float PUSH = 0.9f, OUT_LIMIT = 1.2f, GRACE = 1.5f;
    private final float phase1, phase2;
    private float depth, balance, vel, shown, outTime, grace;
    private boolean holding, reachedBottom, left, right;

    public SquatGame(SportsNet.Open p) {
        super(p);
        phase1 = random.nextFloat() * 6.28f;
        phase2 = random.nextFloat() * 6.28f;
    }

    private float zoneHalf() {
        return Math.max(0.3f, Math.min(0.6f, 0.45f * params.ease() * fitnessEase()));
    }

    /** Сила дрейфа: тяжелее и слабее Телосложение — сильнее; глубже присел — сильнее. */
    private float drift() {
        float wave = (float) (Math.sin(time * 0.7 + phase1) * 0.6 + Math.sin(time * 1.13 + phase2) * 0.4);
        float scale = 0.35f * Math.max(0.6f, params.ratio()) / fitnessEase() * (tremble ? 1.3f : 1f) * (0.6f + depth * 0.8f);
        return wave * Math.min(0.75f, scale);
    }

    @Override
    protected void update(float dt) {
        if (holding) depth = Math.min(1, depth + 0.9f * dt);
        else depth = Math.max(0, depth - 1.1f * strengthEase() * dt);
        if (depth >= 0.95f) reachedBottom = true;

        float push = (right ? 1 : 0) - (left ? 1 : 0);
        vel = approach(vel, drift() + push * PUSH, dt, 3f);
        balance += vel * dt;
        if (Math.abs(balance) >= 1) {
            balance = Math.signum(balance);
            vel = 0;
        }
        shown = approach(shown, balance, dt, 16);

        boolean out = Math.abs(balance) > zoneHalf();
        if (grace > 0) grace -= dt;
        outTime = out ? outTime + dt : 0;
        if (grace <= 0 && (outTime > OUT_LIMIT || Math.abs(balance) >= 1)) {
            rep(false, 0);
            reachedBottom = false;
            grace = GRACE;
            outTime = 0;
        }
        if (reachedBottom && depth <= 0.05f) {
            reachedBottom = false;
            if (!out) rep(true, 0);
        }
    }

    @Override
    public boolean owns(int key) {
        return key == GLFW.GLFW_KEY_E || key == GLFW.GLFW_KEY_A || key == GLFW.GLFW_KEY_D;
    }

    @Override
    public void key(int key, int action) {
        boolean down = action != GLFW.GLFW_RELEASE;
        switch (key) {
            case GLFW.GLFW_KEY_E -> holding = down;
            case GLFW.GLFW_KEY_A -> left = down;
            case GLFW.GLFW_KEY_D -> right = down;
            default -> { }
        }
    }

    @Override
    public float poseA() {
        return depth;
    }

    @Override
    public float poseB() {
        return shown;
    }

    @Override
    protected int draw(GuiGraphics g, Font font, int x, int y) {
        g.fill(x, y, x + W, y + 6, C_BG);
        float h = zoneHalf();
        zone(g, x, y, W, 6, 0.5f - h / 2, 0.5f + h / 2);
        marker(g, x, y, W, 6, 0.5f + shown / 2);
        g.drawString(font, "A", x - 8, y - 1, left ? C_FILL : C_DIM, true);
        g.drawString(font, "D", x + W + 3, y - 1, right ? C_FILL : C_DIM, true);
        // Глубина — вертикальная полоска справа.
        int bx = x + W + 12;
        g.fill(bx, y - 10, bx + 4, y + 16, C_BG);
        int f = Math.round(depth * 24);
        g.fill(bx + 1, y - 9, bx + 3, y - 9 + f, reachedBottom ? C_ZONE : C_FILL);
        flashFrame(g, x, y, W, 6);
        label(g, font, Component.translatable("rpsports.ui.reps", reps), x, y + 10);
        return 19;
    }
}
