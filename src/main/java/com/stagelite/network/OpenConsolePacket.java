package com.stagelite.network;

import com.stagelite.client.ClientHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: abre (o actualiza) la consola. */
public class OpenConsolePacket {
    private final BlockPos pos;
    private final ConsoleSnapshot snapshot;

    public OpenConsolePacket(BlockPos pos, ConsoleSnapshot snapshot) {
        this.pos = pos;
        this.snapshot = snapshot;
    }

    public static void encode(OpenConsolePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        msg.snapshot.write(buf);
    }

    public static OpenConsolePacket decode(FriendlyByteBuf buf) {
        return new OpenConsolePacket(buf.readBlockPos(), ConsoleSnapshot.read(buf));
    }

    public static void handle(OpenConsolePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> ClientHooks.openConsole(msg.pos, msg.snapshot)));
        ctx.get().setPacketHandled(true);
    }
}
