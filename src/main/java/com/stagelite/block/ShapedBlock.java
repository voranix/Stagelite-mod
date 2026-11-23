package com.stagelite.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Bloque decorativo con forma propia (atriles, etc.). */
public class ShapedBlock extends Block {
    private final VoxelShape shape;

    public ShapedBlock(VoxelShape shape) {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5F, 6.0F).sound(SoundType.METAL).noOcclusion());
        this.shape = shape;
    }

    public static VoxelShape boxes(double[]... b) {
        VoxelShape s = Shapes.empty();
        for (double[] a : b) {
            s = Shapes.or(s, Block.box(a[0], a[1], a[2], a[3], a[4], a[5]));
        }
        return s;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shape;
    }
}
