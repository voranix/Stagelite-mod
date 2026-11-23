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
                    .icon(() -> new ItemStack(ModBlocks.LIGHT_CONSOLE_ITEM.get()))
                    .displayItems((params, output) ->
                    {
                        output.accept(ModBlocks.LIGHT_CONSOLE_ITEM.get());
                        output.accept(ModBlocks.MOLINETE_ITEM.get());
                        output.accept(ModBlocks.VIDEO_REMOTE.get());
                        ModBlocks.lightItems().forEach(item -> output.accept(item.get()));
                        output.accept(ModBlocks.TRUSS_ITEM.get());
                        output.accept(ModBlocks.STAGE_PLATFORM_ITEM.get());
                        output.accept(ModBlocks.PROJECTOR_STAND_ITEM.get());
                        output.accept(ModBlocks.MIC_STAND_ITEM.get());
                        int[] bandColors = {0xFFFFFF, 0xFF2D95, 0x39FF14, 0xFFE600, 0x00B7FF, 0xB026FF, 0xFF6A00};
                        for (int c : bandColors) {
                            ItemStack st = new ItemStack(ModBlocks.WRISTBAND.get());
                            ((net.minecraft.world.item.DyeableLeatherItem) ModBlocks.WRISTBAND.get()).setColor(st, c);
                            output.accept(st);
                        }
                        output.accept(ModBlocks.CREDENTIAL_VIP.get());
                        output.accept(ModBlocks.CREDENTIAL_STAFF.get());
                        output.accept(ModBlocks.CREDENTIAL_BACKSTAGE.get());
                    })
                    .build());

    private ModTabs() {
    }
}
