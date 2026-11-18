package com.stagelite.client.screen;

import com.stagelite.light.FixtureType;
import com.stagelite.light.LightData;
import com.stagelite.network.ModNetwork;
import com.stagelite.network.UpdateLightPacket;
import com.stagelite.client.HologramImages;
import com.stagelite.network.ImageChunkPacket;
import com.stagelite.network.ImageIds;
import net.minecraft.client.Minecraft;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.IntConsumer;

/**
 * Panel de control con faders verticales, como una mesa de luces.
 *
 *  - Arrastra un fader (o usa la rueda del raton; Shift = pasos de 5).
 *  - Los cuadritos de colores de arriba ponen un color al instante.
 *  - Los cambios se envian al servidor mientras arrastras, asi ves la luz cambiar en vivo.
 */
public class StageLightScreen extends Screen {
    private static final int[] PRESETS = {
            0xFFFFFF, 0xFF0000, 0xFF8000, 0xFFFF00, 0x00FF00, 0x00FFFF, 0x0040FF, 0x8000FF, 0xFF00FF, 0xFF60B0
    };
    private static final int PANEL_W = 470;
    private static final int FADER_PITCH = 30;
    private static final int FADER_H = 96;

    private final BlockPos pos;
    private final FixtureType type;
    private final LightData data;

    private FaderWidget faderR;
    private FaderWidget faderG;
    private EditBox urlBox;
    private Component status = Component.empty();
    private FaderWidget faderB;

    private int left;
    private int top;
    private int groupLabelX;
    private int groupLabelY;

    private boolean dirty;
    private int ticks;
    private int lastSend;

    public StageLightScreen(BlockPos pos, FixtureType type, LightData data) {
        super(Component.translatable("block.stagelite." + type.id()));
        this.pos = pos;
        this.type = type;
        this.data = data;
    }

