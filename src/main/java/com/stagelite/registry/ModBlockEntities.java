package com.stagelite.registry;

import com.stagelite.StageLite;
import com.stagelite.blockentity.StageLightBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, StageLite.MODID);

    /** Un solo tipo de block entity para los 4 bloques de luz. */
    public static final RegistryObject<BlockEntityType<StageLightBlockEntity>> STAGE_LIGHT = TYPES.register("stage_light",
            () -> BlockEntityType.Builder.of(StageLightBlockEntity::new, ModBlocks.lightBlocks()).build(null));

    private ModBlockEntities() {
    }
}
