package com.stagelite.network;

import com.stagelite.StageLite;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "2";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(StageLite.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private ModNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, OpenLightScreenPacket.class,
                OpenLightScreenPacket::encode, OpenLightScreenPacket::decode, OpenLightScreenPacket::handle);
        CHANNEL.registerMessage(id++, OpenConsolePacket.class,
                OpenConsolePacket::encode, OpenConsolePacket::decode, OpenConsolePacket::handle);
        CHANNEL.registerMessage(id++, ConsoleActionPacket.class,
                ConsoleActionPacket::encode, ConsoleActionPacket::decode, ConsoleActionPacket::handle);
        CHANNEL.registerMessage(id++, UpdateLightPacket.class,
                UpdateLightPacket::encode, UpdateLightPacket::decode, UpdateLightPacket::handle);
    }
}
