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
    public static final RegistryObject<Block> DANCE_FLOOR = registerLight(FixtureType.FLOOR);
    public static final RegistryObject<Block> SPEAKER = registerLight(FixtureType.SPEAKER);
    public static final RegistryObject<Block> SPEAKER_MEDIUM = registerLight(FixtureType.SPEAKER_M, true);
    public static final RegistryObject<Block> SPEAKER_LARGE = registerLight(FixtureType.SPEAKER_L, true);
    public static final RegistryObject<Block> SPEAKER_PART = BLOCKS.register("speaker_part", com.stagelite.block.SpeakerPartBlock::new);
    public static final RegistryObject<Block> MICROPHONE = registerLight(FixtureType.MIC);
    public static final RegistryObject<Block> VIDEO_SCREEN = registerLight(FixtureType.SCREEN);
    public static final RegistryObject<Block> VIDEO_FRAME = registerLight(FixtureType.FRAME);
    public static final RegistryObject<Block> VIDEO_PROJECTOR = registerLight(FixtureType.PROJECTOR);
    public static final RegistryObject<Block> VIDEO_WALL = registerLight(FixtureType.WALL);
    public static final RegistryObject<Block> VIDEO_TV = registerLight(FixtureType.TV);

    public static final RegistryObject<Block> MOLINETE = BLOCKS.register("molinete", MolineteBlock::new);
    public static final RegistryObject<Item> MOLINETE_ITEM = ITEMS.register("molinete",
            () -> new MolineteItem(MOLINETE.get(), new Item.Properties()));

    // ---- Decoracion de escenario
    public static final RegistryObject<Block> TRUSS = BLOCKS.register("truss", com.stagelite.block.TrussBlock::new);
    public static final RegistryObject<Block> STAGE_PLATFORM = BLOCKS.register("stage_platform", () ->
            new Block(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_BLACK).strength(2.0F, 6.0F)
                    .sound(net.minecraft.world.level.block.SoundType.WOOD)));
    public static final RegistryObject<Block> PROJECTOR_STAND = BLOCKS.register("projector_stand", () ->
            new com.stagelite.block.ShapedBlock(com.stagelite.block.ShapedBlock.boxes(
                    new double[]{3, 0, 3, 13, 1, 13}, new double[]{7, 1, 7, 9, 12, 9}, new double[]{2, 12, 2, 14, 14, 14})));
    public static final RegistryObject<Block> MIC_STAND = BLOCKS.register("mic_stand", com.stagelite.block.MicStandBlock::new);
    public static final RegistryObject<Item> TRUSS_ITEM = ITEMS.register("truss", () -> new BlockItem(TRUSS.get(), new Item.Properties()));
    public static final RegistryObject<Item> STAGE_PLATFORM_ITEM = ITEMS.register("stage_platform", () -> new BlockItem(STAGE_PLATFORM.get(), new Item.Properties()));
    public static final RegistryObject<Item> PROJECTOR_STAND_ITEM = ITEMS.register("projector_stand", () -> new BlockItem(PROJECTOR_STAND.get(), new Item.Properties()));
    public static final RegistryObject<Item> MIC_STAND_ITEM = ITEMS.register("mic_stand", () -> new BlockItem(MIC_STAND.get(), new Item.Properties()));

    // ---- Accesos
    public static final RegistryObject<Item> WRISTBAND = ITEMS.register("wristband", () -> new com.stagelite.item.WristbandItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> CREDENTIAL_VIP = ITEMS.register("credential_vip", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CREDENTIAL_STAFF = ITEMS.register("credential_staff", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CREDENTIAL_BACKSTAGE = ITEMS.register("credential_backstage", () -> new Item(new Item.Properties().stacksTo(1)));

    // ---- Cine
    public static final RegistryObject<Block> CINEMA_SEAT = BLOCKS.register("cinema_seat", com.stagelite.block.CinemaSeatBlock::new);
    public static final RegistryObject<Item> CINEMA_SEAT_ITEM = ITEMS.register("cinema_seat", () -> new BlockItem(CINEMA_SEAT.get(), new Item.Properties()));
    public static final RegistryObject<Item> POPCORN = ITEMS.register("popcorn", () -> new Item(new Item.Properties().stacksTo(16)
            .food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationMod(0.3F).build())));
    public static final RegistryObject<Item> NACHOS = ITEMS.register("nachos", () -> new Item(new Item.Properties().stacksTo(16)
            .food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(6).saturationMod(0.5F).build())));
    public static final RegistryObject<Item> EMPTY_CUP = ITEMS.register("empty_cup", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DRINK_COLA = drink("drink_cola");
    public static final RegistryObject<Item> DRINK_ORANGE = drink("drink_orange");
    public static final RegistryObject<Item> DRINK_LIME = drink("drink_lime");

    private static RegistryObject<Item> drink(String id) {
        return ITEMS.register(id, () -> new com.stagelite.item.DrinkItem(new Item.Properties().stacksTo(16)
                .food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationMod(0.2F).alwaysEat().build())));
    }

    public static final RegistryObject<Item> VIDEO_REMOTE = ITEMS.register("video_remote",
            () -> new com.stagelite.item.VideoRemoteItem(new Item.Properties()));

    public static final RegistryObject<Block> LIGHT_CONSOLE = BLOCKS.register("light_console", LightConsoleBlock::new);
    public static final RegistryObject<Item> LIGHT_CONSOLE_ITEM = ITEMS.register("light_console",
            () -> new BlockItem(LIGHT_CONSOLE.get(), new Item.Properties()));

    private ModBlocks() {
    }

    private static RegistryObject<Block> registerLight(FixtureType type) {
        return registerLight(type, false);
    }

    private static RegistryObject<Block> registerLight(FixtureType type, boolean multi) {
        RegistryObject<Block> block = BLOCKS.register(type.id(), () -> new StageLightBlock(type));
        RegistryObject<Item> item = ITEMS.register(type.id(), () -> multi
                ? new com.stagelite.block.MultiSpeakerItem((StageLightBlock) block.get(), new Item.Properties())
                : new BlockItem(block.get(), new Item.Properties()));
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
