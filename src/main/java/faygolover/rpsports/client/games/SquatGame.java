package faygolover.rpsports.client.games;

import faygolover.rpsports.network.SportsNet;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Приседания: держишь пробел — опускаешься, отпускаешь — встаёшь; дошёл до низа и встал — повтор. Отпустил, не дойдя
 * до низа, — ошибка. Всё время плывёт равновесие — держать маркер в светлой зоне клавишами A и D.
 *
 * <p>Дрейф плавный и читаемый (сумма медленных волн), глубже присел — сильнее; A/D всегда сильнее дрейфа. Вне зоны
 * дольше {@link #OUT_LIMIT} с или упёрся в край — потерял равновесие и упал: упражнение кончается.
 */
public class SquatGame extends Minigame {
    /** Повтор «упал» для сервера. */
    public static final int FELL = 9;
    private static final float PUSH = 1.1f, OUT_LIMIT = 1.2f;
    private final float phase1, phase2;
    private float depth, balance, vel, shown, outTime;
    private boolean holding, reachedBottom, left, right, fell;

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
        float wave = (float) (Math.sin(time * 0.75 + phase1) * 0.6 + Math.sin(time * 1.2 + phase2) * 0.4);
        float scale = 0.4f * Math.max(0.6f, params.ratio()) / fitnessEase() * (tremble ? 1.3f : 1f) * (0.6f + depth * 0.8f);
        return wave * Math.min(0.85f, scale);
    }

    @Override
    protected void update(float dt) {
        if (fell) return;
        if (holding) depth = Math.min(1, depth + 0.9f * dt);
        else depth = Math.max(0, depth - 1.1f * strengthEase() * dt);
        if (depth >= 0.95f) reachedBottom = true;

        float push = (right ? 1 : 0) - (left ? 1 : 0);
        vel = approach(vel, drift() + push * PUSH, dt, 7f);
        balance += vel * dt;
        if (Math.abs(balance) >= 1) {
            balance = Math.signum(balance);
            vel = 0;
        }
        shown = approach(shown, balance, dt, 30);

        boolean out = Math.abs(balance) > zoneHalf();
        outTime = out ? outTime + dt : 0;
        if (outTime > OUT_LIMIT || Math.abs(balance) >= 1) {
            fell = true;
            rep(false, FELL);
            return;
        }
        if (reachedBottom && depth <= 0.05f) {
            reachedBottom = false;
            rep(true, 0);
        }
    }

    @Override
    public boolean owns(int key) {
        return key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_A || key == GLFW.GLFW_KEY_D;
    }

    @Override
    protected void key(int key, int action) {
        boolean down = action != GLFW.GLFW_RELEASE;
        switch (key) {
            case GLFW.GLFW_KEY_SPACE -> {
                if (!down && holding && !reachedBottom && depth > 0.2f && !fell) rep(false, 0); // не дошёл до низа
                holding = down;
            }
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
        float cx = x + W / 2f;
        float h = zoneHalf();
        boolean out = Math.abs(balance) > h;
        Qte.key(g, font, x + 8, y + 6, 7, "A", press(GLFW.GLFW_KEY_A), true);
        Qte.key(g, font, x + W - 8, y + 6, 7, "D", press(GLFW.GLFW_KEY_D), true);
        Qte.track(g, x + 19, y + 5, W - 38, 0.5f - h / 2, 0.5f + h / 2, 0.5f + shown / 2, out ? Qte.BAD : flashColor(Qte.WHITE));
        Qte.wideKey(g, font, cx, y + 24, 54, 11, Component.translatable("rpsports.ui.space").getString(), depth,
                press(GLFW.GLFW_KEY_SPACE));
        if (reachedBottom) Qte.small(g, font, Component.literal("▲"), cx + 33, y + 21, Qte.GOLD_ZONE);
        return 32;
    }
}
