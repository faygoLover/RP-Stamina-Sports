package faygolover.rpsports.network;

import faygolover.rpsports.RpSports;
import faygolover.rpsports.core.Exercise;
import faygolover.rpsports.server.SportsService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Сеть аддона. Мини-игру ведёт клиент (ей верим, как в RP Medicine), сервер проверяет только частоту повторов и
 * сам считает опыт, усталость и выносливость. Поза для анимации идёт через сервер всем, кто рядом.
 */
public final class SportsNet {
    private SportsNet() {}

    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(RpSports.MODID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, Start.class, Start::encode, Start::decode, Start::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, Open.class, Open::encode, Open::decode, Open::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, Rep.class, Rep::encode, Rep::decode, Rep::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, Progress.class, Progress::encode, Progress::decode, Progress::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, Status.class, Status::encode, Status::decode, Status::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, Anim.class, Anim::encode, Anim::decode, Anim::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, StopRequest.class, StopRequest::encode, StopRequest::decode, StopRequest::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, Stopped.class, Stopped::encode, Stopped::decode, Stopped::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void send(ServerPlayer p, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), msg);
    }

    public static void toTrackers(Entity e, Object msg) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> e), msg);
    }

    public static void toServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }

    private static void client(Supplier<NetworkEvent.Context> ctx, Runnable r) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> r));
        ctx.get().setPacketHandled(true);
    }

    private static void server(Supplier<NetworkEvent.Context> ctx, java.util.function.Consumer<ServerPlayer> r) {
        ServerPlayer sp = ctx.get().getSender();
        ctx.get().enqueueWork(() -> {
            if (sp != null) r.accept(sp);
        });
        ctx.get().setPacketHandled(true);
    }

    /** Клиент → сервер: упражнение без снаряда (клавиша «Упражнения»). */
    public record Start(int exercise) {
        static void encode(Start p, FriendlyByteBuf b) { b.writeVarInt(p.exercise); }
        static Start decode(FriendlyByteBuf b) { return new Start(b.readVarInt()); }
        static void handle(Start p, Supplier<NetworkEvent.Context> c) {
            server(c, sp -> {
                if (p.exercise >= 0 && p.exercise < Exercise.VALUES.length) SportsService.startBodyweight(sp, Exercise.VALUES[p.exercise]);
            });
        }
    }

    /**
     * Сервер → клиент: начать мини-игру. Числа сложности считает сервер.
     *
     * @param ratio      отношение веса снаряда к «своему» (1 — свой вес; без снаряда — по весу тела)
     * @param strength   уровень Силы
     * @param fitness    уровень Телосложения
     * @param ease       множитель ширины зон (Ловкий, Плохая координация, броня)
     * @param weightKg   вес снаряда (для подписи)
     * @param hand       гантель: 0 — правая (основная), 1 — левая
     * @param program    дорожка: 0 ходьба, 1 интервалы, 2 бег
     */
    public record Open(int exercise, float ratio, int strength, int fitness, float ease, float weightKg, int hand, int program) {
        static void encode(Open p, FriendlyByteBuf b) {
            b.writeVarInt(p.exercise); b.writeFloat(p.ratio); b.writeVarInt(p.strength); b.writeVarInt(p.fitness);
            b.writeFloat(p.ease); b.writeFloat(p.weightKg); b.writeVarInt(p.hand); b.writeVarInt(p.program);
        }
        static Open decode(FriendlyByteBuf b) {
            return new Open(b.readVarInt(), b.readFloat(), b.readVarInt(), b.readVarInt(), b.readFloat(), b.readFloat(), b.readVarInt(), b.readVarInt());
        }
        static void handle(Open p, Supplier<NetworkEvent.Context> c) {
            client(c, () -> faygolover.rpsports.client.ClientSession.open(p));
        }
    }

    /** Клиент → сервер: повтор окончен. {@code extra}: груша — рука (0 левая, 1 правая), дорожка — темп ×100. */
    public record Rep(boolean success, int extra) {
        static void encode(Rep p, FriendlyByteBuf b) { b.writeBoolean(p.success); b.writeVarInt(p.extra); }
        static Rep decode(FriendlyByteBuf b) { return new Rep(b.readBoolean(), b.readVarInt()); }
        static void handle(Rep p, Supplier<NetworkEvent.Context> c) { server(c, sp -> SportsService.onRep(sp, p.success, p.extra)); }
    }

    /** Клиент → сервер: поза для анимации (a, b — смысл у каждого упражнения свой), ~10 раз в секунду. */
    public record Progress(float a, float b) {
        static void encode(Progress p, FriendlyByteBuf b) { b.writeFloat(p.a); b.writeFloat(p.b); }
        static Progress decode(FriendlyByteBuf b) { return new Progress(b.readFloat(), b.readFloat()); }
        static void handle(Progress p, Supplier<NetworkEvent.Context> c) { server(c, sp -> SportsService.onProgress(sp, p.a, p.b)); }
    }

    /** Сервер → клиент: дрожь (мало выносливости или устала зона), встряска камеры, число повторов. */
    public record Status(boolean tremble, boolean shake, int reps) {
        static void encode(Status p, FriendlyByteBuf b) { b.writeBoolean(p.tremble); b.writeBoolean(p.shake); b.writeVarInt(p.reps); }
        static Status decode(FriendlyByteBuf b) { return new Status(b.readBoolean(), b.readBoolean(), b.readVarInt()); }
        static void handle(Status p, Supplier<NetworkEvent.Context> c) { client(c, () -> faygolover.rpsports.client.ClientSession.status(p)); }
    }

    /** Сервер → клиенты рядом: поза игрока; {@code exercise} −1 — упражнение кончилось. */
    public record Anim(int entityId, int exercise, float a, float b) {
        static void encode(Anim p, FriendlyByteBuf b) { b.writeVarInt(p.entityId); b.writeVarInt(p.exercise); b.writeFloat(p.a); b.writeFloat(p.b); }
        static Anim decode(FriendlyByteBuf b) { return new Anim(b.readVarInt(), b.readVarInt(), b.readFloat(), b.readFloat()); }
        static void handle(Anim p, Supplier<NetworkEvent.Context> c) { client(c, () -> faygolover.rpsports.client.ExerciseAnimations.remote(p)); }
    }

    /** Клиент → сервер: закончить упражнение (Ctrl). */
    public record StopRequest() {
        static void encode(StopRequest p, FriendlyByteBuf b) {}
        static StopRequest decode(FriendlyByteBuf b) { return new StopRequest(); }
        static void handle(StopRequest p, Supplier<NetworkEvent.Context> c) { server(c, sp -> SportsService.stop(sp, null)); }
    }

    /** Сервер → клиент: упражнение окончено (причина — ключ перевода или пусто). */
    public record Stopped(String reason) {
        static void encode(Stopped p, FriendlyByteBuf b) { b.writeUtf(p.reason, 256); }
        static Stopped decode(FriendlyByteBuf b) { return new Stopped(b.readUtf(256)); }
        static void handle(Stopped p, Supplier<NetworkEvent.Context> c) { client(c, () -> faygolover.rpsports.client.ClientSession.stopped(p.reason)); }
    }
}
