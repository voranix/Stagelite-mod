package com.stagelite.client;

import com.stagelite.client.screen.StageLightScreen;
import com.stagelite.light.FixtureType;
import com.stagelite.light.LightData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/** Punto de entrada del lado cliente (solo se llama dentro de DistExecutor). */
public final class ClientHooks {
    private ClientHooks() {
    }

    public static void openLightScreen(BlockPos pos, int typeId, CompoundTag tag) {
        Minecraft.getInstance().setScreen(
                new StageLightScreen(pos, FixtureType.byId(typeId), LightData.fromTag(tag)));
    }
}
