package com.stagelite.entity;

import com.stagelite.block.CinemaSeatBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/** Entidad invisible sobre la que se sienta el jugador en una butaca. */
public class SeatEntity extends Entity {
    public SeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.0;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && (getPassengers().isEmpty() || !(level().getBlockState(blockPosition()).getBlock() instanceof CinemaSeatBlock))) {
            discard();
        }
    }
}
