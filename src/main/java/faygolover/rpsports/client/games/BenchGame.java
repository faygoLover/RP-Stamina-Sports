package faygolover.rpsports.client.games;

import faygolover.rpsports.SportsConfig;
import faygolover.rpsports.item.PlateItem;
import faygolover.rpsports.network.SportsNet;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Штанга: долбить E — кольцо подъёма вокруг клавиши растёт (от Силы и веса) и пустеет; тонкое внешнее кольцо — время
 * на это поднятие. Не успел — ошибка. С каждым поднятием труднее. После поднятия штанга плавно опускается.
 */
public class BenchGame extends Minigame {
    private static final float REP_TIME = 4.0f, LOWER_TIME = 1.0f;
    private float fill, shown, left = REP_TIME, lower;
    private int lifted;

    public BenchGame(SportsNet.Open p) {
        super(p);
    }

    /** Доля подъёма за нажатие (docs/10): Сила 3 — 50 кг ≈ 6 нажатий/с, пустой гриф ≈ 3; перегруз дорог выносливостью. */
    private float perPress() {
        double r = Math.max(0.3, params.ratio());
        double heavy = Math.pow(r, SportsConfig.get(r <= 1 ? SportsConfig.BENCH_EXP_LIGHT : SportsConfig.BENCH_EXP_HEAVY));
        double ease = Math.max(0.2, 1 + SportsConfig.get(SportsConfig.BENCH_STRENGTH_EASE) * (params.strength() - 3));
        return (float) (SportsConfig.get(SportsConfig.BENCH_PRESS) * ease / heavy) * (tremble ? 0.75f : 1f);
    }

    private float drain() {
        return (float) (SportsConfig.get(SportsConfig.BENCH_DRAIN) * (1 + SportsConfig.get(SportsConfig.BENCH_DRAIN_PER_REP) * lifted));
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
    protected void key(int key, int action) {
        if (key != GLFW.GLFW_KEY_E || action != GLFW.GLFW_PRESS || lower > 0) return;
        fill += perPress();
        if (fill >= 1) {
            fill = 1;
            rep(true, 0, params.weightKg());
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
        float cx = x + W / 2f, cy = y + 26;
        Qte.small(g, font, Component.translatable("rpsports.ui.mash_kg", PlateItem.format(params.weightKg())), cx, y, Qte.GOLD);
        Qte.arc(g, cx, cy, 15, 3, 0, 1, Qte.SHADE);
        Qte.arc(g, cx, cy, 15, 3, 0, shown, flashColor(Qte.GOLD));
        if (lower <= 0) Qte.arc(g, cx, cy, 19.5f, 1, 0, left / REP_TIME, Qte.WHITE_DIM);
        Qte.key(g, font, cx, cy, 10, "E", press(GLFW.GLFW_KEY_E), lower <= 0);
        return 48;
    }
}