    @Override
    protected void init() {
        left = (this.width - PANEL_W) / 2;
        top = Math.max(32, this.height / 2 - 80);

        // Colores rapidos
        if (type.usesColor()) {
            for (int i = 0; i < PRESETS.length; i++) {
                final int color = PRESETS[i];
                SwatchWidget swatch = new SwatchWidget(left + i * 18, top, 16, 14, color, () -> setColor(color));
                swatch.setTooltip(Tooltip.create(Component.translatable("tip.stagelite.preset")));
                addRenderableWidget(swatch);
            }
        }

        // Faders (solo los que tienen sentido para este dispositivo)
        int fy = top + 22;
        int fx = left;
        if (type.usesColor()) {
            faderR = addFader(fx, fy, "red", 0, 255, (data.color >> 16) & 255, 0xFF4040,
                    v -> data.color = (data.color & 0x00FFFF) | (v << 16));
            fx += FADER_PITCH;
            faderG = addFader(fx, fy, "green", 0, 255, (data.color >> 8) & 255, 0x40E040,
                    v -> data.color = (data.color & 0xFF00FF) | (v << 8));
            fx += FADER_PITCH;
            faderB = addFader(fx, fy, "blue", 0, 255, data.color & 255, 0x5080FF,
                    v -> data.color = (data.color & 0xFFFF00) | v);
            fx += FADER_PITCH + 8;
        }
        addFader(fx, fy, "intensity", 0, 100, data.intensity, 0xFFD060, v -> data.intensity = v);
        fx += FADER_PITCH;
        if (type.usesBeamSize()) {
            addFader(fx, fy, "spread", 1, 40, data.spread, 0xC0C0C0, v -> data.spread = v);
            fx += FADER_PITCH;
            addFader(fx, fy, "length", LightData.MIN_LENGTH, LightData.MAX_LENGTH, data.length, 0xC0C0C0, v -> data.length = v);
            fx += FADER_PITCH;
        }
        fx += 8;
        addFader(fx, fy, "speed", 1, 10, data.speed, 0xA070FF, v -> data.speed = v);
        fx += FADER_PITCH;
        if (type.animates()) {
            addFader(fx, fy, "range", 0, 90, data.range, 0xA070FF, v -> data.range = v);
            fx += FADER_PITCH;
        }
        if (type.tiltable()) {
            addFader(fx, fy, "tilt", -90, 90, data.tilt, 0x40C0A0, v -> data.tilt = v);
            fx += FADER_PITCH;
            addFader(fx, fy, "yaw", -90, 90, data.yaw, 0x40C0A0, v -> data.yaw = v);
            fx += FADER_PITCH;
        }
        if (type == FixtureType.HOLOGRAM) {
            addFader(fx, fy, "holo_turn", 0, 90, data.range, 0xA070FF, v -> data.range = v);
            fx += FADER_PITCH;
        }
        if (type.extraKey() != null) {
            addFader(fx, fy, type.extraKey(), 1, 10, data.extra, 0xFF9040, v -> data.extra = v);
        }

        // Botones a la derecha
        int bx = left + PANEL_W - 110;
        int bw = 110;
        int by = fy;

        addRenderableWidget(Button.builder(effectLabel(), btn -> {
                    data.effect = data.effect.next();
                    btn.setMessage(effectLabel());
                    commit();
                }).bounds(bx, by, bw, 20)
                .tooltip(Tooltip.create(Component.translatable("tip.stagelite.effect"))).build());
        by += 24;

        if (type.animates()) {
            addRenderableWidget(Button.builder(movementLabel(), btn -> {
                        data.movement = data.movement.next();
                        btn.setMessage(movementLabel());
                        commit();
                    }).bounds(bx, by, bw, 20)
                    .tooltip(Tooltip.create(Component.translatable("tip.stagelite.movement"))).build());
            by += 24;
        }

        if (type.usesUrl()) {
            addRenderableWidget(Button.builder(billboardLabel(), btn -> {
                        data.billboard = !data.billboard;
                        btn.setMessage(billboardLabel());
                        commit();
                    }).bounds(bx, by, bw, 20)
                    .tooltip(Tooltip.create(Component.translatable("tip.stagelite.billboard"))).build());
            by += 24;
        }

        addRenderableWidget(Button.builder(onLabel(), btn -> {
                    data.on = !data.on;
                    btn.setMessage(onLabel());
                    commit();
                }).bounds(bx, by, bw, 20)
                .tooltip(Tooltip.create(Component.translatable("tip.stagelite.power"))).build());
        by += 24;

        // Grupo: [-] etiqueta [+]
        addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
                    data.group = Mth.clamp(data.group - (hasShiftDown() ? 5 : 1), 0, 99);
                    commit();
                }).bounds(bx, by, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("tip.stagelite.group"))).build());
        addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
                    data.group = Mth.clamp(data.group + (hasShiftDown() ? 5 : 1), 0, 99);
                    commit();
                }).bounds(bx + bw - 20, by, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("tip.stagelite.group"))).build());
        groupLabelX = bx + bw / 2;
        groupLabelY = by + 6;
        by += 24;

        addRenderableWidget(Button.builder(Component.translatable("gui.stagelite.done"), btn -> onClose())
                .bounds(bx, by + 4, bw, 20).build());

        // Holograma: casilla para pegar un link, o elegir / arrastrar una imagen del computador
        if (type.usesUrl()) {
            urlBox = new EditBox(this.font, left, top + 152, PANEL_W - 130, 18, Component.translatable("gui.stagelite.url"));
            urlBox.setMaxLength(1024);
            urlBox.setHint(Component.translatable("gui.stagelite.url_hint"));
            urlBox.setValue(data.url);
            urlBox.setTooltip(Tooltip.create(Component.translatable("tip.stagelite.url")));
            urlBox.setResponder(text -> {
                String t = text.trim();
                if (t.isEmpty() || t.regionMatches(true, 0, "http", 0, 4) || t.startsWith(ImageIds.PREFIX)) {
                    data.url = t;
                    markDirty();
                }
            });
            addRenderableWidget(urlBox);
            addRenderableWidget(Button.builder(Component.translatable("gui.stagelite.pick_file"), btn -> pickFile())
                    .bounds(left + PANEL_W - 126, top + 152, 126, 18)
                    .tooltip(Tooltip.create(Component.translatable("tip.stagelite.pick_file"))).build());
        }
    }

    // ------------------------------------------------------------------ imagenes locales

    private void pickFile() {
        status = Component.translatable("gui.stagelite.img_dialog");
        Thread t = new Thread(() -> {
            String picked = null;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filters = stack.mallocPointer(5);
                filters.put(stack.UTF8("*.png")).put(stack.UTF8("*.jpg")).put(stack.UTF8("*.jpeg"))
                        .put(stack.UTF8("*.gif")).put(stack.UTF8("*.bmp"));
                filters.flip();
                picked = TinyFileDialogs.tinyfd_openFileDialog("StageLite", "", filters, "Imagenes", false);
            } catch (Throwable ex) {
                Minecraft.getInstance().execute(() -> status = Component.translatable("gui.stagelite.img_nodialog"));
                return;
            }
            final String file = picked;
            Minecraft.getInstance().execute(() -> {
                if (file == null) {
                    status = Component.empty();
                } else {
                    importFile(file);
                }
            });
        }, "StageLite-filedialog");
        t.setDaemon(true);
        t.start();
    }

    /** Lee una imagen del computador, la sube al servidor y deja el holograma apuntando a ella. */
    private void importFile(String pathText) {
        try {
            String t = pathText.trim();
            if (t.length() > 1 && t.startsWith("\"") && t.endsWith("\"")) {
                t = t.substring(1, t.length() - 1);
            }
            Path path = Path.of(t);
            long size = Files.size(path);
            if (size > ImageIds.MAX_CLIENT_BYTES) {
                status = Component.translatable("gui.stagelite.img_big", ImageIds.MAX_CLIENT_BYTES / 1024);
                return;
            }
            byte[] bytes = Files.readAllBytes(path);
            if (!ImageIds.isImage(bytes)) {
                status = Component.translatable("gui.stagelite.img_bad");
                return;
            }
            String hash = ImageIds.hash(bytes);
            HologramImages.saveLocal(hash, bytes);
            int total = (bytes.length + ImageIds.CHUNK - 1) / ImageIds.CHUNK;
            for (int i = 0; i < total; i++) {
                int from = i * ImageIds.CHUNK;
                int to = Math.min(bytes.length, from + ImageIds.CHUNK);
                ModNetwork.CHANNEL.sendToServer(new ImageChunkPacket(hash, i, total, Arrays.copyOfRange(bytes, from, to)));
            }
            String url = ImageIds.PREFIX + hash;
            data.url = url;
            urlBox.setValue(url);
            markDirty();
            status = Component.translatable("gui.stagelite.img_ok", path.getFileName().toString());
        } catch (IOException | RuntimeException ex) {
            status = Component.translatable("gui.stagelite.img_err");
        }
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        if (type.usesUrl() && !paths.isEmpty()) {
            importFile(paths.get(0).toString());
        }
    }

    @Override
    public boolean keyPressed(int key, int scancode, int mods) {
        // Enter en la casilla con una ruta de archivo (por ejemplo C:\fotos\logo.png)
        if (urlBox != null && urlBox.isFocused() && (key == 257 || key == 335)) {
            String t = urlBox.getValue().trim();
            if (!t.isEmpty() && !t.regionMatches(true, 0, "http", 0, 4) && !t.startsWith(ImageIds.PREFIX)) {
                importFile(t);
                return true;
            }
        }
        return super.keyPressed(key, scancode, mods);
    }

    private Component billboardLabel() {
        return Component.translatable(data.billboard ? "gui.stagelite.billboard_on" : "gui.stagelite.billboard_off");
    }

    private FaderWidget addFader(int x, int y, String key, int min, int max, int value, int color, IntConsumer setter) {
        FaderWidget fader = new FaderWidget(x, y, 28, FADER_H, Component.translatable("fader.stagelite." + key),
                min, max, value, color, setter, this::markDirty, this::commit);
        fader.setTooltip(Tooltip.create(Component.translatable("tip.stagelite." + key)));
        addRenderableWidget(fader);
        return fader;
    }

    private void setColor(int rgb) {
        data.color = rgb & 0xFFFFFF;
        if (faderR != null) {
            faderR.setSilently((rgb >> 16) & 255);
            faderG.setSilently((rgb >> 8) & 255);
            faderB.setSilently(rgb & 255);
        }
        commit();
    }

    private Component effectLabel() {
        return Component.translatable("gui.stagelite.effect", Component.translatable(data.effect.key()));
    }

    private Component movementLabel() {
        return Component.translatable("gui.stagelite.movement", Component.translatable(data.movement.key()));
    }

    private Component onLabel() {
        return Component.translatable(data.on ? "gui.stagelite.on" : "gui.stagelite.off");
    }

    private void markDirty() {
        dirty = true;
    }

    /** Envia el estado actual al servidor. */
    private void commit() {
        ModNetwork.CHANNEL.sendToServer(new UpdateLightPacket(pos, data.toTag()));
        dirty = false;
        lastSend = ticks;
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        // Mientras arrastras, envia como mucho cada 3 ticks (~7 veces por segundo).
        if (dirty && ticks - lastSend >= 3) {
            commit();
        }
    }

    @Override
    public void removed() {
        commit();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        // Fondo del panel
        int bottom = top + (type.usesUrl() ? 190 : 148);
        g.fill(left - 8, top - 24, left + PANEL_W + 8, bottom, 0xD0101014);
        g.fill(left - 8, top - 24, left + PANEL_W + 8, top - 23, 0xFF505058);
        g.fill(left - 8, bottom - 1, left + PANEL_W + 8, bottom, 0xFF505058);
        g.drawString(this.font, this.title, left, top - 18, 0xFFFFFF, false);

        // Muestra del color actual
        if (type.usesColor()) {
            int sx = left + 192;
            g.fill(sx - 1, top - 1, sx + 31, top + 15, 0xFFFFFFFF);
            g.fill(sx, top, sx + 30, top + 14, 0xFF000000 | (data.color & 0xFFFFFF));
        }

        // Rotulos de zonas
        if (type.usesColor()) {
            g.drawString(this.font, Component.translatable("gui.stagelite.zone_color"), left, top + 124, 0x909098, false);
            g.drawString(this.font, Component.translatable("gui.stagelite.zone_beam"), left + FADER_PITCH * 3 + 8, top + 124, 0x909098, false);
        }

        super.render(g, mouseX, mouseY, partialTick);
        if (type.usesUrl()) {
            g.drawString(this.font, status, left, top + 174, 0xA0D0FF, false);
        }

        // Etiqueta del grupo (entre - y +)
        Component group = Component.translatable("gui.stagelite.group",
                data.group == 0 ? Component.translatable("gui.stagelite.none") : Component.literal(String.valueOf(data.group)));
        g.drawCenteredString(this.font, group, groupLabelX, groupLabelY, 0xFFFFFF);
    }

    // ================================================================== widgets
}
