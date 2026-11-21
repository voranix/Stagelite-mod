package com.stagelite.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Molinete de 5x5 bloques: 17 piezas (una por cada bloque de la huella). Se coloca de una vez con su item. */
public class MolineteBlock extends Block {
    public static final EnumProperty<MolinetePart> PART = EnumProperty.create("part", MolinetePart.class);

    private static final VoxelShape SHAPE_CENTER = Block.box(0, 0, 0, 16, 27.2, 16);
    private static final VoxelShape SHAPE_INNER = Block.box(0, 0, 0, 16, 19, 16);
    private static final VoxelShape SHAPE_OUTER = Block.box(0, 14, 0, 16, 19, 16);
    private static final VoxelShape COLLISION_BASE = Block.box(0, 0, 0, 16, 8, 16);
    private static final VoxelShape COLLISION_CENTER = Shapes.or(COLLISION_BASE, Block.box(3.5, 8, 3.5, 12.5, 27, 12.5));

    public MolineteBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(-1.0F, 3600000.0F)
                .sound(SoundType.WOOL)
                .noOcclusion()
                .pushReaction(PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(PART, MolinetePart.X0_Z0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        MolinetePart part = state.getValue(PART);
        if (part.isCenter()) {
            return SHAPE_CENTER;
        }
        return part.isInner() ? SHAPE_INNER : SHAPE_OUTER;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        MolinetePart part = state.getValue(PART);
        if (part.isCenter()) {
            return COLLISION_CENTER;
        }
        return part.isInner() ? COLLISION_BASE : Shapes.empty();
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    /** Al romper una pieza (solo en creativo) se quitan las 17. */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            MolinetePart mine = state.getValue(PART);
            BlockPos center = pos.offset(-mine.dx, 0, -mine.dz);
            for (MolinetePart part : MolinetePart.values()) {
                BlockPos p = center.offset(part.dx, 0, part.dz);
                BlockState other = level.getBlockState(p);
                if (!p.equals(pos) && other.is(this) && other.getValue(PART) == part) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 35);
                }
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }
}
