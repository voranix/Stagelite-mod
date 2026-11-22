package com.stagelite.block;

import com.stagelite.blockentity.StageLightBlockEntity;
import com.stagelite.config.StageLiteConfig;
import com.stagelite.light.FixtureType;
import com.stagelite.light.LightData;
import com.stagelite.registry.ModBlockEntities;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.stagelite.network.ModNetwork;
import com.stagelite.network.OpenLightScreenPacket;
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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Foco de escenario. Se pone mirando hacia la cara clicada (en el techo apunta hacia abajo).
 * Clic derecho (solo staff) abre el panel de control.
 *
 * Es indestructible en supervivencia: solo se puede quitar en creativo.
 */
public class StageLightBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    /** Nivel de luz de bloque, lo actualiza el block entity. */
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);
    /** true = el modelo fijo se oculta y lo dibuja el renderer inclinado. */
    public static final BooleanProperty TILTED = BooleanProperty.create("tilted");

    private static final VoxelShape SHAPE = Block.box(3, 3, 3, 13, 13, 13);
    private static final VoxelShape FLOOR_SHAPE = Block.box(0, 0, 0, 16, 15, 16);

    private final FixtureType type;

    public StageLightBlock(FixtureType type) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(-1.0F, 3600000.0F)
                .sound(SoundType.METAL)
                .noOcclusion()
                .lightLevel(state -> state.getValue(LIGHT)));
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(LIGHT, 0).setValue(TILTED, false));
    }

    public FixtureType fixtureType() {
        return type;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIGHT, TILTED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LightData defaults = new LightData();
        type.applyDefaults(defaults);
        return defaultBlockState().setValue(FACING, context.getClickedFace())
                .setValue(LIGHT, type.lightLevel(defaults));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return type.isFloor() ? FLOOR_SHAPE : SHAPE;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer,
                            net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof StageLightBlockEntity be) {
            be.joinNeighbors();
        }
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StageLightBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> beType) {
        if (!type.ticks() || beType != ModBlockEntities.STAGE_LIGHT.get()) {
            return null;
        }
        return (lvl, p, st, be) -> {
            if (be instanceof StageLightBlockEntity light) {
                light.tick(lvl);
            }
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (StageLiteConfig.canEdit(serverPlayer)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof StageLightBlockEntity light) {
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new OpenLightScreenPacket(pos, type.ordinal(), light.data.toTag()));
                }
            } else {
                serverPlayer.displayClientMessage(Component.translatable("message.stagelite.no_permission"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
