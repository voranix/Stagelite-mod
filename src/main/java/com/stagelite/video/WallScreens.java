package com.stagelite.video;

import com.stagelite.block.StageLightBlock;
import com.stagelite.light.FixtureType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Modulos de pantalla: los vecinos del mismo lado forman un rectangulo (max 24x24) cuya esquina inferior izquierda manda. */
public final class WallScreens {
    public static final int MAX = 24;

    private WallScreens() {
    }

    public static Direction facing(BlockState state) {
        Direction f = state.getValue(StageLightBlock.FACING);
        return f.getAxis() == Direction.Axis.Y ? Direction.NORTH : f;
    }

    /** Derecha vista de frente. */
    public static Direction right(Direction f) {
        return f.getCounterClockWise();
    }

    public static boolean same(Level level, BlockPos pos, Direction f) {
        BlockState s = level.getBlockState(pos);
        return s.getBlock() instanceof StageLightBlock b && b.fixtureType() == FixtureType.WALL && facing(s) == f;
    }

    public static boolean isMaster(Level level, BlockPos pos, Direction f) {
        return !same(level, pos.relative(right(f).getOpposite()), f) && !same(level, pos.below(), f);
    }

    /** Bloque que manda en la pantalla a la que pertenece "pos" (esquina inferior izquierda). */
    public static BlockPos master(Level level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (!(s.getBlock() instanceof StageLightBlock)) {
            return pos;
        }
        Direction f = facing(s);
        Direction left = right(f).getOpposite();
        BlockPos p = pos;
        for (int i = 0; i < MAX * 2; i++) {
            if (same(level, p.relative(left), f)) {
                p = p.relative(left);
            } else if (same(level, p.below(), f)) {
                p = p.below();
            } else {
                break;
            }
        }
        return p;
    }

    /** Ancho y alto en bloques del rectangulo que parte de la esquina "master". */
    public static int[] size(Level level, BlockPos master, Direction f) {
        Direction r = right(f);
        int w = 1;
        while (w < MAX && same(level, master.relative(r, w), f)) {
            w++;
        }
        int h = 1;
        while (h < MAX && same(level, master.above(h), f)) {
            h++;
        }
        return new int[]{w, h};
    }
}
