package faygolover.rpsports.gametest;

import com.mojang.authlib.GameProfile;
import faygolover.rpsports.RpSports;
import faygolover.rpsports.capability.SportsData;
import faygolover.rpsports.core.Exercise;
import faygolover.rpsports.core.Zone;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Края состояния мышц: запись и чтение, смерть, возврат из Края, перезаход. Страховка перед переводом данных
 * на общий механизм RP Core (план {@code _family/refactor}, задача T0.3): эти тесты обязаны проходить и до, и после.
 */
@GameTestHolder(RpSports.MODID)
@PrefixGameTestTemplate(false)
public final class SportsEdgeTests {
    private SportsEdgeTests() {}

    static final String T = "platform";
    private static int counter;

    /**
     * Поля, которые сейчас в NBT не пишутся (находки, записаны в docs/test_findings.md): после перезахода они
     * сбрасываются. Список держит тест честным: новое поле, забытое в save/load, уронит nbtRoundTrip.
     */
    private static final Set<String> NOT_SAVED = Set.of("lastWorked", "recoveredPercent");

    // ------------------------------------------------------------------ помощники

    static ServerPlayer player(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        ServerPlayer p = new ServerPlayer(server, h.getLevel(), new GameProfile(UUID.randomUUID(), "rps-edge-" + (counter++)));
        Connection conn = new Connection(PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(conn);
        server.getPlayerList().placeNewPlayer(conn, p);
        p.setGameMode(GameType.SURVIVAL);
        Vec3 pos = h.absoluteVec(new Vec3(2.5, 1, 2.5));
        p.moveTo(pos.x, pos.y, pos.z, 0, 0);
        p.setOnGround(true);
        return p;
    }

    static void remove(GameTestHelper h, ServerPlayer p) {
        if (!p.isRemoved()) h.getLevel().getServer().getPlayerList().remove(p);
    }

    /** Все сохраняемые поля — не значениями по умолчанию и разными по зонам. */
    static void fill(SportsData d) {
        for (Zone z : Zone.VALUES) {
            int i = z.ordinal();
            d.strength[i] = 11 + i * 3;
            d.fatigued[i] = true;
            d.pain[i] = i % 2 == 0;
            d.overload[i] = 5.5 + i;
            d.strainSent[i] = 1 + i % 3;
            d.lastWorked[i] = 1000 + i;
        }
        d.afterburnUntil = 123456L;
        d.recoveredPercent = 42.5;
        d.setRecord(Exercise.VALUES[0], 17.0);
        d.setRecord(Exercise.VALUES[Exercise.VALUES.length - 1], 3.5);
    }

    /** Различия двух объектов по всем нестатическим полям, кроме перечисленных. */
    static List<String> differences(Object a, Object b, Set<String> skip) {
        List<String> out = new ArrayList<>();
        try {
            for (Field f : a.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || skip.contains(f.getName())) continue;
                f.setAccessible(true);
                if (!same(f.get(a), f.get(b))) out.add(f.getName());
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        return out;
    }

    private static boolean same(Object x, Object y) {
        if (x == null || y == null) return x == y;
        if (x.getClass().isArray()) {
            int n = Array.getLength(x);
            if (!y.getClass().isArray() || Array.getLength(y) != n) return false;
            for (int i = 0; i < n; i++) if (!same(Array.get(x, i), Array.get(y, i))) return false;
            return true;
        }
        return Objects.equals(x, y);
    }

    // ------------------------------------------------------------------ тесты

    /** Записал → прочитал: все поля те же (кроме известных несохраняемых). */
    @GameTest(template = T, batch = "sports_edge_roundtrip")
    public static void nbtRoundTrip(GameTestHelper h) {
        SportsData a = new SportsData();
        fill(a);
        SportsData b = new SportsData();
        b.load(a.save());
        List<String> diff = differences(a, b, NOT_SAVED);
        if (!diff.isEmpty()) h.fail("После записи и чтения отличаются поля: " + diff);
        // Список несохраняемых не должен устареть: если поле начали сохранять, убрать его из NOT_SAVED.
        List<String> stillLost = differences(a, b, Set.of());
        for (String name : NOT_SAVED) if (!stillLost.contains(name)) h.fail("Поле " + name + " теперь сохраняется — убери его из NOT_SAVED");
        h.succeed();
    }

    /** Старое сохранение без данных мода: читается без ошибок, мышцы свежие. */
    @GameTest(template = T, batch = "sports_edge_empty")
    public static void nbtEmptyLoad(GameTestHelper h) {
        SportsData d = new SportsData();
        fill(d);
        d.load(new CompoundTag());
        for (Zone z : Zone.VALUES) {
            int i = z.ordinal();
            if (d.strength[i] != 100) h.fail("Сила зоны " + z.id + " после пустого чтения: " + d.strength[i]);
            if (d.fatigued[i] || d.pain[i] || d.overload[i] != 0 || d.strainSent[i] != 0) h.fail("Зона " + z.id + " не сброшена");
        }
        if (d.afterburnUntil != 0) h.fail("Дожигание не сброшено");
        if (d.record(Exercise.VALUES[0]) != 0) h.fail("Рекорды не сброшены");
        h.succeed();
    }

    /** Смерть: мышцы свежие, рекорды остаются. */
    @GameTest(template = T, batch = "sports_edge_death")
    public static void deathResetsMusclesKeepsRecords(GameTestHelper h) {
        ServerPlayer p = player(h);
        SportsData d = SportsData.get(p);
        if (d == null) { h.fail("Нет данных мышц у игрока"); return; }
        fill(d);
        ServerPlayer n = h.getLevel().getServer().getPlayerList().respawn(p, false);
        try {
            SportsData nd = SportsData.get(n);
            if (nd == null) { h.fail("Нет данных мышц после возрождения"); return; }
            for (Zone z : Zone.VALUES) {
                int i = z.ordinal();
                if (nd.strength[i] != 100 || nd.fatigued[i] || nd.pain[i] || nd.overload[i] != 0 || nd.strainSent[i] != 0)
                    h.fail("Зона " + z.id + " после смерти не свежая");
            }
            if (nd.afterburnUntil != 0) h.fail("Дожигание пережило смерть");
            if (nd.record(Exercise.VALUES[0]) != 17.0 || nd.record(Exercise.VALUES[Exercise.VALUES.length - 1]) != 3.5)
                h.fail("Рекорды после смерти потеряны");
        } finally {
            remove(h, n);
        }
        h.succeed();
    }

    /** Возврат из Края (не смерть): сохраняется всё, что пишется в NBT. */
    @GameTest(template = T, batch = "sports_edge_end")
    public static void endReturnKeepsAll(GameTestHelper h) {
        ServerPlayer p = player(h);
        SportsData d = SportsData.get(p);
        if (d == null) { h.fail("Нет данных мышц у игрока"); return; }
        fill(d);
        CompoundTag before = d.save();
        ServerPlayer n = h.getLevel().getServer().getPlayerList().respawn(p, true);
        try {
            SportsData nd = SportsData.get(n);
            if (nd == null || !before.equals(nd.save())) h.fail("После возврата из Края данные мышц изменились");
        } finally {
            remove(h, n);
        }
        h.succeed();
    }

    /** Перезаход: данные игрока записаны и прочитаны целиком, ветка мода в сохранении та же. */
    @GameTest(template = T, batch = "sports_edge_relog")
    public static void relogKeeps(GameTestHelper h) {
        ServerPlayer p = player(h);
        ServerPlayer q = player(h);
        try {
            SportsData d = SportsData.get(p);
            if (d == null) { h.fail("Нет данных мышц у игрока"); return; }
            fill(d);
            CompoundTag saved = p.saveWithoutId(new CompoundTag());
            String key = SportsData.ID.toString();
            if (!saved.getCompound("ForgeCaps").contains(key)) h.fail("В сохранении игрока нет ветки ForgeCaps." + key);
            q.load(saved);
            SportsData qd = SportsData.get(q);
            if (qd == null || !d.save().equals(qd.save())) h.fail("После перезахода данные мышц изменились");
        } finally {
            remove(h, p);
            remove(h, q);
        }
        h.succeed();
    }
}
