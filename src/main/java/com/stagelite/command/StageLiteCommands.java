package com.stagelite.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.stagelite.StageLite;
import com.stagelite.blockentity.StageLightBlockEntity;
import com.stagelite.config.StageLiteConfig;
import com.stagelite.data.ShowPlayer;
import com.stagelite.data.StageData;
import com.stagelite.light.Effect;
import com.stagelite.light.LightData;
import com.stagelite.light.LightRegistry;
import com.stagelite.light.Movement;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * /stagelite ... (solo staff)
 *
 *   list | blackout | on
 *   group <n> on|off|color <nombre|hex>|rgb <r> <g> <b>|intensity <0-100>|effect <x>|move <x>|speed <1-10>
 *   scene save|load|delete <nombre> | scene list
 *   show create|delete|play|loop <nombre> | show add <show> <escena> <segundos> | show stop | show list
 */
@Mod.EventBusSubscriber(modid = StageLite.MODID)
public final class StageLiteCommands {
    private static final Map<String, Integer> NAMED_COLORS = new LinkedHashMap<>();

    static {
        NAMED_COLORS.put("white", 0xFFFFFF);
        NAMED_COLORS.put("red", 0xFF0000);
        NAMED_COLORS.put("orange", 0xFF8000);
        NAMED_COLORS.put("yellow", 0xFFFF00);
        NAMED_COLORS.put("green", 0x00FF00);
        NAMED_COLORS.put("cyan", 0x00FFFF);
        NAMED_COLORS.put("blue", 0x0040FF);
        NAMED_COLORS.put("purple", 0x8000FF);
        NAMED_COLORS.put("magenta", 0xFF00FF);
        NAMED_COLORS.put("pink", 0xFF60B0);
    }

    private static final SimpleCommandExceptionType BAD_COLOR =
            new SimpleCommandExceptionType(Component.translatable("commands.stagelite.bad_color"));
    private static final SimpleCommandExceptionType BAD_VALUE =
            new SimpleCommandExceptionType(Component.translatable("commands.stagelite.bad_value"));

