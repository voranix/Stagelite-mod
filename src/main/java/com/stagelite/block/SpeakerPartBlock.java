package com.stagelite.block;

import com.stagelite.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/** Pieza sin datos de un parlante grande: abre el panel del bloque de control y se rompe junto con el. */
public class SpeakerPartBlock extends Block {
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);
    public static final EnumProperty<SpeakerPart> PART = EnumProperty.create("part", SpeakerPart.class);

    public SpeakerPartBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(-1.0F, 3600000.0F).sound(SoundType.METAL));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, SpeakerPart.M_1_0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    /** Posicion del bloque de control (esquina inferior izquierda vista de frente). */
    public static BlockPos origin(BlockState state, BlockPos pos) {
        SpeakerPart part = state.getValue(PART);
        Direction right = state.getValue(FACING).getCounterClockWise();
        return pos.offset(-right.getStepX() * part.x, -part.y, -right.getStepZ() * part.x);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos origin = origin(state, pos);
        BlockState os = level.getBlockState(origin);
        if (os.getBlock() instanceof StageLightBlock) {
            return os.use(level, player, hand, new BlockHitResult(hit.getLocation(), hit.getDirection(), origin, hit.isInside()));
        }
        return InteractionResult.PASS;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        BlockState os = level.getBlockState(origin(state, pos));
        return os.getBlock() instanceof StageLightBlock ? new ItemStack(os.getBlock().asItem()) : ItemStack.EMPTY;
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            StageLightBlock.removeSpeaker(level, origin(state, pos), pos);
        }
        super.playerWillDestroy(level, pos, state, player);
    }
}
