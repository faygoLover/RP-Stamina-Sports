package faygolover.rpsports.server;

import faygolover.rpsports.capability.SportsData;
import faygolover.rpsports.core.Exercise;
import faygolover.rpsports.item.PlateItem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Рекорды (docs/00 п. 10): личный — у игрока ({@link SportsData}), общий топ-10 сервера по каждому упражнению —
 * здесь ({@code <мир>/data/rpsports_records.dat}). Рекорд — лучшая серия удачных повторов за подход.
 */
public final class Records extends SavedData {
    private static final String NAME = "rpsports_records";
    public static final int TOP = 10;

    public record Entry(UUID player, String name, double value) {}

    private final Map<Exercise, List<Entry>> top = new EnumMap<>(Exercise.class);

    public static Records get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Records::load, Records::new, NAME);
    }

    public List<Entry> top(Exercise e) {
        return top.getOrDefault(e, List.of());
    }

    /** Новый результат игрока: попал в десятку — встаёт на место (у игрока одна строка — лучшая). true — в десятке. */
    public boolean submit(ServerPlayer sp, Exercise e, double value) {
        if (value <= 0) return false;
        List<Entry> list = new ArrayList<>(top(e));
        for (Entry x : list) if (x.player.equals(sp.getUUID()) && x.value >= value) return false;
        list.removeIf(x -> x.player.equals(sp.getUUID()));
        list.add(new Entry(sp.getUUID(), sp.getGameProfile().getName(), value));
        list.sort((a, b) -> Double.compare(b.value, a.value));
        while (list.size() > TOP) list.remove(list.size() - 1);
        boolean in = list.stream().anyMatch(x -> x.player.equals(sp.getUUID()));
        top.put(e, list);
        setDirty();
        return in;
    }

    /** Таблица для чата: упражнение, личный рекорд, десятка (или первые {@code limit}). */
    public static List<Component> lines(MinecraftServer server, ServerPlayer viewer, Exercise e, int limit) {
        List<Component> out = new ArrayList<>();
        SportsData d = viewer == null ? null : SportsData.get(viewer);
        MutableComponent head = Component.translatable(e.key()).withStyle(ChatFormatting.GOLD);
        if (d != null && d.record(e) > 0)
            head.append(Component.translatable("rpsports.records.mine", PlateItem.format(d.record(e))).withStyle(ChatFormatting.GRAY));
        out.add(head);
        List<Entry> list = get(server).top(e);
        if (list.isEmpty()) out.add(Component.translatable("rpsports.records.empty").withStyle(ChatFormatting.DARK_GRAY));
        for (int i = 0; i < list.size() && i < limit; i++) {
            Entry x = list.get(i);
            boolean me = viewer != null && x.player.equals(viewer.getUUID());
            out.add(Component.literal("  " + (i + 1) + ". " + x.name + " — " + PlateItem.format(x.value))
                    .withStyle(me ? ChatFormatting.YELLOW : ChatFormatting.WHITE));
        }
        return out;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        for (var en : top.entrySet()) {
            ListTag l = new ListTag();
            for (Entry x : en.getValue()) {
                CompoundTag c = new CompoundTag();
                c.putUUID("id", x.player);
                c.putString("name", x.name);
                c.putDouble("v", x.value);
                l.add(c);
            }
            tag.put(en.getKey().id, l);
        }
        return tag;
    }

    private static Records load(CompoundTag tag) {
        Records r = new Records();
        for (Exercise e : Exercise.VALUES) {
            ListTag l = tag.getList(e.id, Tag.TAG_COMPOUND);
            List<Entry> list = new ArrayList<>();
            for (int i = 0; i < l.size(); i++) {
                CompoundTag c = l.getCompound(i);
                if (c.hasUUID("id")) list.add(new Entry(c.getUUID("id"), c.getString("name"), c.getDouble("v")));
            }
            if (!list.isEmpty()) r.top.put(e, list);
        }
        return r;
    }
}
