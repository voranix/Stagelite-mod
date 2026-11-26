package com.stagelite.block;

import com.stagelite.entity.SeatEntity;
import com.stagelite.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/** Butaca de cine: clic derecho para sentarse (agachado para levantarse). Mira hacia donde mira quien la coloca. */
public class CinemaSeatBlock extends HorizontalDirectionalBlock {
    private static final double[][] NORTH_BOXES = {{3, 0, 3, 13, 5, 13}, {1, 5, 2, 15, 9, 15}, {1, 9, 12, 15, 16, 16}, {0, 9, 2, 1, 12, 14}, {15, 9, 2, 16, 12, 14}};
    private static final VoxelShape[] SHAPES = new VoxelShape[4];

    static {
        for (int turns = 0; turns < 4; turns++) {
            VoxelShape s = Shapes.empty();
            for (double[] b : NORTH_BOXES) {
                double x0 = b[0], z0 = b[2], x1 = b[3], z1 = b[5];
                for (int i = 0; i < turns; i++) {
                    double nx0 = 16 - z1, nx1 = 16 - z0;
                    double nz0 = x0, nz1 = x1;
                    x0 = nx0; x1 = nx1; z0 = nz0; z1 = nz1;
                }
                s = Shapes.or(s, Block.box(x0, b[1], z0, x1, b[4], z1));
            }
            SHAPES[turns] = s;
        }
    }

    public CinemaSeatBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.0F, 6.0F).sound(SoundType.WOOL).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    private static int turns(Direction d) {
        switch (d) {
            case EAST:
                return 1;
            case SOUTH:
                return 2;
            case WEST:
                return 3;
            default:
                return 0;
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES[turns(state.getValue(FACING))];
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() || player.isPassenger()) {
            return InteractionResult.PASS;
        }
        List<SeatEntity> taken = level.getEntitiesOfClass(SeatEntity.class, new AABB(pos));
        if (!taken.isEmpty()) {
            return InteractionResult.CONSUME;
        }
        Direction f = state.getValue(FACING);
        SeatEntity seat = ModEntities.SEAT.get().create(level);
        if (seat == null) {
            return InteractionResult.PASS;
        }
        seat.setPos(pos.getX() + 0.5 - f.getStepX() * 0.1, pos.getY() + 0.5, pos.getZ() + 0.5 - f.getStepZ() * 0.1);
        level.addFreshEntity(seat);
        player.setYRot(f.toYRot());
        player.startRiding(seat, true);
        return InteractionResult.CONSUME;
    }
}
