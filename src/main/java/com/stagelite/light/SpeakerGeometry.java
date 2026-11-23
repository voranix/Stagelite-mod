package com.stagelite.light;

import com.stagelite.block.StageLightBlock;
import com.stagelite.blockentity.StageLightBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** Centro de un parlante (los de varios bloques suenan desde su centro, no desde el bloque de control). */
public final class SpeakerGeometry {
    private SpeakerGeometry() {
    }

    public static Vec3 center(StageLightBlockEntity be) {
        FixtureType t = be.type();
        BlockPos p = be.getBlockPos();
        if (t.partsW() <= 1) {
            return Vec3.atCenterOf(p);
        }
        Direction f = be.getBlockState().getValue(StageLightBlock.FACING);
        if (f.getAxis() == Direction.Axis.Y) {
            f = Direction.NORTH;
        }
        Direction r = f.getCounterClockWise();
        double ox = (t.partsW() - 1) / 2.0;
        double oy = (t.partsH() - 1) / 2.0;
        return new Vec3(p.getX() + 0.5 + r.getStepX() * ox, p.getY() + 0.5 + oy, p.getZ() + 0.5 + r.getStepZ() * ox);
    }
}
