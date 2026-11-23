package com.stagelite.item;

import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Pulsera de entrada: se tiñe con tintes (mesa de crafteo o caldero). */
public class WristbandItem extends Item implements DyeableLeatherItem {
    public WristbandItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getColor(ItemStack stack) {
        net.minecraft.nbt.CompoundTag tag = stack.getTagElement("display");
        return tag != null && tag.contains("color", 99) ? tag.getInt("color") : 0xFFFFFF;
    }
}
