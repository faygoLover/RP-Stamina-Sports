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
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
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
        modBus.addListener(this::registerCaps);

        var bus = MinecraftForge.EVENT_BUS;
        bus.addGenericListener(Entity.class, this::attachCaps);
        bus.addListener(this::onPlayerTick);
        bus.addListener(this::onClone);
        bus.addListener(this::onLogout);
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
        });
    }

    private void registerCaps(RegisterCapabilitiesEvent e) {
        e.register(SportsData.class);
    }

    private void attachCaps(AttachCapabilitiesEvent<Entity> e) {
        if (e.getObject() instanceof Player) e.addCapability(SportsData.ID, new SportsData.Provider());
    }

    private void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer sp) || sp.tickCount % 20 != 0) return;
        SportsService.secondTick(sp);
    }

    private void onClone(PlayerEvent.Clone e) {
        e.getOriginal().reviveCaps();
        SportsData from = SportsData.get(e.getOriginal());
        SportsData to = SportsData.get(e.getEntity());
        e.getOriginal().invalidateCaps();
        // После смерти мышцы свежие; при возвращении из Края — те же.
        if (from != null && to != null && !e.isWasDeath()) to.copyFrom(from);
    }

    private void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) SportsService.forget(sp);
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
        if (d != null) Muscles.recover(sp, d, e.seconds(), e.sleeping(), sp.level().getGameTime());
    }

    private void registerCommands(RegisterCommandsEvent e) {
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

    private static int rest(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        SportsService.rest(p);
        c.getSource().sendSuccess(() -> Component.translatable("rpsports.cmd.rested", p.getDisplayName()), true);
        return 1;
    }
}
