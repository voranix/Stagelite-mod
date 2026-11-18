package com.stagelite.network;

import com.stagelite.client.ClientHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: abre el panel de control de una luz. */
public class OpenLightScreenPacket {
    private final BlockPos pos;
    private final int type;
    private final CompoundTag tag;

    public OpenLightScreenPacket(BlockPos pos, int type, CompoundTag tag) {
        this.pos = pos;
        this.type = type;
        this.tag = tag;
    }

    public static void encode(OpenLightScreenPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeVarInt(msg.type);
        buf.writeNbt(msg.tag);
    }

    public static OpenLightScreenPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int type = buf.readVarInt();
        CompoundTag tag = buf.readNbt();
        return new OpenLightScreenPacket(pos, type, tag == null ? new CompoundTag() : tag);
    }

    public static void handle(OpenLightScreenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> ClientHooks.openLightScreen(msg.pos, msg.type, msg.tag)));
        ctx.get().setPacketHandled(true);
    }
}
