package com.stagelite.registry;

import com.stagelite.StageLite;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, StageLite.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.stagelite"))
                    .icon(() -> new ItemStack(ModBlocks.lightItems().get(0).get()))
                    .displayItems((params, output) ->
                            ModBlocks.lightItems().forEach(item -> output.accept(item.get())))
                    .build());

    private ModTabs() {
    }
}
