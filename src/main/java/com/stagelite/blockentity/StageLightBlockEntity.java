package com.stagelite.blockentity;

import com.stagelite.block.StageLightBlock;
import com.stagelite.light.LightData;
import com.stagelite.light.LightRegistry;
import com.stagelite.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public class StageLightBlockEntity extends BlockEntity {
    public final LightData data = new LightData();

    // Cache solo del cliente: largo del haz recortado por bloques.
    public float clippedLength = -1f;
    public long clipTime = Long.MIN_VALUE;

    private AABB renderBox;

    public StageLightBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STAGE_LIGHT.get(), pos, state);
        if (state.getBlock() instanceof StageLightBlock block) {
            block.fixtureType().applyDefaults(data);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            LightRegistry.add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        LightRegistry.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        LightRegistry.remove(this);
    }

    /** Aplica un estado nuevo y lo envia a los clientes. Solo en el servidor. */
    public void applyAndSync(LightData newData) {
        data.copyFrom(newData);
        data.clamp();
        sync();
    }

    /** Guarda, actualiza el nivel de luz del bloque y avisa a los clientes. Solo en el servidor. */
    public void sync() {
        if (level == null || level.isClientSide) {
            return;
        }
        setChanged();
        BlockState state = getBlockState();
        int wanted = data.blockLightLevel();
        if (state.getValue(StageLightBlock.LIGHT) != wanted) {
            level.setBlock(worldPosition, state.setValue(StageLightBlock.LIGHT, wanted), 3);
        } else {
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("light", data.toTag());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("light", Tag.TAG_COMPOUND)) {
            data.load(tag.getCompound("light"));
        }
        renderBox = null;
        clipTime = Long.MIN_VALUE;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Caja grande para que el haz no desaparezca al mirar hacia otro lado. */
    @Override
    public AABB getRenderBoundingBox() {
        if (renderBox == null) {
            renderBox = new AABB(worldPosition).inflate(LightData.MAX_LENGTH + 2);
        }
        return renderBox;
    }
}
