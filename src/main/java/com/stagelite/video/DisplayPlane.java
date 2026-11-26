package com.stagelite.video;

import com.stagelite.block.StageLightBlock;
import com.stagelite.blockentity.StageLightBlockEntity;
import com.stagelite.light.FixtureType;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Donde y de que tamano se ve la imagen de una pantalla, marco o proyector (cliente y servidor usan lo mismo). */
public final class DisplayPlane {
    public static final double[] RATIOS = {16.0 / 9.0, 9.0 / 16.0, 4.0 / 3.0, 1.0, 21.0 / 9.0};
    public static final String[] RATIO_NAMES = {"16:9", "9:16", "4:3", "1:1", "21:9"};

    public final Vec3 center;
    public final Vec3 right;
    public final Vec3 up;
    public final Vec3 normal;
    public final double hw;
    public final double hh;

    private DisplayPlane(Vec3 center, Vec3 right, Vec3 up, Vec3 normal, double hw, double hh) {
        this.center = center;
        this.right = right;
        this.up = up;
        this.normal = normal;
        this.hw = hw;
        this.hh = hh;
    }

    private static Vec3 vec(Direction d) {
        return new Vec3(d.getStepX(), d.getStepY(), d.getStepZ());
    }

    /** null = no hay donde dibujar (por ejemplo, un proyector que no apunta a ninguna superficie). */
    public static DisplayPlane of(Level level, StageLightBlockEntity be) {
        FixtureType type = be.type();
        if (!type.isVideo()) {
            return null;
        }
        Direction f = be.getBlockState().getValue(StageLightBlock.FACING);
        double ratio = RATIOS[Math.max(0, Math.min(RATIOS.length - 1, be.data.aspect))];
        double size;
        Vec3 center;
        Vec3 normal;
        Vec3 up = new Vec3(0, 1, 0);
        double px = be.getBlockPos().getX() + 0.5;
        double py = be.getBlockPos().getY() + 0.5;
        double pz = be.getBlockPos().getZ() + 0.5;

        if (type == FixtureType.PROJECTOR) {
            double reach = Math.max(4, be.data.range);
            Vec3 from = new Vec3(px + f.getStepX() * 0.55, py + f.getStepY() * 0.55, pz + f.getStepZ() * 0.55);
            Vec3 to = from.add(f.getStepX() * reach, f.getStepY() * reach, f.getStepZ() * reach);
            BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
            if (hit.getType() != HitResult.Type.BLOCK) {
                return null;
            }
            Direction n = hit.getDirection();
            normal = vec(n);
            center = hit.getLocation().add(normal.scale(0.004));
            if (n.getAxis() == Direction.Axis.Y) {
                // en suelo o techo: la "parte de arriba" de la imagen apunta lejos del proyector
                up = f.getAxis() == Direction.Axis.Y ? new Vec3(0, 0, -1) : vec(f);
            }
            size = be.data.extra * 3.0;
        } else if (type == FixtureType.WALL) {
            f = WallScreens.facing(be.getBlockState());
            if (!WallScreens.isMaster(level, be.getBlockPos(), f)) {
                return null; // solo la esquina inferior izquierda dibuja
            }
            int[] wh = WallScreens.size(level, be.getBlockPos(), f);
            Vec3 n = vec(f);
            Vec3 r = new Vec3(0, 1, 0).cross(n);
            Vec3 c = new Vec3(px, py, pz).add(r.scale((wh[0] - 1) / 2.0)).add(0, (wh[1] - 1) / 2.0, 0)
                    .subtract(n.scale(0.375 - 0.004));
            return new DisplayPlane(c, r, new Vec3(0, 1, 0), n, wh[0] / 2.0, wh[1] / 2.0);
        } else if (type == FixtureType.TV) {
            f = WallScreens.facing(be.getBlockState());
            Vec3 n = vec(f);
            Vec3 c = new Vec3(px, py, pz).subtract(n.scale(0.3125 - 0.004));
            return new DisplayPlane(c, new Vec3(0, 1, 0).cross(n), new Vec3(0, 1, 0), n, 0.375, 3.375 / 16.0);
        } else {
            if (f.getAxis() == Direction.Axis.Y) {
                f = Direction.NORTH;
            }
            normal = vec(f);
            center = new Vec3(px - f.getStepX() * 0.375 + f.getStepX() * 0.004, py, pz - f.getStepZ() * 0.375 + f.getStepZ() * 0.004);
            size = type == FixtureType.SCREEN ? be.data.extra * 2.0 : be.data.extra;
        }
        Vec3 right = up.cross(normal);
        double hw = size / 2.0 * (ratio >= 1.0 ? 1.0 : ratio);
        double hh = size / 2.0 * (ratio >= 1.0 ? 1.0 / ratio : 1.0);
        return new DisplayPlane(center, right, up, normal, hw, hh);
    }

    /** Distancia a lo largo del rayo hasta la imagen, o -1 si no la toca. */
    public double hit(Vec3 eye, Vec3 look, double max) {
        double denom = look.dot(normal);
        if (denom >= -1e-4) {
            return -1; // mira hacia otro lado
        }
        double t = center.subtract(eye).dot(normal) / denom;
        if (t < 0.05 || t > max) {
            return -1;
        }
        Vec3 d = eye.add(look.scale(t)).subtract(center);
        return Math.abs(d.dot(right)) <= hw && Math.abs(d.dot(up)) <= hh ? t : -1;
    }
}
