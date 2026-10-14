package faygolover.rpsports.block;

import faygolover.rpsports.registry.SportsRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Блины на грифе скамьи: список пар (вес одного блина), видно всем рядом. */
public class BenchBlockEntity extends BlockEntity {
    private final List<Double> plates = new ArrayList<>();

    public BenchBlockEntity(BlockPos pos, BlockState state) {
        super(SportsRegistry.BENCH_BE.get(), pos, state);
    }

    public List<Double> plates() {
        return List.copyOf(plates);
    }

    public int pairs() {
        return plates.size();
    }

    /** Гриф + все блины. */
    public double totalKg() {
        double sum = BenchBlock.BAR_KG;
        for (double p : plates) sum += p * 2;
        return sum;
    }

    public void addPair(double kg) {
        plates.add(kg);
        changed();
    }

    @Nullable
    public Double removePair() {
        if (plates.isEmpty()) return null;
        Double kg = plates.remove(plates.size() - 1);
        changed();
        return kg;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (double p : plates) {
            CompoundTag c = new CompoundTag();
            c.putDouble("kg", p);
            list.add(c);
        }
        tag.put("plates", list);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        plates.clear();
        ListTag list = tag.getList("plates", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) plates.add(list.getCompound(i).getDouble("kg"));
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
}
