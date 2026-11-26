package com.stagelite.registry;

import com.stagelite.StageLite;
import com.stagelite.entity.SeatEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, StageLite.MODID);

    public static final RegistryObject<EntityType<SeatEntity>> SEAT = ENTITIES.register("seat", () ->
            EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC)
                    .sized(0.0F, 0.0F).noSave().noSummon().fireImmune().clientTrackingRange(8).updateInterval(20)
                    .build("seat"));

    private ModEntities() {
    }
}
