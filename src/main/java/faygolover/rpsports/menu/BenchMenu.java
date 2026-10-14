package faygolover.rpsports.menu;

import faygolover.rpsports.block.BenchBlockEntity;
import faygolover.rpsports.item.PlateItem;
import faygolover.rpsports.registry.SportsRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Окно блинов скамьи: верхний ряд — левая сторона грифа, нижний — правая (от середины к краю), по одному блину в
 * ячейке. Ниже — инвентарь игрока. Shift+клик блином — в первую свободную ячейку.
 */
public class BenchMenu extends AbstractContainerMenu {
    /** Левый верхний угол первой ячейки блинов (как у сундука на 2 ряда). */
    public static final int PLATES_X = 8, PLATES_Y = 18, INV_Y = 67;
    private final Container plates;
    private final BlockPos pos;

    /** Клиент: пустое хранилище, содержимое придёт с сервера. */
    public BenchMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, new SimpleContainer(BenchBlockEntity.SLOTS), buf.readBlockPos());
    }

    public BenchMenu(int id, Inventory inv, Container plates, BlockPos pos) {
        super(SportsRegistry.BENCH_MENU.get(), id);
        this.plates = plates;
        this.pos = pos;
        for (int side = 0; side < 2; side++) {
            for (int i = 0; i < BenchBlockEntity.PER_SIDE; i++) {
                addSlot(new Slot(plates, side * BenchBlockEntity.PER_SIDE + i, PLATES_X + i * 18, PLATES_Y + side * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.getItem() instanceof PlateItem;
                    }

                    @Override
                    public int getMaxStackSize() {
                        return 1;
                    }
                });
            }
        }
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, INV_Y + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, 8 + col * 18, INV_Y + 58));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack st = slot.getItem();
        ItemStack copy = st.copy();
        int n = BenchBlockEntity.SLOTS;
        if (index < n) {
            if (!moveItemStackTo(st, n, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!(st.getItem() instanceof PlateItem)) return ItemStack.EMPTY;
            // По одному блину — в первую свободную ячейку.
            boolean moved = false;
            for (int i = 0; i < n && !st.isEmpty(); i++) {
                Slot s = slots.get(i);
                if (!s.hasItem()) {
                    s.set(st.split(1));
                    moved = true;
                    break;
                }
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (st.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    /** Вес на грифе по содержимому окна (у клиента — то, что прислал сервер). */
    public double totalKg() {
        double sum = faygolover.rpsports.block.BenchBlock.BAR_KG;
        for (int i = 0; i < BenchBlockEntity.SLOTS; i++)
            if (plates.getItem(i).getItem() instanceof PlateItem p) sum += p.kg();
        return sum;
    }

    @Override
    public boolean stillValid(Player player) {
        return plates.stillValid(player) && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
}
