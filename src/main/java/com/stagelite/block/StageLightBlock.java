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
        if (type.partsW() > 1) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }
        if (type == FixtureType.PROJECTOR) {
            // el proyector apunta hacia donde miras
            return defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
        }
        if (type == FixtureType.TV) {
            // solo en paredes: se coloca en el costado de un bloque y mira hacia afuera
            Direction face = context.getClickedFace();
            if (face.getAxis() == Direction.Axis.Y) {
                return null;
            }
            BlockState s = defaultBlockState().setValue(FACING, face);
            return s.canSurvive(context.getLevel(), context.getClickedPos()) ? s : null;
        }
        if (type.isVideo()) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }
        return defaultBlockState().setValue(FACING, context.getClickedFace())
                .setValue(LIGHT, type.lightLevel(defaults));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (type.partsW() > 1) {
            return net.minecraft.world.phys.shapes.Shapes.block();
        }
        if (type == FixtureType.TV) {
            switch (state.getValue(FACING)) {
                case SOUTH:
                    return Block.box(1, 3, 0, 15, 13, 3);
                case EAST:
                    return Block.box(0, 3, 1, 3, 13, 15);
                case WEST:
                    return Block.box(13, 3, 1, 16, 13, 15);
                default:
                    return Block.box(1, 3, 13, 15, 13, 16);
            }
        }
        if (type.isVideo() && type != FixtureType.PROJECTOR) {
            switch (state.getValue(FACING)) {
                case SOUTH:
                    return Block.box(0, 0, 0, 16, 16, 2);
                case EAST:
                    return Block.box(0, 0, 0, 2, 16, 16);
                case WEST:
                    return Block.box(14, 0, 0, 16, 16, 16);
                default:
                    return Block.box(0, 0, 14, 16, 16, 16);
            }
        }
        return type.isFloor() ? FLOOR_SHAPE : SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        if (type != FixtureType.TV) {
            return true;
        }
        Direction f = state.getValue(FACING);
        BlockPos back = pos.relative(f.getOpposite());
        return level.getBlockState(back).isFaceSturdy(level, back, f);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, net.minecraft.world.level.LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        if (type == FixtureType.TV && dir == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer,
                            net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof StageLightBlockEntity be) {
            be.joinNeighbors();
            if (type == FixtureType.WALL) {
                // el modulo nuevo hereda el video de la pantalla a la que se une
                Direction f = com.stagelite.video.WallScreens.facing(state);
                Direction r = com.stagelite.video.WallScreens.right(f);
                for (BlockPos n : new BlockPos[]{pos.relative(r), pos.above(), pos.relative(r.getOpposite()), pos.below()}) {
                    if (com.stagelite.video.WallScreens.same(level, n, f) && level.getBlockEntity(n) instanceof StageLightBlockEntity nb
                            && !nb.data.url.isEmpty()) {
                        LightData copy = new LightData();
                        copy.copyFrom(nb.data);
                        be.applyAndSync(copy);
                        break;
                    }
                }
            }
        }
    }

    /** Quita todas las piezas de un parlante grande (menos la posicion "skip", que ya se esta rompiendo). */
    public static void removeSpeaker(Level level, BlockPos origin, BlockPos skip) {
        BlockState os = level.getBlockState(origin);
        if (!(os.getBlock() instanceof StageLightBlock slb) || !slb.type.isSpeaker() || slb.type.partsW() <= 1) {
            return;
        }
        Direction right = os.getValue(FACING).getCounterClockWise();
        for (int x = 0; x < slb.type.partsW(); x++) {
            for (int y = 0; y < slb.type.partsH(); y++) {
                BlockPos p = origin.offset(right.getStepX() * x, y, right.getStepZ() * x);
                if (!p.equals(skip) && (level.getBlockState(p).getBlock() instanceof SpeakerPartBlock || p.equals(origin))) {
                    level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 35);
                }
            }
        }
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && type.partsW() > 1) {
            removeSpeaker(level, pos, pos);
        }
        super.playerWillDestroy(level, pos, state, player);
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
        // Humo, fuego y musica solo trabajan en el cliente; los fuegos artificiales solo en el servidor.
        boolean serverWork = type == FixtureType.FIREWORK;
        if (level.isClientSide == serverWork) {
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
        // Con el control remoto en la mano, el clic lo maneja el control (enlazar pantallas / conectar parlantes).
        if (player.getMainHandItem().getItem() instanceof com.stagelite.item.VideoRemoteItem
                || player.getOffhandItem().getItem() instanceof com.stagelite.item.VideoRemoteItem) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (StageLiteConfig.canEdit(serverPlayer)) {
                BlockPos target = type == FixtureType.WALL ? com.stagelite.video.WallScreens.master(level, pos) : pos;
                BlockEntity be = level.getBlockEntity(target);
                if (be instanceof StageLightBlockEntity light) {
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new OpenLightScreenPacket(target, type.ordinal(), light.data.toTag()));
                }
            } else {
                serverPlayer.displayClientMessage(Component.translatable("message.stagelite.no_permission"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
