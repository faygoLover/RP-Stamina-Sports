package faygolover.rpsports.capability;

import faygolover.rpsports.RpSports;
import faygolover.rpsports.core.Zone;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Мышцы игрока (сервер): силы зон 0–100, стадии «Усталость» и «Боль в мышцах» (держатся до полного восстановления),
 * перегрузка «сверх нуля», дожигание. Игроку не синхронизируется: он не видит цифр — только ощущения.
 */
public final class SportsData {
    public static final Capability<SportsData> CAP = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation ID = new ResourceLocation(RpSports.MODID, "muscles");

    public final double[] strength = new double[Zone.VALUES.length];
    /** Скрытая усталость (ниже 30 %) — до полного восстановления. */
    public final boolean[] fatigued = new boolean[Zone.VALUES.length];
    /** «Боль в мышцах» (ниже 10 %) — до полного восстановления. */
    public final boolean[] pain = new boolean[Zone.VALUES.length];
    /** Нагрузка «сверх нуля», % (растяжение, надрыв). */
    public final double[] overload = new double[Zone.VALUES.length];
    /** Последняя ступень перегрузки, о которой сообщили моду тела (1 — боль, 2 — растяжение, 3 — надрыв). */
    public final int[] strainSent = new int[Zone.VALUES.length];
    /** Игровое время, когда зона работала в последний раз (пока работает — не восстанавливается). */
    public final long[] lastWorked = new long[Zone.VALUES.length];
    /** До какого игрового времени идёт дожигание. */
    public long afterburnUntil;
    /** Восстановлено мышц с прошлой оплаты калориями, %. */
    public double recoveredPercent;
    /** Рекорды подхода по упражнениям (id → значение). Не пропадают и после смерти. */
    private final java.util.Map<String, Double> records = new java.util.HashMap<>();

    public double record(faygolover.rpsports.core.Exercise e) {
        return records.getOrDefault(e.id, 0.0);
    }

    public void setRecord(faygolover.rpsports.core.Exercise e, double value) {
        records.put(e.id, value);
    }

    public SportsData() {
        java.util.Arrays.fill(strength, 100);
    }

    @Nullable
    public static SportsData get(Player p) {
        return p.getCapability(CAP).resolve().orElse(null);
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        for (Zone z : Zone.VALUES) {
            CompoundTag c = new CompoundTag();
            int i = z.ordinal();
            c.putDouble("s", strength[i]);
            c.putBoolean("f", fatigued[i]);
            c.putBoolean("p", pain[i]);
            c.putDouble("o", overload[i]);
            c.putInt("st", strainSent[i]);
            t.put(z.id, c);
        }
        t.putLong("afterburn", afterburnUntil);
        CompoundTag rec = new CompoundTag();
        records.forEach(rec::putDouble);
        t.put("records", rec);
        return t;
    }

    public void load(CompoundTag t) {
        for (Zone z : Zone.VALUES) {
            int i = z.ordinal();
            CompoundTag c = t.getCompound(z.id);
            strength[i] = c.contains("s") ? c.getDouble("s") : 100;
            fatigued[i] = c.getBoolean("f");
            pain[i] = c.getBoolean("p");
            overload[i] = c.getDouble("o");
            strainSent[i] = c.getInt("st");
        }
        afterburnUntil = t.getLong("afterburn");
        records.clear();
        CompoundTag rec = t.getCompound("records");
        for (String k : rec.getAllKeys()) records.put(k, rec.getDouble(k));
    }

    public void copyFrom(SportsData o) {
        System.arraycopy(o.strength, 0, strength, 0, strength.length);
        System.arraycopy(o.fatigued, 0, fatigued, 0, fatigued.length);
        System.arraycopy(o.pain, 0, pain, 0, pain.length);
        System.arraycopy(o.overload, 0, overload, 0, overload.length);
        System.arraycopy(o.strainSent, 0, strainSent, 0, strainSent.length);
        afterburnUntil = o.afterburnUntil;
        copyRecords(o);
    }

    public void copyRecords(SportsData o) {
        records.clear();
        records.putAll(o.records);
    }

    public static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final SportsData data = new SportsData();
        private final LazyOptional<SportsData> opt = LazyOptional.of(() -> data);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return CAP.orEmpty(cap, opt);
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.save();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            data.load(nbt);
        }
    }
}
