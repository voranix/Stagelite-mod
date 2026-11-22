package com.stagelite.blockentity;

import com.stagelite.block.StageLightBlock;
import com.stagelite.config.StageLiteConfig;
import com.stagelite.light.Effects;
import com.stagelite.light.FixtureType;
import com.stagelite.light.LightData;
import com.stagelite.light.LightRegistry;
import com.stagelite.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public class StageLightBlockEntity extends BlockEntity {
    public final LightData data = new LightData();

    // Cache solo del cliente: largo del haz recortado por bloques.
    public float clippedLength = -1f;
    public long clipTime = Long.MIN_VALUE;

    // Cache solo del cliente: centro de la pista conectada.
    public float floorCx;
    public float floorCz;
    public long floorTime = Long.MIN_VALUE;

    private AABB renderBox;
    private int fireworkCooldown = 20;

    public StageLightBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STAGE_LIGHT.get(), pos, state);
        if (state.getBlock() instanceof StageLightBlock block) {
            block.fixtureType().applyDefaults(data);
        }
    }

    /** Baldosas de pista conectadas (vecinas por los 4 lados, mismo grupo y altura). Maximo 1024. */
    public static java.util.List<StageLightBlockEntity> floorCluster(Level lvl, BlockPos start, int group) {
        java.util.List<StageLightBlockEntity> out = new java.util.ArrayList<>();
        java.util.Set<BlockPos> seen = new java.util.HashSet<>();
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        net.minecraft.core.Direction[] dirs = {net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.SOUTH,
                net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.WEST};
        while (!queue.isEmpty() && out.size() < 1024) {
            BlockPos p = queue.poll();
            if (!lvl.hasChunkAt(p)) {
                continue;
            }
            if (lvl.getBlockEntity(p) instanceof StageLightBlockEntity e && e.type().isFloor() && e.data.group == group) {
                out.add(e);
                for (net.minecraft.core.Direction dir : dirs) {
                    BlockPos n = p.relative(dir);
                    if (seen.add(n)) {
                        queue.add(n);
                    }
                }
            }
        }
        return out;
    }

    /** Cliente: calcula (cada ~2 s) el centro de la pista conectada para los patrones. */
    public void updateFloorCenter(Level lvl) {
        long now = lvl.getGameTime();
        if (floorTime != Long.MIN_VALUE && Math.abs(now - floorTime) < 40) {
            return;
        }
        floorTime = now + (worldPosition.hashCode() & 15);
        int minX = worldPosition.getX();
        int maxX = minX;
        int minZ = worldPosition.getZ();
        int maxZ = minZ;
        for (StageLightBlockEntity e : floorCluster(lvl, worldPosition, data.group)) {
            minX = Math.min(minX, e.worldPosition.getX());
            maxX = Math.max(maxX, e.worldPosition.getX());
            minZ = Math.min(minZ, e.worldPosition.getZ());
            maxZ = Math.max(maxZ, e.worldPosition.getZ());
        }
        floorCx = (minX + maxX + 1) / 2f;
        floorCz = (minZ + maxZ + 1) / 2f;
    }

    /** Servidor: una baldosa nueva copia el estado de una vecina (asi se une a la pista). */
    public void joinNeighbors() {
        if (level == null || level.isClientSide || !type().isFloor()) {
            return;
        }
        for (net.minecraft.core.Direction dir : new net.minecraft.core.Direction[]{net.minecraft.core.Direction.NORTH,
                net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.WEST}) {
            if (level.getBlockEntity(worldPosition.relative(dir)) instanceof StageLightBlockEntity n && n.type().isFloor()) {
                applyAndSync(n.data);
                return;
            }
        }
    }

    public FixtureType type() {
        return getBlockState().getBlock() instanceof StageLightBlock b ? b.fixtureType() : FixtureType.SPOT;
    }

    /** Se llama cada tick en cliente y servidor para humo, fuego y fuegos artificiales. */
    public void tick(Level lvl) {
        FixtureType type = type();
        if (!data.on || data.intensity <= 0) {
            return;
        }
        if (lvl.isClientSide) {
            if (type == FixtureType.SMOKE) {
                Effects.smoke(lvl, worldPosition, getBlockState().getValue(StageLightBlock.FACING), data);
            } else if (type == FixtureType.FIRE) {
                Effects.fire(lvl, worldPosition, getBlockState().getValue(StageLightBlock.FACING), data);
            }
        } else if (type == FixtureType.FIREWORK && StageLiteConfig.FIREWORKS_ENABLED.get()) {
            if (--fireworkCooldown <= 0) {
                int interval = Math.max(20, 220 - data.speed * 20);
                fireworkCooldown = interval;
                Effects.launchFirework(lvl, worldPosition, getBlockState().getValue(StageLightBlock.FACING), data);
            }
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
        FixtureType t = type();
        BlockState wanted = state
                .setValue(StageLightBlock.LIGHT, t.lightLevel(data))
                .setValue(StageLightBlock.TILTED, t.bodyTilted(data));
        if (wanted != state) {
            level.setBlock(worldPosition, wanted, 3);
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
        floorTime = Long.MIN_VALUE;
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
            if (type().isFloor()) {
                renderBox = new AABB(worldPosition).expandTowards(0, 1, 0);
                return renderBox;
            }
            renderBox = new AABB(worldPosition).inflate(LightData.MAX_LENGTH + 2);
        }
        return renderBox;
    }
}
