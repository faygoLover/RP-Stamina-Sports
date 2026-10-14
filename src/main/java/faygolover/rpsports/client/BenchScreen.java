package faygolover.rpsports.client;

import faygolover.rpsports.item.PlateItem;
import faygolover.rpsports.menu.BenchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Окно блинов скамьи на текстуре сундука: два ряда по 6 ячеек (лево / право), справа — подписи, в заголовке — вес. */
public class BenchScreen extends AbstractContainerScreen<BenchMenu> {
    private static final ResourceLocation CHEST = new ResourceLocation("textures/gui/container/generic_54.png");

    public BenchScreen(BenchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageHeight = 114 + 2 * 18;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        super.render(g, mx, my, partial);
        renderTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x = leftPos, y = topPos;
        g.blit(CHEST, x, y, 0, 0, imageWidth, 2 * 18 + 17);
        g.blit(CHEST, x, y + 2 * 18 + 17, 0, 126, imageWidth, 96);
        // Ячеек по 6 — лишние три в ряду закрыть фоном, там подписи сторон.
        g.fill(x + 7 + 6 * 18, y + 17, x + 7 + 9 * 18, y + 17 + 2 * 18, 0xFFC6C6C6);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, Component.translatable("rpsports.ui.bench_title", PlateItem.format(menu.totalKg())), titleLabelX, titleLabelY, 0x404040, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        g.drawString(font, Component.translatable("rpsports.ui.bench_left"), 7 + 6 * 18 + 4, 17 + 5, 0x404040, false);
        g.drawString(font, Component.translatable("rpsports.ui.bench_right"), 7 + 6 * 18 + 4, 17 + 18 + 5, 0x404040, false);
    }
}
