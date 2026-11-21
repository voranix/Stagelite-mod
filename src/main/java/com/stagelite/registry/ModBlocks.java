package com.stagelite.registry;

import com.stagelite.StageLite;
import com.stagelite.block.LightConsoleBlock;
import com.stagelite.block.MolineteBlock;
import com.stagelite.block.MolineteItem;
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
    public static final RegistryObject<Block> LASER = registerLight(FixtureType.LASER);
    public static final RegistryObject<Block> SMOKE_MACHINE = registerLight(FixtureType.SMOKE);
    public static final RegistryObject<Block> FIRE_MACHINE = registerLight(FixtureType.FIRE);
    public static final RegistryObject<Block> FIREWORK_LAUNCHER = registerLight(FixtureType.FIREWORK);
    public static final RegistryObject<Block> DISCO_BALL = registerLight(FixtureType.DISCO);
    public static final RegistryObject<Block> HOLOGRAM = registerLight(FixtureType.HOLOGRAM);

    public static final RegistryObject<Block> MOLINETE = BLOCKS.register("molinete", MolineteBlock::new);
    public static final RegistryObject<Item> MOLINETE_ITEM = ITEMS.register("molinete",
            () -> new MolineteItem(MOLINETE.get(), new Item.Properties()));

    public static final RegistryObject<Block> LIGHT_CONSOLE = BLOCKS.register("light_console", LightConsoleBlock::new);
    public static final RegistryObject<Item> LIGHT_CONSOLE_ITEM = ITEMS.register("light_console",
            () -> new BlockItem(LIGHT_CONSOLE.get(), new Item.Properties()));

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
