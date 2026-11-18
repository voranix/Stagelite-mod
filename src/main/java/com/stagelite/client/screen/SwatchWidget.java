package com.stagelite.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Cuadrito de color rapido. */
public class SwatchWidget extends AbstractWidget {
    private final int color;
    private final Runnable action;

    SwatchWidget(int x, int y, int w, int h, int color, Runnable action) {
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
