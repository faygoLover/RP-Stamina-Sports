package faygolover.rpsports.entity;

import faygolover.rpsports.registry.SportsRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/**
 * Невидимое сиденье на скамье: игрок «сидит», пока жмёт и отдыхает между подходами, — RP Stamina восстанавливает
 * выносливость как сидя. Без седока исчезает.
 */
public class SeatEntity extends Entity {
    public SeatEntity(EntityType<? extends SeatEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setInvisible(true);
    }

    public static SeatEntity create(Level level, double x, double y, double z) {
        SeatEntity e = new SeatEntity(SportsRegistry.SEAT.get(), level);
        e.setPos(x, y, z);
        return e;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && getPassengers().isEmpty()) discard();
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
