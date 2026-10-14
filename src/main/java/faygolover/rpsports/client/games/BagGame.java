package faygolover.rpsports.client.games;

import faygolover.rpsports.network.SportsNet;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Груша: она качается, бить надо, когда она возвращается — маркер в зоне посередине. Подсказка — какой рукой:
 * ЛКМ — левой, ПКМ — правой, иногда «двойка» (две быстрые подряд). Мимо, не той рукой или пропустил — ошибка, серия
 * сначала. С каждым ударом серии груша качается быстрее.
 */
public class BagGame extends Minigame {
    private static final int LEFT = 0, RIGHT = 1, DOUBLE = 2;
    private float phase, omega = 2.2f, punch;
    private int prompt, series, punchHand, doubleLeft, passes;
    private float doubleTimer;
    private boolean wasInZone;

    public BagGame(SportsNet.Open p) {
        super(p);
        newPrompt();
    }

    private float zoneHalf() {
        return Math.min(0.3f, 0.13f * fitnessEase() * params.ease() * (tremble ? 0.8f : 1f));
    }

    private float marker() {
        return (float) Math.sin(phase);
    }

    private void newPrompt() {
        int r = random.nextInt(10);
        prompt = r < 4 ? LEFT : r < 8 ? RIGHT : DOUBLE;
        doubleLeft = prompt == DOUBLE ? 2 : 0;
        passes = 0;
    }

    @Override
    protected void update(float dt) {
        phase += omega * dt;
        if (punch > 0) punch = Math.max(0, punch - dt * 4);
        if (doubleTimer > 0) {
            doubleTimer -= dt;
            if (doubleTimer <= 0 && doubleLeft == 1) fail();
        }
        boolean in = Math.abs(marker()) < zoneHalf();
        if (wasInZone && !in) {
            // Маркер прошёл зону без удара: за два прохода — пропустил.
            if (++passes >= 2) fail();
        }
        wasInZone = in;
    }

    private void fail() {
        rep(false, punchHand);
        series = 0;
        omega = 2.2f;
        newPrompt();
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
        punchHand = button == 0 ? LEFT : RIGHT;
        punch = 1;
        boolean in = Math.abs(marker()) < zoneHalf();
        if (prompt == DOUBLE) {
            if (doubleLeft == 2 && in) {
                doubleLeft = 1;
                doubleTimer = 0.35f;
                return true;
            }
            if (doubleLeft == 1 && doubleTimer > 0) {
                hit();
                return true;
            }
            fail();
            return true;
        }
        if (in && punchHand == prompt) hit();
        else fail();
        return true;
    }

    private void hit() {
        rep(true, punchHand);
        series++;
        omega = Math.min(5f, 2.2f + 0.08f * series);
        // Груша отлетает — удар Сильного отбрасывает дальше (дольше до возврата).
        phase += (float) (Math.PI * (0.6 + 0.04 * (params.strength() - 3)));
        newPrompt();
    }

    @Override
    public float poseA() {
        return punch;
    }

    @Override
    public float poseB() {
        return punchHand;
    }

    @Override
    public void render(GuiGraphics g, Font font, int x, int y) {
        String key = switch (prompt) {
            case LEFT -> "rpsports.ui.bag.left";
            case RIGHT -> "rpsports.ui.bag.right";
            default -> "rpsports.ui.bag.double";
        };
        g.drawCenteredString(font, Component.translatable(key), x + W / 2, y, C_FILL);
        int by = y + 11;
        g.fill(x, by, x + W, by + 6, C_BG);
        float half = zoneHalf();
        zone(g, x, by, W, 6, 0.5f - half / 2, 0.5f + half / 2);
        int mx = x + 1 + (int) ((marker() * 0.5f + 0.5f) * (W - 3)) + jitter();
        g.fill(mx, by - 1, mx + 2, by + 7, 0xFFFFFFFF);
        flashFrame(g, x, by, W, 6);
        label(g, font, Component.translatable("rpsports.ui.bag", series), x, by + 9);
    }
}
