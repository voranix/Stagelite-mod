package com.stagelite.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Coloca todas las piezas del molinete de una vez (el bloque donde se hace clic queda como centro). */
public class MolineteItem extends BlockItem {
    public MolineteItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext ctx) {
        Level level = ctx.getLevel();
        BlockPos center = ctx.getClickedPos();
        Player player = ctx.getPlayer();

        for (MolinetePart part : MolinetePart.values()) {
            BlockPos p = center.offset(part.dx, part.dy, part.dz);
            if (!level.isInWorldBounds(p) || !level.getBlockState(p).canBeReplaced()) {
                if (player != null && !level.isClientSide) {
                    player.displayClientMessage(Component.translatable("message.stagelite.molinete_no_space"), true);
                }
                return InteractionResult.FAIL;
            }
        }
        if (!level.isClientSide) {
            for (MolinetePart part : MolinetePart.values()) {
                BlockPos p = center.offset(part.dx, part.dy, part.dz);
                level.setBlock(p, getBlock().defaultBlockState().setValue(MolineteBlock.PART, part), 3);
            }
            level.playSound(null, center, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (player == null || !player.getAbilities().instabuild) {
                ctx.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
