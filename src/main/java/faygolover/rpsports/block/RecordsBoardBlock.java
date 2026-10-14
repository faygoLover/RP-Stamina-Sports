package faygolover.rpsports.block;

import faygolover.rpsports.core.Exercise;
import faygolover.rpsports.server.Records;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Доска рекордов (docs/00 п. 10): ПКМ — первые три места сервера по каждому упражнению и свой рекорд. */
public class RecordsBoardBlock extends HorizontalDirectionalBlock {
    private final VoxelShape shapeNorth;

    public RecordsBoardBlock(Properties props, VoxelShape shape) {
        super(props);
        this.shapeNorth = shape;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes2.rotate(shapeNorth, state.getValue(FACING));
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.translatable("rpsports.records.board").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            for (Exercise e : Exercise.VALUES) for (Component c : Records.lines(sp.server, sp, e, 3)) sp.sendSystemMessage(c);
        }
        return InteractionResult.CONSUME;
    }
}
