package faygolover.rpsports.block;

import faygolover.rpsports.core.Exercise;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Тренажёр из нескольких блоков: сетка {@code cols × rows}. Столбцы идут вправо от игрока, который его ставил
 * (по часовой от {@code FACING}), ряды — вверх. Часть 0 — основание: к нему привязано упражнение и с него дроп.
 * Ставится основанием, остальные части появляются сами (нужно свободное место); сломать любую — пропадает весь
 * (как у двери: соседние части по очереди превращаются в воздух).
 */
public class MultiBlockMachine extends MachineBlock {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 5);
    private final int cols, rows;
    private final VoxelShape[] shapes;

    /** {@code shapes[part]} — форма части для «на север». */
    public MultiBlockMachine(Properties props, @Nullable Exercise exercise, int cols, int rows, VoxelShape... shapes) {
        super(props, shapes[0], exercise);
        this.cols = cols;
        this.rows = rows;
        this.shapes = shapes;
        registerDefaultState(defaultBlockState().setValue(PART, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(PART);
    }

    private int col(BlockState s) {
        return s.getValue(PART) % cols;
    }

    private int row(BlockState s) {
        return s.getValue(PART) / cols;
    }

    private static Direction right(Direction facing) {
        return facing.getClockWise();
    }

    /** Блок части (col, row) от основания. */
    private BlockPos at(BlockPos base, Direction facing, int col, int row) {
        return base.relative(right(facing), col).above(row);
    }

    @Override
    protected BlockPos basePos(BlockState state, BlockPos pos) {
        return pos.relative(right(state.getValue(FACING)), -col(state)).below(row(state));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = super.getStateForPlacement(ctx);
        Level level = ctx.getLevel();
        BlockPos base = ctx.getClickedPos();
        Direction facing = state.getValue(FACING);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (r == 0 && c == 0) continue;
                BlockPos p = at(base, facing, c, r);
                if (p.getY() >= level.getMaxBuildHeight() || !level.getBlockState(p).canBeReplaced(ctx)) return null;
            }
        }
        return state.setValue(PART, 0);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        Direction facing = state.getValue(FACING);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (r == 0 && c == 0) continue;
                level.setBlock(at(pos, facing, c, r), state.setValue(PART, r * cols + c), 3);
            }
        }
    }

    /** Соседняя часть пропала (или чужой блок) — пропадает и эта. */
    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction facing = state.getValue(FACING);
        int c = col(state), r = row(state), nc = c, nr = r;
        if (dir == Direction.UP) nr++;
        else if (dir == Direction.DOWN) nr--;
        else if (dir == right(facing)) nc++;
        else if (dir == right(facing).getOpposite()) nc--;
        else return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
        if (nc < 0 || nc >= cols || nr < 0 || nr >= rows) return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
        boolean ok = neighbor.is(this) && neighbor.getValue(FACING) == facing && neighbor.getValue(PART) == nr * cols + nc;
        return ok ? state : Blocks.AIR.defaultBlockState();
    }

    /** В творческом ломают любую часть — основание убрать без дропа (иначе оно выпадет предметом). */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && state.getValue(PART) != 0) {
            BlockPos base = basePos(state, pos);
            BlockState b = level.getBlockState(base);
            if (b.is(this) && b.getValue(PART) == 0) {
                level.setBlock(base, Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, base, Block.getId(b));
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        int part = Math.min(shapes.length - 1, state.getValue(PART));
        return Shapes2.rotate(shapes[part], state.getValue(FACING));
    }
}
