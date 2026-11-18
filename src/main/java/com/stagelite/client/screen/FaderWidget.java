package com.stagelite.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.IntConsumer;

/** Fader vertical tipo mesa de luces. */
public class FaderWidget extends AbstractWidget {
    private final int min;
    private final int max;
    private final int fillColor;
    private final IntConsumer onChange;
    private final Runnable onDirty;
    private final Runnable onCommit;
    private int value;

    FaderWidget(int x, int y, int w, int h, Component label, int min, int max, int value, int fillColor,
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
