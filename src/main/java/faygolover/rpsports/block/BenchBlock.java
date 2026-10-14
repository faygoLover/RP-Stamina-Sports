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
 * Скамья со штангой в два блока. Гриф 20 кг; блины — в окне скамьи (Shift+ПКМ пустой рукой): два ряда, стороны
 * должны быть одинаковыми. ПКМ — лечь и жать; если скамья занята — встать на страховку.
 */
public class BenchBlock extends MultiBlockMachine implements EntityBlock {
    public static final double BAR_KG = 20;

    /** Скамья в два блока: часть 0 — со стойкой и грифом (там же блины), часть 1 — под ноги. */
    public BenchBlock(Properties props, VoxelShape head, VoxelShape foot) {
        super(props, Exercise.BENCH, 2, 1, head, foot);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? new BenchBlockEntity(pos, state) : null;
    }

    /** ПКМ — лечь и жать (занято — встать на страховку); Shift+ПКМ пустой рукой — окно блинов (см. SportsService). */
    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BenchBlockEntity bench && !level.isClientSide)
            net.minecraft.world.Containers.dropContents(level, pos, bench.container());
        super.onRemove(state, level, pos, newState, moving);
    }

    /** Для подсказки «вес на грифе» при взгляде на скамью (клиент) — из данных блока. */
    public static double weightAt(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BenchBlockEntity b ? b.totalKg() : BAR_KG;
    }
}
