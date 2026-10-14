package faygolover.rpsports.block;

import faygolover.rpsports.item.PlateItem;
import faygolover.rpsports.menu.BenchMenu;
import faygolover.rpsports.registry.SportsRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Блины на грифе скамьи: два ряда по {@link #PER_SIDE} ячеек — левая и правая сторона, от середины к краю, по одному
 * блину в ячейке. Вес = гриф + все блины. Стороны должны быть одинаковыми (иначе штанга перекошена — жать нельзя).
 * Видно всем рядом (блины рисуются на грифе).
 */
public class BenchBlockEntity extends BlockEntity implements MenuProvider {
    public static final int PER_SIDE = 6, SLOTS = PER_SIDE * 2;

    private final SimpleContainer plates = new SimpleContainer(SLOTS) {
        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.getItem() instanceof PlateItem;
        }

        @Override
        public boolean stillValid(Player player) {
            return !isRemoved() && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            changed();
        }
    };

    public BenchBlockEntity(BlockPos pos, BlockState state) {
        super(SportsRegistry.BENCH_BE.get(), pos, state);
    }

    public SimpleContainer container() {
        return plates;
    }

    /** Блин в ячейке: сторона 0 — левая, 1 — правая; {@code i} — от середины; 0 — пусто. */
    public double plateKg(int side, int i) {
        ItemStack st = plates.getItem(side * PER_SIDE + i);
        return st.getItem() instanceof PlateItem p ? p.kg() : 0;
    }

    /** Гриф + все блины. */
    public double totalKg() {
        double sum = BenchBlock.BAR_KG;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack st = plates.getItem(i);
            if (st.getItem() instanceof PlateItem p) sum += p.kg();
        }
        return sum;
    }

    /** Стороны одинаковые: те же блины в том же порядке. */
    public boolean balanced() {
        for (int i = 0; i < PER_SIDE; i++) if (Math.abs(plateKg(0, i) - plateKg(1, i)) > 1e-6) return false;
        return true;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rpsports.bench");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new BenchMenu(id, inv, plates, worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        NonNullList<ItemStack> list = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < SLOTS; i++) list.set(i, plates.getItem(i));
        ContainerHelper.saveAllItems(tag, list, true);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        NonNullList<ItemStack> list = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, list);
        for (int i = 0; i < SLOTS; i++) plates.setItem(i, list.get(i));
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag t = new CompoundTag();
        saveAdditional(t);
        return t;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Гриф с блинами торчит за блок — рисовать, даже когда сам блок за краем экрана. */
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(2);
    }
}
