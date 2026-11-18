package com.stagelite.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: "mandame la imagen local con este hash". */
public class ImageRequestPacket {
    private final String hash;

    public ImageRequestPacket(String hash) {
        this.hash = hash;
    }

    public static void encode(ImageRequestPacket m, FriendlyByteBuf buf) {
        buf.writeUtf(m.hash, 32);
    }

    public static ImageRequestPacket decode(FriendlyByteBuf buf) {
        return new ImageRequestPacket(buf.readUtf(32));
    }

    public static void handle(ImageRequestPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                ImageStore.onRequest(sender, m.hash);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
