package com.stagelite.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Barra de estructura de escenario: se une sola con las barras vecinas en las 6 direcciones. */
public class TrussBlock extends Block {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    private static final VoxelShape CORE = Block.box(2, 2, 2, 14, 14, 14);
    private static final VoxelShape[] ARMS = new VoxelShape[6];

    static {
        ARMS[Direction.NORTH.get3DDataValue()] = Block.box(2, 2, 0, 14, 14, 2);
        ARMS[Direction.SOUTH.get3DDataValue()] = Block.box(2, 2, 14, 14, 14, 16);
        ARMS[Direction.WEST.get3DDataValue()] = Block.box(0, 2, 2, 2, 14, 14);
        ARMS[Direction.EAST.get3DDataValue()] = Block.box(14, 2, 2, 16, 14, 14);
        ARMS[Direction.UP.get3DDataValue()] = Block.box(2, 14, 2, 14, 16, 14);
        ARMS[Direction.DOWN.get3DDataValue()] = Block.box(2, 0, 2, 14, 2, 14);
    }

    public TrussBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0F, 6.0F).sound(SoundType.METAL).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
    }

    public static BooleanProperty prop(Direction d) {
        switch (d) {
            case NORTH:
                return NORTH;
            case EAST:
                return EAST;
            case SOUTH:
                return SOUTH;
            case WEST:
                return WEST;
            case UP:
                return UP;
            default:
                return DOWN;
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = defaultBlockState();
        for (Direction d : Direction.values()) {
            s = s.setValue(prop(d), ctx.getLevel().getBlockState(ctx.getClickedPos().relative(d)).getBlock() instanceof TrussBlock);
        }
        return s;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(prop(dir), neighbor.getBlock() instanceof TrussBlock);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        VoxelShape shape = CORE;
        for (Direction d : Direction.values()) {
            if (state.getValue(prop(d))) {
                shape = Shapes.or(shape, ARMS[d.get3DDataValue()]);
            }
        }
        return shape;
    }
}
