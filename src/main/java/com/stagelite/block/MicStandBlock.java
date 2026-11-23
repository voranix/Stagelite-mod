package com.stagelite.block;

import com.stagelite.config.StageLiteConfig;
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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Atril de micrófono de mesa con brazo. Clic derecho cambia la inclinación del brazo (agachado: al revés). */
public class MicStandBlock extends HorizontalDirectionalBlock {
    /** 0 = abajo 22,5°, 1 = recto, 2 = arriba 22,5°, 3 = arriba 45°. */
    public static final IntegerProperty TILT = IntegerProperty.create("tilt", 0, 3);
    private static final String[] TILT_NAMES = {"-22,5°", "0°", "22,5°", "45°"};
    private static final double[][] NORTH_BOXES = {{4, 0, 4, 12, 1, 12}, {7, 1, 7, 9, 11, 9}, {7, 10, 0, 9, 13, 12}};
    private static final VoxelShape[] SHAPES = new VoxelShape[4];

    static {
        for (int turns = 0; turns < 4; turns++) {
            VoxelShape s = Shapes.empty();
            for (double[] b : NORTH_BOXES) {
                double x0 = b[0], z0 = b[2], x1 = b[3], z1 = b[5];
                for (int i = 0; i < turns; i++) {
                    double nx0 = 16 - z1, nx1 = 16 - z0, nz0 = x0, nz1 = x1;
                    x0 = nx0; x1 = nx1; z0 = nz0; z1 = nz1;
                }
                s = Shapes.or(s, Block.box(x0, b[1], z0, x1, b[4], z1));
            }
            SHAPES[turns] = s;
        }
    }

    public MicStandBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5F, 6.0F).sound(SoundType.METAL).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TILT, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TILT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()).setValue(TILT, 1);
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
        if (player instanceof ServerPlayer sp && !StageLiteConfig.canEdit(sp)) {
            sp.displayClientMessage(Component.translatable("message.stagelite.no_permission"), true);
            return InteractionResult.CONSUME;
        }
        int t = (state.getValue(TILT) + (player.isShiftKeyDown() ? 3 : 1)) % 4;
        level.setBlock(pos, state.setValue(TILT, t), 3);
        player.displayClientMessage(Component.translatable("message.stagelite.mic_tilt", TILT_NAMES[t]), true);
        return InteractionResult.CONSUME;
    }
}
