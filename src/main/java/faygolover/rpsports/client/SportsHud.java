package faygolover.rpsports.client;

import faygolover.rpcore.client.hud.Anchor;
import faygolover.rpcore.client.hud.HudElement;
import faygolover.rpsports.RpSports;
import faygolover.rpsports.client.games.Minigame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Виджет мини-игры — элемент общего HUD RP Core: маленький, внизу по центру, над полоской выносливости. */
public final class SportsHud implements HudElement {
    private static final ResourceLocation ID = new ResourceLocation(RpSports.MODID, "minigame");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Component title() {
        return Component.translatable("hud.rpsports.minigame");
    }

    @Override
    public int width() {
        return Minigame.W;
    }

    @Override
    public int height() {
        return 76;
    }

    @Override
    public Anchor defaultAnchor() {
        return Anchor.BOTTOM_CENTER;
    }

    @Override
    public int defaultX() {
        return 0;
    }

    @Override
    public int defaultY() {
        return -110;
    }

    @Override
    public void render(GuiGraphics g, float partialTick, boolean preview) {
        Minecraft mc = Minecraft.getInstance();
        if (preview) {
            g.fill(0, 0, Minigame.W, 8, 0xA0000000);
            g.fill(1, 1, Minigame.W * 2 / 3, 7, 0xFFE3B341);
            g.drawString(mc.font, Component.translatable("rpsports.ui.reps", 7), 0, 11, 0xFFE6E6E6, true);
            return;
        }
        Minigame game = ClientSession.game();
        if (game == null || mc.options.hideGui) return;
        game.render(g, mc.font, 0, 0);
    }
}
