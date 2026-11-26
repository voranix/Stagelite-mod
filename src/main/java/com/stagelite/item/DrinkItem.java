package com.stagelite.item;

import com.stagelite.registry.ModBlocks;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** Bebida de cine: se toma y devuelve el vaso vacio. */
public class DrinkItem extends Item {
    public DrinkItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public SoundEvent getEatingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (entity instanceof Player p && !p.getAbilities().instabuild) {
            ItemStack cup = new ItemStack(ModBlocks.EMPTY_CUP.get());
            if (result.isEmpty()) {
                return cup;
            }
            if (!p.getInventory().add(cup)) {
                p.drop(cup, false);
            }
        }
        return result;
    }
}
