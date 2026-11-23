package com.stagelite.client;

import com.stagelite.StageLite;
import com.stagelite.client.render.StageLightRenderer;
import com.stagelite.registry.ModBlockEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = StageLite.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onItemColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> tint == 0 && stack.getItem() instanceof net.minecraft.world.item.DyeableLeatherItem d
                ? 0xFF000000 | d.getColor(stack) : -1,
                com.stagelite.registry.ModBlocks.WRISTBAND.get());
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.STAGE_LIGHT.get(), StageLightRenderer::new);
    }
}
