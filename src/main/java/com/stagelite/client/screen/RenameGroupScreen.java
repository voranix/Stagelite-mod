package com.stagelite.client.screen;

import com.stagelite.network.ConsoleActionPacket;
import com.stagelite.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Ventanita para ponerle nombre a un grupo (vacio = sin nombre). */
public class RenameGroupScreen extends Screen {
    private final Screen parent;
    private final BlockPos pos;
    private final int group;
    private final String current;
    private final Consumer<String> onNamed;
    private EditBox box;

    public RenameGroupScreen(Screen parent, BlockPos pos, int group, String current, Consumer<String> onNamed) {
        super(Component.translatable("gui.stagelite.rename_title", group));
        this.parent = parent;
        this.pos = pos;
        this.group = group;
        this.current = current;
        this.onNamed = onNamed;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        box = new EditBox(this.font, cx - 100, cy - 10, 200, 20, this.title);
        box.setMaxLength(24);
        box.setHint(Component.translatable("gui.stagelite.rename_hint"));
        box.setValue(current);
        addRenderableWidget(box);
        setInitialFocus(box);
        addRenderableWidget(Button.builder(Component.translatable("gui.stagelite.rename_ok"), b -> confirm())
                .bounds(cx - 100, cy + 16, 98, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.stagelite.rename_cancel"), b -> onClose())
                .bounds(cx + 2, cy + 16, 98, 20).build());
    }

    private void confirm() {
        String name = box.getValue().trim();
        ModNetwork.CHANNEL.sendToServer(new ConsoleActionPacket(pos, ConsoleActionPacket.GROUP_NAME, group, 0, name));
        onNamed.accept(name);
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int mods) {
        if (key == 257 || key == 335) {
            confirm();
            return true;
        }
        return super.keyPressed(key, scancode, mods);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 28, 0xFFFFFF);
        super.render(g, mouseX, mouseY, partialTick);
    }
}
