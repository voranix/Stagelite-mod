package com.stagelite.light;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/** Humo, fuego y fuegos artificiales. Humo y fuego son solo particulas (no dañan ni queman nada). */
public final class Effects {
    private Effects() {
    }

    /** Cliente: nube de humo. intensity = densidad, extra = alcance. */
    public static void smoke(Level level, BlockPos pos, Direction dir, LightData d) {
        RandomSource rnd = level.random;
        float on = LightFx.brightness(d, level.getGameTime());
        if (d.effect == Effect.STROBE && on <= 0f) {
            return; // en modo "estrobo" el humo sale a rafagas
        }
        int count = Math.max(1, d.intensity / 20);
        double speed = 0.04 + d.extra * 0.012;
        boolean white = (d.color & 0xFFFFFF) == 0xFFFFFF && d.effect != Effect.RAINBOW;
        int rgb = LightFx.color(d, level.getGameTime());
        Vector3f col = new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f);
        for (int i = 0; i < count; i++) {
            double x = pos.getX() + 0.5 + dir.getStepX() * 0.55 + (rnd.nextDouble() - 0.5) * 0.3;
            double y = pos.getY() + 0.5 + dir.getStepY() * 0.55 + (rnd.nextDouble() - 0.5) * 0.3;
            double z = pos.getZ() + 0.5 + dir.getStepZ() * 0.55 + (rnd.nextDouble() - 0.5) * 0.3;
            double vx = dir.getStepX() * speed + (rnd.nextDouble() - 0.5) * 0.03;
            double vy = dir.getStepY() * speed + (rnd.nextDouble() - 0.5) * 0.03 + 0.005;
            double vz = dir.getStepZ() * speed + (rnd.nextDouble() - 0.5) * 0.03;
            if (white) {
                level.addParticle(ParticleTypes.CLOUD, true, x, y, z, vx, vy, vz);
            } else {
                level.addParticle(new DustParticleOptions(col, 2.5f), true, x, y, z, vx, vy, vz);
            }
        }
    }

    /** Cliente: chorro de llamas. extra = altura. Modo "estrobo" = llamaradas intermitentes. */
    public static void fire(Level level, BlockPos pos, Direction dir, LightData d) {
        RandomSource rnd = level.random;
        if (d.effect == Effect.STROBE && LightFx.brightness(d, level.getGameTime()) <= 0f) {
            return;
        }
        double speed = 0.10 + d.extra * 0.035;
        for (int i = 0; i < 4; i++) {
            double x = pos.getX() + 0.5 + dir.getStepX() * 0.55 + (rnd.nextDouble() - 0.5) * 0.15;
            double y = pos.getY() + 0.5 + dir.getStepY() * 0.55 + (rnd.nextDouble() - 0.5) * 0.15;
            double z = pos.getZ() + 0.5 + dir.getStepZ() * 0.55 + (rnd.nextDouble() - 0.5) * 0.15;
            double vx = dir.getStepX() * speed + (rnd.nextDouble() - 0.5) * 0.04;
            double vy = dir.getStepY() * speed + (rnd.nextDouble() - 0.5) * 0.04;
            double vz = dir.getStepZ() * speed + (rnd.nextDouble() - 0.5) * 0.04;
            level.addParticle(ParticleTypes.FLAME, true, x, y, z, vx, vy, vz);
        }
        if (rnd.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, true,
                    pos.getX() + 0.5 + dir.getStepX() * (1.0 + d.extra * 0.3),
                    pos.getY() + 0.5 + dir.getStepY() * (1.0 + d.extra * 0.3),
                    pos.getZ() + 0.5 + dir.getStepZ() * (1.0 + d.extra * 0.3), 0, 0.02, 0);
        }
    }

    /** Servidor: lanza un cohete. extra = altura (1-10 -> vuelo 1..3). Efecto arcoiris = colores al azar. */
    public static void launchFirework(Level level, BlockPos pos, Direction dir, LightData d) {
        RandomSource rnd = level.random;
        int rgb = d.effect == Effect.RAINBOW
                ? net.minecraft.util.Mth.hsvToRgb(rnd.nextFloat(), 1f, 1f) & 0xFFFFFF
                : d.color & 0xFFFFFF;
        int fade = net.minecraft.util.Mth.hsvToRgb(rnd.nextFloat(), 0.6f, 1f) & 0xFFFFFF;

        CompoundTag explosion = new CompoundTag();
        explosion.putByte("Type", (byte) rnd.nextInt(5));
        explosion.put("Colors", new IntArrayTag(new int[]{rgb}));
        explosion.put("FadeColors", new IntArrayTag(new int[]{fade}));
        explosion.putBoolean("Flicker", rnd.nextBoolean());
        explosion.putBoolean("Trail", rnd.nextBoolean());
        ListTag explosions = new ListTag();
        explosions.add(explosion);
        CompoundTag fireworks = new CompoundTag();
        fireworks.putByte("Flight", (byte) Math.max(1, Math.min(3, 1 + d.extra / 4)));
        fireworks.put("Explosions", explosions);
        ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
        stack.getOrCreateTag().put("Fireworks", fireworks);

        FireworkRocketEntity rocket = new FireworkRocketEntity(level,
                pos.getX() + 0.5 + dir.getStepX() * 0.7,
                pos.getY() + 0.5 + dir.getStepY() * 0.7,
                pos.getZ() + 0.5 + dir.getStepZ() * 0.7, stack);
        level.addFreshEntity(rocket);
    }
}
