package faygolover.rpsports;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import faygolover.rpcore.api.RpCoreAPI;
import faygolover.rpcore.api.event.RpTimeSkipEvent;
import faygolover.rpsports.capability.SportsData;
import faygolover.rpsports.core.Zone;
import faygolover.rpsports.network.SportsNet;
import faygolover.rpsports.registry.SportsRegistry;
import faygolover.rpsports.server.Muscles;
import faygolover.rpsports.server.SportsService;
import faygolover.rpstamina.api.StaminaConsumeEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.Locale;

/**
 * RP Stamina — Sports addon: тренажёры и упражнения без снаряда с мини-играми, усталость мышц по зонам, рост Силы и
 * Телосложения (дизайн — docs/00–09). Требует RP Core и RP Stamina; RP Perks, RP Culinary, RP Medicine — по желанию.
 */
@Mod(RpSports.MODID)
public class RpSports {
    public static final String MODID = "rpsports";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RpSports() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SportsConfig.SPEC);
        SportsRegistry.register(modBus);
        modBus.addListener(this::commonSetup);
        SportsData.register();

        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener(this::onPlayerTick);
        bus.addListener(net.minecraftforge.eventbus.api.EventPriority.NORMAL, false, faygolover.rpcore.api.event.RpCharacterWipeEvent.class, this::onWipe);
        bus.addListener(this::onDimension);
        bus.addListener(this::onStaminaSpent);
        bus.addListener(this::onTimeSkip);
        bus.addListener(this::registerCommands);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> faygolover.rpsports.client.ClientSetup.init(modBus));
    }

    private void commonSetup(FMLCommonSetupEvent e) {
        SportsNet.register();
        e.enqueueWork(() -> {
            RpCoreAPI.registerModule(MODID, "спорт: тренажёры, мини-игры, усталость мышц, рост Силы и Телосложения");
            RpCoreAPI.registerCommand(MODID, "/rpsports status [игрок]", "rpsports.help.status", 2, false);
            RpCoreAPI.registerCommand(MODID, "/rpsports rest [игрок]", "rpsports.help.rest", 2, false);
            RpCoreAPI.registerCommand(MODID, "/rpsports records [упражнение]", "rpsports.help.records", 0, false);
        });
    }

    private void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer sp)) return;
        SportsService.tick(sp);
        if (sp.tickCount % 20 == 0) SportsService.secondTick(sp);
    }

    /** Вайп персонажа: мышцы и рекорды как у нового. */
    private void onWipe(faygolover.rpcore.api.event.RpCharacterWipeEvent e) {
        SportsData d = SportsData.get(e.getEntity());
        if (d != null) d.copyFrom(new SportsData());
    }

    private void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) SportsService.forget(sp);
    }

    /** Обычная нагрузка RP Stamina понемногу утомляет мышцы (бег — ноги и т. д.). */
    private void onStaminaSpent(StaminaConsumeEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp && !e.isCanceled()) Muscles.ordinary(sp, e.getSource(), e.getAmount());
    }

    /** Прокрутка времени (/rptime sleep|skip): мышцы восстанавливаются за прошедшие секунды, во сне — быстрее. */
    private void onTimeSkip(RpTimeSkipEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        SportsData d = SportsData.get(sp);
        // Восстановление считается «как при 3-часовых сутках»; секунды события — по длине суток без /time speed.
        if (d != null) Muscles.recover(sp, d, e.seconds() * 10800.0 / RpCoreAPI.dayLengthSeconds(), e.sleeping(), sp.level().getGameTime());
    }

    private void registerCommands(RegisterCommandsEvent e) {
        // Рекорды — всем: /rpsports records [упражнение].
        var records = Commands.literal("records").executes(c -> records(c, null));
        for (faygolover.rpsports.core.Exercise ex : faygolover.rpsports.core.Exercise.VALUES)
            records.then(Commands.literal(ex.id).executes(c -> records(c, ex)));
        e.getDispatcher().register(Commands.literal("rpsports").then(records));
        e.getDispatcher().register(Commands.literal("rpsports").requires(s -> s.hasPermission(2))
                .then(Commands.literal("status")
                        .executes(c -> status(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).executes(c -> status(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("rest")
                        .executes(c -> rest(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).executes(c -> rest(c, EntityArgument.getPlayer(c, "player"))))));
    }

    private static int status(CommandContext<CommandSourceStack> c, ServerPlayer p) throws CommandSyntaxException {
        SportsData d = SportsData.get(p);
        if (d == null) return 0;
        c.getSource().sendSuccess(() -> Component.translatable("rpsports.cmd.status", p.getDisplayName()).withStyle(ChatFormatting.GOLD), false);
        for (Zone z : Zone.VALUES) {
            int i = z.ordinal();
            String flags = (d.pain[i] ? " боль" : d.fatigued[i] ? " усталость" : "") + (d.overload[i] > 0 ? String.format(Locale.ROOT, " перегрузка %.0f%%", d.overload[i]) : "");
            c.getSource().sendSuccess(() -> Component.translatable(z.key()).append(String.format(Locale.ROOT, ": %.0f%%", d.strength[i]) + flags)
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int records(CommandContext<CommandSourceStack> c, @org.jetbrains.annotations.Nullable faygolover.rpsports.core.Exercise ex) {
        ServerPlayer viewer = c.getSource().getPlayer();
        for (faygolover.rpsports.core.Exercise e : faygolover.rpsports.core.Exercise.VALUES) {
            if (ex != null && e != ex) continue;
            for (Component line : faygolover.rpsports.server.Records.lines(c.getSource().getServer(), viewer, e, ex == null ? 3 : 10))
                c.getSource().sendSuccess(() -> line, false);
        }
        return 1;
    }

    private static int rest(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        SportsService.rest(p);
        c.getSource().sendSuccess(() -> Component.translatable("rpsports.cmd.rested", p.getDisplayName()), true);
        return 1;
    }
}
