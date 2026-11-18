package com.stagelite.registry;

import com.stagelite.StageLite;
import com.stagelite.block.StageLightBlock;
import com.stagelite.light.FixtureType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, StageLite.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, StageLite.MODID);

    private static final List<RegistryObject<Block>> LIGHT_BLOCKS = new ArrayList<>();
    private static final List<RegistryObject<Item>> LIGHT_ITEMS = new ArrayList<>();

    public static final RegistryObject<Block> SPOT_LIGHT = registerLight(FixtureType.SPOT);
    public static final RegistryObject<Block> MOVING_HEAD = registerLight(FixtureType.MOVING_HEAD);
    public static final RegistryObject<Block> LED_PANEL = registerLight(FixtureType.LED_PANEL);
    public static final RegistryObject<Block> STROBE_LIGHT = registerLight(FixtureType.STROBE);

    private ModBlocks() {
    }

    private static RegistryObject<Block> registerLight(FixtureType type) {
        RegistryObject<Block> block = BLOCKS.register(type.id(), () -> new StageLightBlock(type));
        RegistryObject<Item> item = ITEMS.register(type.id(), () -> new BlockItem(block.get(), new Item.Properties()));
        LIGHT_BLOCKS.add(block);
        LIGHT_ITEMS.add(item);
        return block;
    }

    /** Todos los bloques de luz (para el block entity). */
    public static Block[] lightBlocks() {
        return LIGHT_BLOCKS.stream().map(RegistryObject::get).toArray(Block[]::new);
    }

    public static List<RegistryObject<Item>> lightItems() {
        return LIGHT_ITEMS;
    }
}