    private static final SuggestionProvider<CommandSourceStack> COLOR_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(NAMED_COLORS.keySet(), builder);
    private static final SuggestionProvider<CommandSourceStack> EFFECT_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    Arrays.stream(Effect.values()).map(Effect::id).toList(), builder);
    private static final SuggestionProvider<CommandSourceStack> MOVE_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    Arrays.stream(Movement.values()).map(Movement::id).toList(), builder);
    private static final SuggestionProvider<CommandSourceStack> SCENE_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    StageData.get(ctx.getSource().getServer()).scenes.keySet(), builder);
    private static final SuggestionProvider<CommandSourceStack> SHOW_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    StageData.get(ctx.getSource().getServer()).shows.keySet(), builder);

    private StageLiteCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("stagelite")
                .requires(src -> src.hasPermission(StageLiteConfig.PERMISSION_LEVEL.get()))
                .then(Commands.literal("list").executes(ctx -> listLights(ctx.getSource())))
                .then(Commands.literal("blackout").executes(ctx -> setAll(ctx.getSource(), false)))
                .then(Commands.literal("on").executes(ctx -> setAll(ctx.getSource(), true)))
                .then(groupBranch())
                .then(sceneBranch())
                .then(showBranch()));
    }

    // ------------------------------------------------------------------ group

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> groupBranch() {
        return Commands.literal("group")
                .then(Commands.argument("n", IntegerArgumentType.integer(1, 99))
                        .then(Commands.literal("on")
                                .executes(ctx -> editGroup(ctx, d -> d.on = true)))
                        .then(Commands.literal("off")
                                .executes(ctx -> editGroup(ctx, d -> d.on = false)))
                        .then(Commands.literal("color")
                                .then(Commands.argument("color", StringArgumentType.word())
                                        .suggests(COLOR_SUGGESTIONS)
                                        .executes(ctx -> {
                                            int rgb = parseColor(StringArgumentType.getString(ctx, "color"));
                                            return editGroup(ctx, d -> d.color = rgb);
                                        })))
                        .then(Commands.literal("rgb")
                                .then(Commands.argument("r", IntegerArgumentType.integer(0, 255))
                                        .then(Commands.argument("g", IntegerArgumentType.integer(0, 255))
                                                .then(Commands.argument("b", IntegerArgumentType.integer(0, 255))
                                                        .executes(ctx -> {
                                                            int rgb = (IntegerArgumentType.getInteger(ctx, "r") << 16)
                                                                    | (IntegerArgumentType.getInteger(ctx, "g") << 8)
                                                                    | IntegerArgumentType.getInteger(ctx, "b");
                                                            return editGroup(ctx, d -> d.color = rgb);
                                                        })))))
                        .then(Commands.literal("intensity")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                        .executes(ctx -> {
                                            int v = IntegerArgumentType.getInteger(ctx, "value");
                                            return editGroup(ctx, d -> d.intensity = v);
                                        })))
                        .then(Commands.literal("effect")
                                .then(Commands.argument("effect", StringArgumentType.word())
                                        .suggests(EFFECT_SUGGESTIONS)
                                        .executes(ctx -> {
                                            Effect e = Effect.byName(StringArgumentType.getString(ctx, "effect"));
                                            if (e == null) {
                                                throw BAD_VALUE.create();
                                            }
                                            return editGroup(ctx, d -> d.effect = e);
                                        })))
                        .then(Commands.literal("move")
                                .then(Commands.argument("movement", StringArgumentType.word())
                                        .suggests(MOVE_SUGGESTIONS)
                                        .executes(ctx -> {
                                            Movement m = Movement.byName(StringArgumentType.getString(ctx, "movement"));
                                            if (m == null) {
                                                throw BAD_VALUE.create();
                                            }
                                            return editGroup(ctx, d -> d.movement = m);
                                        })))
                        .then(Commands.literal("speed")
                                .then(Commands.argument("value", IntegerArgumentType.integer(1, 10))
                                        .executes(ctx -> {
                                            int v = IntegerArgumentType.getInteger(ctx, "value");
                                            return editGroup(ctx, d -> d.speed = v);
                                        }))));
    }

    private static int editGroup(CommandContext<CommandSourceStack> ctx, Consumer<LightData> edit) {
        int group = IntegerArgumentType.getInteger(ctx, "n");
        int count = 0;
        for (StageLightBlockEntity be : LightRegistry.all()) {
            if (be.data.group == group) {
                edit.accept(be.data);
                be.data.clamp();
                be.sync();
                count++;
            }
        }
        if (count == 0) {
            ctx.getSource().sendFailure(Component.translatable("commands.stagelite.group_empty", group));
            return 0;
        }
        final int changed = count;
        ctx.getSource().sendSuccess(
                () -> Component.translatable("commands.stagelite.group_updated", changed, group), true);
        return count;
    }

    private static int parseColor(String text) throws CommandSyntaxException {
        String s = text.toLowerCase(Locale.ROOT);
        Integer named = NAMED_COLORS.get(s);
        if (named != null) {
            return named;
        }
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.length() != 6) {
            throw BAD_COLOR.create();
        }
        try {
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            throw BAD_COLOR.create();
        }
    }

    // ------------------------------------------------------------------ global

    private static int setAll(CommandSourceStack src, boolean on) {
        int count = 0;
        for (StageLightBlockEntity be : LightRegistry.all()) {
            be.data.on = on;
            be.sync();
            count++;
        }
        final int changed = count;
        src.sendSuccess(() -> Component.translatable(
                on ? "commands.stagelite.all_on" : "commands.stagelite.all_off", changed), true);
        return count;
    }

    private static int listLights(CommandSourceStack src) {
        Map<Integer, Integer> perGroup = new TreeMap<>();
        int total = 0;
        for (StageLightBlockEntity be : LightRegistry.all()) {
            perGroup.merge(be.data.group, 1, Integer::sum);
            total++;
        }
        final int all = total;
        src.sendSuccess(() -> Component.translatable("commands.stagelite.list_header", all), false);
        for (Map.Entry<Integer, Integer> e : perGroup.entrySet()) {
            final int g = e.getKey();
            final int c = e.getValue();
            src.sendSuccess(() -> g == 0
                    ? Component.translatable("commands.stagelite.list_ungrouped", c)
                    : Component.translatable("commands.stagelite.list_group", g, c), false);
        }
        return total;
    }

    // ------------------------------------------------------------------ scenes

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> sceneBranch() {
        return Commands.literal("scene")
                .then(Commands.literal("save")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(StageLiteCommands::saveScene)))
                .then(Commands.literal("load")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(SCENE_SUGGESTIONS)
                                .executes(StageLiteCommands::loadScene)))
                .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(SCENE_SUGGESTIONS)
                                .executes(StageLiteCommands::deleteScene)))
                .then(Commands.literal("list").executes(ctx -> {
                    StageData data = StageData.get(ctx.getSource().getServer());
                    String names = String.join(", ", data.scenes.keySet());
                    ctx.getSource().sendSuccess(() -> Component.translatable(
                            "commands.stagelite.scene_list", data.scenes.size(), names), false);
                    return data.scenes.size();
                }));
    }

    private static int saveScene(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        Map<Integer, LightData> looks = new TreeMap<>();
        for (StageLightBlockEntity be : LightRegistry.all()) {
            int g = be.data.group;
            if (g > 0 && !looks.containsKey(g)) {
                LightData copy = new LightData();
                copy.copyFrom(be.data);
                looks.put(g, copy);
            }
        }
        if (looks.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("commands.stagelite.scene_nothing"));
            return 0;
        }
        StageData data = StageData.get(ctx.getSource().getServer());
        data.scenes.put(name, looks);
        data.setDirty();
        final int groups = looks.size();
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.stagelite.scene_saved", name, groups), true);
        return groups;
    }

    private static int loadScene(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        Map<Integer, LightData> looks = StageData.get(ctx.getSource().getServer()).scenes.get(name);
        if (looks == null) {
            ctx.getSource().sendFailure(Component.translatable("commands.stagelite.scene_unknown", name));
            return 0;
        }
        final int changed = ShowPlayer.applyScene(looks);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.stagelite.scene_loaded", name, changed), true);
        return changed;
    }

    private static int deleteScene(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        StageData data = StageData.get(ctx.getSource().getServer());
        if (data.scenes.remove(name) == null) {
            ctx.getSource().sendFailure(Component.translatable("commands.stagelite.scene_unknown", name));
            return 0;
        }
        data.setDirty();
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.stagelite.scene_deleted", name), true);
        return 1;
    }

    // ------------------------------------------------------------------ shows

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> showBranch() {
        return Commands.literal("show")
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(StageLiteCommands::createShow)))
                .then(Commands.literal("add")
                        .then(Commands.argument("show", StringArgumentType.word())
                                .suggests(SHOW_SUGGESTIONS)
                                .then(Commands.argument("scene", StringArgumentType.word())
                                        .suggests(SCENE_SUGGESTIONS)
                                        .then(Commands.argument("seconds", DoubleArgumentType.doubleArg(0.1, 3600))
                                                .executes(StageLiteCommands::addStep)))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(SHOW_SUGGESTIONS)
                                .executes(StageLiteCommands::deleteShow)))
                .then(Commands.literal("play")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(SHOW_SUGGESTIONS)
                                .executes(ctx -> playShow(ctx, false))))
                .then(Commands.literal("loop")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(SHOW_SUGGESTIONS)
                                .executes(ctx -> playShow(ctx, true))))
                .then(Commands.literal("stop").executes(ctx -> {
                    ShowPlayer.stop();
                    ctx.getSource().sendSuccess(() -> Component.translatable("commands.stagelite.show_stopped"), true);
                    return 1;
                }))
                .then(Commands.literal("list").executes(StageLiteCommands::listShows));
    }

    private static int createShow(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        StageData data = StageData.get(ctx.getSource().getServer());
        if (data.shows.containsKey(name)) {
            ctx.getSource().sendFailure(Component.translatable("commands.stagelite.show_exists", name));
            return 0;
        }
        data.shows.put(name, new ArrayList<>());
        data.setDirty();
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.stagelite.show_created", name), true);
        return 1;
    }

    private static int addStep(CommandContext<CommandSourceStack> ctx) {
        String show = StringArgumentType.getString(ctx, "show");
        String scene = StringArgumentType.getString(ctx, "scene");
        double seconds = DoubleArgumentType.getDouble(ctx, "seconds");
        StageData data = StageData.get(ctx.getSource().getServer());
        List<StageData.ShowStep> steps = data.shows.get(show);
        if (steps == null) {
            ctx.getSource().sendFailure(Component.translatable("commands.stagelite.show_unknown", show));
            return 0;
        }
        if (!data.scenes.containsKey(scene)) {
            ctx.getSource().sendFailure(Component.translatable("commands.stagelite.scene_unknown", scene));
            return 0;
        }
        steps.add(new StageData.ShowStep(scene, seconds));
        data.setDirty();
        final int size = steps.size();
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.stagelite.show_step_added", scene, seconds, show, size), true);
        return size;
    }

    private static int deleteShow(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        StageData data = StageData.get(ctx.getSource().getServer());
        if (data.shows.remove(name) == null) {
            ctx.getSource().sendFailure(Component.translatable("commands.stagelite.show_unknown", name));
            return 0;
        }
        data.setDirty();
        if (name.equals(ShowPlayer.currentName())) {
            ShowPlayer.stop();
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.stagelite.show_deleted", name), true);
        return 1;
    }

    private static int playShow(CommandContext<CommandSourceStack> ctx, boolean loop) {
        String name = StringArgumentType.getString(ctx, "name");
        if (!ShowPlayer.play(ctx.getSource().getServer(), name, loop)) {
            ctx.getSource().sendFailure(Component.translatable("commands.stagelite.show_cannot_play", name));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable(
                loop ? "commands.stagelite.show_looping" : "commands.stagelite.show_playing", name), true);
        return 1;
    }

    private static int listShows(CommandContext<CommandSourceStack> ctx) {
        StageData data = StageData.get(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.stagelite.show_list_header", data.shows.size()), false);
        for (Map.Entry<String, List<StageData.ShowStep>> e : data.shows.entrySet()) {
            StringBuilder sb = new StringBuilder();
            for (StageData.ShowStep step : e.getValue()) {
                if (sb.length() > 0) {
                    sb.append(" > ");
                }
                sb.append(step.scene()).append(" (").append(step.seconds()).append("s)");
            }
            final String line = "  " + e.getKey() + ": " + (sb.length() == 0 ? "-" : sb);
            ctx.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return data.shows.size();
    }
}
