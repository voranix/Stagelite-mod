package com.stagelite.network;

import com.stagelite.client.HologramImages;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Trozo de una imagen local. Cliente -> servidor: subida de la imagen elegida por el staff.
 * Servidor -> cliente: respuesta a una peticion (total = 0 significa "no existe").
 */
public class ImageChunkPacket {
    public final String hash;
    public final int index;
    public final int total;
    public final byte[] data;

    public ImageChunkPacket(String hash, int index, int total, byte[] data) {
        this.hash = hash;
        this.index = index;
        this.total = total;
        this.data = data;
    }

    public static void encode(ImageChunkPacket m, FriendlyByteBuf buf) {
        buf.writeUtf(m.hash, 32);
        buf.writeVarInt(m.index);
        buf.writeVarInt(m.total);
        buf.writeByteArray(m.data);
    }

    public static ImageChunkPacket decode(FriendlyByteBuf buf) {
        return new ImageChunkPacket(buf.readUtf(32), buf.readVarInt(), buf.readVarInt(),
                buf.readByteArray(ImageIds.CHUNK + 64));
    }

    public static void handle(ImageChunkPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> HologramImages.onChunk(m.hash, m.index, m.total, m.data));
            } else {
                ServerPlayer sender = ctx.get().getSender();
                if (sender != null) {
                    ImageStore.onUpload(sender, m.hash, m.index, m.total, m.data);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
