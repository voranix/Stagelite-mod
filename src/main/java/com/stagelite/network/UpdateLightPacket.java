package com.stagelite.network;

import com.stagelite.blockentity.StageLightBlockEntity;
import com.stagelite.config.StageLiteConfig;
import com.stagelite.light.LightData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: aplica los cambios hechos en el panel. El servidor valida todo. */
public class UpdateLightPacket {
    private final BlockPos pos;
    private final CompoundTag tag;

    public UpdateLightPacket(BlockPos pos, CompoundTag tag) {
        this.pos = pos;
        this.tag = tag;
    }

    public static void encode(UpdateLightPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeNbt(msg.tag);
    }

    public static UpdateLightPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        CompoundTag tag = buf.readNbt();
        return new UpdateLightPacket(pos, tag == null ? new CompoundTag() : tag);
    }

    public static void handle(UpdateLightPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || !StageLiteConfig.canEdit(sender)) {
                return;
            }
            ServerLevel level = sender.serverLevel();
            if (!level.isLoaded(msg.pos) || sender.distanceToSqr(Vec3.atCenterOf(msg.pos)) > 144.0) {
                return;
            }
            BlockEntity be = level.getBlockEntity(msg.pos);
            if (be instanceof StageLightBlockEntity light) {
                light.applyAndSync(LightData.fromTag(msg.tag));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
