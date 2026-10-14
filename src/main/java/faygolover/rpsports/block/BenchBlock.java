package faygolover.rpsports.block;

import faygolover.rpsports.core.Exercise;
import faygolover.rpsports.item.PlateItem;
import faygolover.rpsports.server.SportsService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Скамья со штангой. Гриф 20 кг; ПКМ с блином — повесить пару (по одному с каждой стороны), Shift+ПКМ пустой рукой —
 * снять последнюю пару. ПКМ — лечь и жать; если скамья занята — встать на страховку.
 */
public class BenchBlock extends MachineBlock implements EntityBlock {
    public static final double BAR_KG = 20;
    public static final int MAX_PAIRS = 6;

    public BenchBlock(Properties props, VoxelShape shape) {
        super(props, shape, Exercise.BENCH);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BenchBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof BenchBlockEntity bench)) return InteractionResult.PASS;
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof PlateItem plate) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (SportsService.benchBusy(level, pos)) {
                player.displayClientMessage(Component.translatable("rpsports.msg.bench_busy").withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.CONSUME;
            }
            if (bench.pairs() >= MAX_PAIRS || held.getCount() < 2 && !player.isCreative()) {
                player.displayClientMessage(Component.translatable(bench.pairs() >= MAX_PAIRS
                        ? "rpsports.msg.bar_full" : "rpsports.msg.need_pair").withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.CONSUME;
            }
            bench.addPair(plate.kg());
            if (!player.isCreative()) held.shrink(2);
            level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON, SoundSource.BLOCKS, 0.8f, 1.3f);
            showWeight(player, bench);
            return InteractionResult.CONSUME;
        }
        if (held.isEmpty() && player.isShiftKeyDown()) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (SportsService.benchBusy(level, pos)) return InteractionResult.CONSUME;
            Double kg = bench.removePair();
            if (kg != null) {
                ItemStack back = PlateItem.stackFor(kg, 2);
                if (!back.isEmpty() && !player.getInventory().add(back)) player.drop(back, false);
                level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON, SoundSource.BLOCKS, 0.8f, 1.0f);
            }
            showWeight(player, bench);
            return InteractionResult.CONSUME;
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    private static void showWeight(Player player, BenchBlockEntity bench) {
        player.displayClientMessage(Component.translatable("rpsports.msg.bar_weight", PlateItem.format(bench.totalKg())), true);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BenchBlockEntity bench && !level.isClientSide) {
            for (double kg : bench.plates()) {
                ItemStack st = PlateItem.stackFor(kg, 2);
                if (!st.isEmpty()) net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY() + 0.5, pos.getZ(), st);
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    /** Для подсказки «вес на грифе» при взгляде на скамью (клиент) — из данных блока. */
    public static double weightAt(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BenchBlockEntity b ? b.totalKg() : BAR_KG;
    }

    static void sendWeight(ServerPlayer sp, BenchBlockEntity bench) {
        showWeight(sp, bench);
    }
}
