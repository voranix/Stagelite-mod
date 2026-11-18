package com.stagelite.client.screen;

import com.stagelite.light.Effect;
import com.stagelite.light.FixtureType;
import com.stagelite.light.LightData;
import com.stagelite.light.Movement;
import com.stagelite.network.ConsoleActionPacket;
import com.stagelite.network.ConsoleSnapshot;
import com.stagelite.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consola: un canal por cada grupo de luces (los mismos grupos de /stagelite).
 * Por canal: encender/apagar, color rapido, brillo, velocidad, efecto y movimiento.
 * Abajo: apagon, escenas y shows.
 */
public class ConsoleScreen extends Screen {
    private static final int[] PRESETS = {
            0xFFFFFF, 0xFF0000, 0xFF8000, 0xFFFF00, 0x00FF00, 0x00FFFF, 0x0040FF, 0x8000FF, 0xFF00FF, 0xFF60B0
    };
    private static final int STRIP_W = 64;
    private static final int GAP = 6;
    private static final int FADER_H = 80;

    private final BlockPos pos;
    private ConsoleSnapshot snap;

    private int page;
    private int sceneIdx;
    private int showIdx;
    private String nameText = "";
    private EditBox nameBox;

    private int left;
    private int top;
    private int panelW;
    private int perPage;
    private int stripsX;
    private int shown;

    private final Map<Integer, int[]> pending = new LinkedHashMap<>();
    private int ticks;
    private int lastSend;

    public ConsoleScreen(BlockPos pos, ConsoleSnapshot snap) {
        super(Component.translatable("block.stagelite.light_console"));
        this.pos = pos;
        this.snap = snap;
    }

    public BlockPos pos() {
        return pos;
    }

    /** Llega una foto nueva del servidor (por ejemplo tras guardar una escena). */
    public void update(ConsoleSnapshot newSnap) {
        this.snap = newSnap;
        this.rebuildWidgets();
    }

