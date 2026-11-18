package com.stagelite.client.screen;

import com.stagelite.light.FixtureType;
import com.stagelite.light.LightData;
import com.stagelite.network.ModNetwork;
import com.stagelite.network.UpdateLightPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
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
    private static final int PANEL_W = 400;
    private static final int FADER_PITCH = 34;
    private static final int FADER_H = 96;

    private final BlockPos pos;
    private final FixtureType type;
    private final LightData data;

    private Fader faderR;
    private Fader faderG;
    private Fader faderB;

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
        for (int i = 0; i < PRESETS.length; i++) {
            final int color = PRESETS[i];
            Swatch swatch = new Swatch(left + i * 18, top, 16, 14, color, () -> setColor(color));
            swatch.setTooltip(Tooltip.create(Component.translatable("tip.stagelite.preset")));
            addRenderableWidget(swatch);
        }

        // Faders
        int fy = top + 22;
        int fx = left;
        faderR = addFader(fx, fy, "red", 0, 255, (data.color >> 16) & 255, 0xFF4040,
                v -> data.color = (data.color & 0x00FFFF) | (v << 16));
        fx += FADER_PITCH;
        faderG = addFader(fx, fy, "green", 0, 255, (data.color >> 8) & 255, 0x40E040,
                v -> data.color = (data.color & 0xFF00FF) | (v << 8));
        fx += FADER_PITCH;
        faderB = addFader(fx, fy, "blue", 0, 255, data.color & 255, 0x5080FF,
                v -> data.color = (data.color & 0xFFFF00) | v);
        fx += FADER_PITCH + 8;
        addFader(fx, fy, "intensity", 0, 100, data.intensity, 0xFFD060, v -> data.intensity = v);
        fx += FADER_PITCH;
        addFader(fx, fy, "spread", 1, 40, data.spread, 0xC0C0C0, v -> data.spread = v);
        fx += FADER_PITCH;
        addFader(fx, fy, "length", LightData.MIN_LENGTH, LightData.MAX_LENGTH, data.length, 0xC0C0C0, v -> data.length = v);
        fx += FADER_PITCH + 8;
        addFader(fx, fy, "speed", 1, 10, data.speed, 0xA070FF, v -> data.speed = v);
        if (type.moves()) {
            fx += FADER_PITCH;
            addFader(fx, fy, "range", 0, 90, data.range, 0xA070FF, v -> data.range = v);
        }

        // Botones a la derecha
        int bx = left + 290;
        int bw = 110;
        int by = fy;

        addRenderableWidget(Button.builder(effectLabel(), btn -> {
                    data.effect = data.effect.next();
                    btn.setMessage(effectLabel());
                    commit();
                }).bounds(bx, by, bw, 20)
                .tooltip(Tooltip.create(Component.translatable("tip.stagelite.effect"))).build());
        by += 24;

        if (type.moves()) {
            addRenderableWidget(Button.builder(movementLabel(), btn -> {
                        data.movement = data.movement.next();
                        btn.setMessage(movementLabel());
                        commit();
                    }).bounds(bx, by, bw, 20)
                    .tooltip(Tooltip.create(Component.translatable("tip.stagelite.movement"))).build());
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
    }

    private Fader addFader(int x, int y, String key, int min, int max, int value, int color, IntConsumer setter) {
        Fader fader = new Fader(x, y, 30, FADER_H, Component.translatable("fader.stagelite." + key),
                min, max, value, color, setter, this::markDirty, this::commit);
        fader.setTooltip(Tooltip.create(Component.translatable("tip.stagelite." + key)));
        addRenderableWidget(fader);
        return fader;
    }

    private void setColor(int rgb) {
        data.color = rgb & 0xFFFFFF;
        faderR.setSilently((rgb >> 16) & 255);
        faderG.setSilently((rgb >> 8) & 255);
        faderB.setSilently(rgb & 255);
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
        g.fill(left - 8, top - 24, left + PANEL_W + 8, top + 148, 0xD0101014);
        g.fill(left - 8, top - 24, left + PANEL_W + 8, top - 23, 0xFF505058);
        g.fill(left - 8, top + 147, left + PANEL_W + 8, top + 148, 0xFF505058);
        g.drawString(this.font, this.title, left, top - 18, 0xFFFFFF, false);

        // Muestra del color actual
        int sx = left + 192;
        g.fill(sx - 1, top - 1, sx + 31, top + 15, 0xFFFFFFFF);
        g.fill(sx, top, sx + 30, top + 14, 0xFF000000 | (data.color & 0xFFFFFF));

        // Rotulos de zonas
        g.drawString(this.font, Component.translatable("gui.stagelite.zone_color"), left, top + 124, 0x909098, false);
        g.drawString(this.font, Component.translatable("gui.stagelite.zone_beam"), left + FADER_PITCH * 3 + 8, top + 124, 0x909098, false);

        super.render(g, mouseX, mouseY, partialTick);

        // Etiqueta del grupo (entre - y +)
        Component group = Component.translatable("gui.stagelite.group",
                data.group == 0 ? Component.translatable("gui.stagelite.none") : Component.literal(String.valueOf(data.group)));
        g.drawCenteredString(this.font, group, groupLabelX, groupLabelY, 0xFFFFFF);
    }

    // ================================================================== widgets

    /** Cuadrito de color rapido. */
    private static class Swatch extends AbstractWidget {
        private final int color;
        private final Runnable action;

        Swatch(int x, int y, int w, int h, int color, Runnable action) {
            super(x, y, w, h, Component.empty());
            this.color = color;
            this.action = action;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int border = isHoveredOrFocused() ? 0xFFFFFFFF : 0xFF404048;
            g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), border);
            g.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, 0xFF000000 | color);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            action.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    /** Fader vertical tipo mesa de luces. */
    private static class Fader extends AbstractWidget {
        private final int min;
        private final int max;
        private final int fillColor;
        private final IntConsumer onChange;
        private final Runnable onDirty;
        private final Runnable onCommit;
        private int value;

        Fader(int x, int y, int w, int h, Component label, int min, int max, int value, int fillColor,
              IntConsumer onChange, Runnable onDirty, Runnable onCommit) {
            super(x, y, w, h, label);
            this.min = min;
            this.max = max;
            this.value = Mth.clamp(value, min, max);
            this.fillColor = fillColor;
            this.onChange = onChange;
            this.onDirty = onDirty;
            this.onCommit = onCommit;
        }

        private int trackTop() {
            return getY() + 12;
        }

        private int trackBottom() {
            return getY() + getHeight() - 13;
        }

        /** Cambia el valor sin avisar (lo usan los colores rapidos). */
        void setSilently(int v) {
            this.value = Mth.clamp(v, min, max);
        }

        private void setValue(int v) {
            int clamped = Mth.clamp(v, min, max);
            if (clamped != value) {
                value = clamped;
                onChange.accept(value);
                onDirty.run();
            }
        }

        private void setFromMouse(double mouseY) {
            double frac = (trackBottom() - mouseY) / (double) (trackBottom() - trackTop());
            frac = Mth.clamp(frac, 0.0, 1.0);
            setValue((int) Math.round(min + frac * (max - min)));
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            Minecraft mc = Minecraft.getInstance();
            int cx = getX() + getWidth() / 2;
            int tTop = trackTop();
            int tBot = trackBottom();

            // Carril
            g.fill(cx - 3, tTop - 1, cx + 3, tBot + 1, 0xFF000000);
            g.fill(cx - 1, tTop, cx + 1, tBot, 0xFF2C2C34);

            // Tramo relleno
            float frac = (value - min) / (float) (max - min);
            int capY = tBot - Math.round(frac * (tBot - tTop));
            g.fill(cx - 1, capY, cx + 1, tBot, 0xFF000000 | fillColor);

            // Marcas cada 25 %
            for (int i = 0; i <= 4; i++) {
                int ty = tBot - i * (tBot - tTop) / 4;
                g.fill(cx - 12, ty, cx - 8, ty + 1, 0xFF606068);
                g.fill(cx + 8, ty, cx + 12, ty + 1, 0xFF606068);
            }

            // Tapa del fader
            boolean hot = isHoveredOrFocused();
            g.fill(cx - 9, capY - 5, cx + 9, capY + 5, 0xFF000000);
            g.fill(cx - 8, capY - 4, cx + 8, capY + 4, hot ? 0xFFE6E6E6 : 0xFFB4B4BC);
            g.fill(cx - 8, capY, cx + 8, capY + 1, 0xFF303038);
            g.fill(cx - 8, capY - 4, cx + 8, capY - 3, 0xFFFFFFFF);

            // Valor arriba y nombre abajo
            g.drawCenteredString(mc.font, String.valueOf(value), cx, getY() + 1, 0xFFFFFF);
            g.drawCenteredString(mc.font, getMessage(), cx, getY() + getHeight() - 10, 0xB0B0B8);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            setFromMouse(mouseY);
        }

        @Override
        protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
            setFromMouse(mouseY);
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            onCommit.run();
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (!isMouseOver(mouseX, mouseY) || delta == 0) {
                return false;
            }
            int step = Screen.hasShiftDown() ? 5 : 1;
            setValue(value + (int) Math.signum(delta) * step);
            return true;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