    @Override
    protected void init() {
        int total = snap.groups.size();
        perPage = Math.max(1, Math.min(10, (this.width - 40) / (STRIP_W + GAP)));
        page = Mth.clamp(page, 0, Math.max(0, (total - 1) / perPage));
        shown = Math.max(0, Math.min(perPage, total - page * perPage));
        panelW = Math.max(shown * (STRIP_W + GAP) - GAP, 380);
        int totalH = 12 + 20 + 49 + FADER_H + 2 + 36 + 6 + 50;
        left = (this.width - panelW) / 2;
        top = Math.max(18, (this.height - totalH) / 2);
        stripsX = left + (panelW - Math.max(0, shown * (STRIP_W + GAP) - GAP)) / 2;

        int yOn = top + 12;
        int ySw = yOn + 20;
        int yF = ySw + 49;
        int yEff = yF + FADER_H + 2;
        int yMov = yEff + 18;

        for (int i = 0; i < shown; i++) {
            final ConsoleSnapshot.Group g = snap.groups.get(page * perPage + i);
            final int x = stripsX + i * (STRIP_W + GAP);
            final int gn = g.number;

            addRenderableWidget(Button.builder(onLabel(g), btn -> {
                        g.look.on = !g.look.on;
                        btn.setMessage(onLabel(g));
                        send(ConsoleActionPacket.GROUP_ON, gn, g.look.on ? 1 : 0, "");
                    }).bounds(x, yOn, STRIP_W, 16)
                    .tooltip(Tooltip.create(Component.translatable("tip.stagelite.power"))).build());

            for (int c = 0; c < PRESETS.length; c++) {
                final int color = PRESETS[c];
                SwatchWidget sw = new SwatchWidget(x + (c % 2) * (STRIP_W / 2), ySw + (c / 2) * 9, STRIP_W / 2 - 1, 8, color, () -> {
                    g.look.color = color;
                    send(ConsoleActionPacket.GROUP_COLOR, gn, color, "");
                });
                addRenderableWidget(sw);
            }

            FaderWidget bright = new FaderWidget(x, yF, 30, FADER_H, Component.translatable("fader.stagelite.intensity"),
                    0, 100, g.look.intensity, 0xFFD060,
                    v -> {
                        g.look.intensity = v;
                        pending.put(gn * 100 + ConsoleActionPacket.GROUP_INTENSITY, new int[]{ConsoleActionPacket.GROUP_INTENSITY, gn, v});
                    }, () -> { }, this::flush);
            bright.setTooltip(Tooltip.create(Component.translatable("tip.stagelite.intensity")));
            addRenderableWidget(bright);

            FaderWidget speed = new FaderWidget(x + 32, yF, 30, FADER_H, Component.translatable("fader.stagelite.speed"),
                    1, 10, g.look.speed, 0xA070FF,
                    v -> {
                        g.look.speed = v;
                        pending.put(gn * 100 + ConsoleActionPacket.GROUP_SPEED, new int[]{ConsoleActionPacket.GROUP_SPEED, gn, v});
                    }, () -> { }, this::flush);
            speed.setTooltip(Tooltip.create(Component.translatable("tip.stagelite.speed")));
            addRenderableWidget(speed);

            addRenderableWidget(Button.builder(Component.translatable(g.look.effect.key()), btn -> {
                        g.look.effect = g.look.effect.next();
                        btn.setMessage(Component.translatable(g.look.effect.key()));
                        send(ConsoleActionPacket.GROUP_EFFECT, gn, g.look.effect.ordinal(), "");
                    }).bounds(x, yEff, STRIP_W, 16)
                    .tooltip(Tooltip.create(Component.translatable("tip.stagelite.effect"))).build());

            addRenderableWidget(Button.builder(Component.translatable(g.look.movement.key()), btn -> {
                        g.look.movement = g.look.movement.next();
                        btn.setMessage(Component.translatable(g.look.movement.key()));
                        send(ConsoleActionPacket.GROUP_MOVE, gn, g.look.movement.ordinal(), "");
                    }).bounds(x, yMov, STRIP_W, 16)
                    .tooltip(Tooltip.create(Component.translatable("tip.stagelite.movement"))).build());
        }

        // Paginas de grupos
        if (total > perPage) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                page = Math.max(0, page - 1);
                rebuildWidgets();
            }).bounds(left + panelW - 44, top - 2, 20, 14).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                page = Math.min((total - 1) / perPage, page + 1);
                rebuildWidgets();
            }).bounds(left + panelW - 22, top - 2, 20, 14).build());
        }

        // Barra inferior
        int by = yMov + 24;
        addRenderableWidget(Button.builder(Component.translatable("gui.stagelite.console.blackout"),
                b -> {
                    for (ConsoleSnapshot.Group g : snap.groups) {
                        g.look.on = false;
                    }
                    send(ConsoleActionPacket.BLACKOUT, 0, 0, "");
                    rebuildWidgets();
                }).bounds(left, by, 90, 18)
                .tooltip(Tooltip.create(Component.translatable("tip.stagelite.blackout"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.stagelite.console.all_on"),
                b -> {
                    for (ConsoleSnapshot.Group g : snap.groups) {
                        g.look.on = true;
                    }
                    send(ConsoleActionPacket.ALL_ON, 0, 0, "");
                    rebuildWidgets();
                }).bounds(left + 94, by, 90, 18).build());

        // Escenas
        int sx = left + 192;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> { sceneIdx = cycle(sceneIdx, -1, snap.scenes.size()); rebuildWidgets(); })
                .bounds(sx, by, 16, 18).build());
        addRenderableWidget(Button.builder(Component.literal(sceneName()), b -> { })
                .bounds(sx + 18, by, 70, 18).tooltip(Tooltip.create(Component.translatable("tip.stagelite.scene"))).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> { sceneIdx = cycle(sceneIdx, 1, snap.scenes.size()); rebuildWidgets(); })
                .bounds(sx + 90, by, 16, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.stagelite.console.load"), b -> {
            if (!snap.scenes.isEmpty()) {
                send(ConsoleActionPacket.SCENE_LOAD, 0, 0, snap.scenes.get(Mth.clamp(sceneIdx, 0, snap.scenes.size() - 1)));
            }
        }).bounds(sx + 108, by, 40, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.stagelite.console.delete"), b -> {
            if (!snap.scenes.isEmpty()) {
                send(ConsoleActionPacket.SCENE_DELETE, 0, 0, snap.scenes.get(Mth.clamp(sceneIdx, 0, snap.scenes.size() - 1)));
            }
        }).bounds(sx + 150, by, 38, 18).build());

        // Guardar escena nueva + shows
        int by2 = by + 22;
        nameBox = new EditBox(this.font, left, by2, 120, 18, Component.translatable("gui.stagelite.console.name"));
        nameBox.setMaxLength(32);
        nameBox.setHint(Component.translatable("gui.stagelite.console.name_hint"));
        nameBox.setValue(nameText);
        nameBox.setResponder(t -> nameText = t);
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(Component.translatable("gui.stagelite.console.save"), b -> {
            if (!nameText.isBlank()) {
                send(ConsoleActionPacket.SCENE_SAVE, 0, 0, nameText.trim());
                nameText = "";
            }
        }).bounds(left + 124, by2, 60, 18)
                .tooltip(Tooltip.create(Component.translatable("tip.stagelite.save_scene"))).build());

        addRenderableWidget(Button.builder(Component.literal("<"), b -> { showIdx = cycle(showIdx, -1, snap.shows.size()); rebuildWidgets(); })
                .bounds(sx, by2, 16, 18).build());
        addRenderableWidget(Button.builder(Component.literal(showName()), b -> { })
                .bounds(sx + 18, by2, 70, 18).tooltip(Tooltip.create(Component.translatable("tip.stagelite.show"))).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> { showIdx = cycle(showIdx, 1, snap.shows.size()); rebuildWidgets(); })
                .bounds(sx + 90, by2, 16, 18).build());
        addRenderableWidget(Button.builder(Component.literal("▶"), b -> playShow(false))
                .bounds(sx + 108, by2, 24, 18).tooltip(Tooltip.create(Component.translatable("tip.stagelite.show_play"))).build());
        addRenderableWidget(Button.builder(Component.literal("↻"), b -> playShow(true))
                .bounds(sx + 134, by2, 24, 18).tooltip(Tooltip.create(Component.translatable("tip.stagelite.show_loop"))).build());
        addRenderableWidget(Button.builder(Component.literal("■"), b -> send(ConsoleActionPacket.SHOW_STOP, 0, 0, ""))
                .bounds(sx + 160, by2, 28, 18).tooltip(Tooltip.create(Component.translatable("tip.stagelite.show_stop"))).build());
    }

    private void playShow(boolean loop) {
        if (!snap.shows.isEmpty()) {
            send(ConsoleActionPacket.SHOW_PLAY, 0, loop ? 1 : 0, snap.shows.get(Mth.clamp(showIdx, 0, snap.shows.size() - 1)));
        }
    }

    private String sceneName() {
        return snap.scenes.isEmpty() ? "-" : snap.scenes.get(Mth.clamp(sceneIdx, 0, snap.scenes.size() - 1));
    }

    private String showName() {
        return snap.shows.isEmpty() ? "-" : snap.shows.get(Mth.clamp(showIdx, 0, snap.shows.size() - 1));
    }

    private int cycle(int idx, int delta, int size) {
        return size == 0 ? 0 : Math.floorMod(idx + delta, size);
    }

    private static Component onLabel(ConsoleSnapshot.Group g) {
        return Component.translatable(g.look.on ? "gui.stagelite.console.on" : "gui.stagelite.console.off")
                .withStyle(g.look.on ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private void send(int action, int group, int value, String name) {
        ModNetwork.CHANNEL.sendToServer(new ConsoleActionPacket(pos, action, group, value, name));
    }

    private void flush() {
        for (int[] p : pending.values()) {
            send(p[0], p[1], p[2], "");
        }
        pending.clear();
        lastSend = ticks;
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        if (!pending.isEmpty() && ticks - lastSend >= 3) {
            flush();
        }
    }

    @Override
    public void removed() {
        flush();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int bottom = top + 12 + 20 + 49 + FADER_H + 2 + 36 + 6 + 50;
        g.fill(left - 8, top - 24 + 16, left + panelW + 8, bottom, 0xD0101014);
        g.fill(left - 8, top - 8, left + panelW + 8, top - 7, 0xFF505058);
        g.drawString(this.font, this.title, left, top - 2, 0xFFFFFF, false);

        if (snap.groups.isEmpty()) {
            g.drawCenteredString(this.font, Component.translatable("gui.stagelite.console.empty"),
                    left + panelW / 2, top + 50, 0xB0B0B8);
        }
        if (!snap.playing.isEmpty()) {
            g.drawString(this.font, Component.translatable("gui.stagelite.console.playing", snap.playing),
                    left + 120, top - 2, 0x60E060, false);
        }

        super.render(g, mouseX, mouseY, partialTick);

        // Titulos de los canales (y lista de dispositivos al pasar el raton)
        for (int i = 0; i < shown; i++) {
            ConsoleSnapshot.Group grp = snap.groups.get(page * perPage + i);
            int x = stripsX + i * (STRIP_W + GAP);
            int total = 0;
            for (int c : grp.counts) {
                total += c;
            }
            g.drawString(this.font, "G" + grp.number + "  x" + total, x, top + 2, 0xFFFFFF, false);
            if (mouseX >= x && mouseX < x + STRIP_W && mouseY >= top && mouseY < top + 11) {
                List<Component> lines = new ArrayList<>();
                FixtureType[] types = FixtureType.values();
                for (int k = 0; k < types.length; k++) {
                    if (grp.counts[k] > 0) {
                        lines.add(Component.literal(grp.counts[k] + " x ").append(Component.translatable("block.stagelite." + types[k].id())));
                    }
                }
                g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
            }
        }
    }
}
