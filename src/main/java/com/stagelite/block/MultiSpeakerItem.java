package com.stagelite.block;

import com.stagelite.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Coloca un parlante grande entero: el bloque donde haces clic es la esquina inferior izquierda (vista de frente). */
public class MultiSpeakerItem extends BlockItem {
    private final StageLightBlock lightBlock;

    public MultiSpeakerItem(StageLightBlock block, Properties properties) {
        super(block, properties);
        this.lightBlock = block;
    }

    @Override
    public InteractionResult place(BlockPlaceContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        Direction facing = ctx.getHorizontalDirection().getOpposite();
        Direction right = facing.getCounterClockWise();
        BlockPos origin = ctx.getClickedPos();
        int w = lightBlock.fixtureType().partsW();
        int h = lightBlock.fixtureType().partsH();

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                BlockPos p = origin.offset(right.getStepX() * x, y, right.getStepZ() * x);
                if (!level.isInWorldBounds(p) || !level.getBlockState(p).canBeReplaced()) {
                    if (player != null && !level.isClientSide) {
                        player.displayClientMessage(Component.translatable("message.stagelite.speaker_no_space", w, h), true);
                    }
                    return InteractionResult.FAIL;
                }
            }
        }
        if (!level.isClientSide) {
            BlockState main = lightBlock.defaultBlockState().setValue(StageLightBlock.FACING, facing);
            level.setBlock(origin, main, 3);
            lightBlock.setPlacedBy(level, origin, main, player, ctx.getItemInHand());
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    if (x == 0 && y == 0) {
                        continue;
                    }
                    SpeakerPart part = SpeakerPart.of(lightBlock.fixtureType(), x, y);
                    BlockPos p = origin.offset(right.getStepX() * x, y, right.getStepZ() * x);
                    level.setBlock(p, ModBlocks.SPEAKER_PART.get().defaultBlockState()
                            .setValue(SpeakerPartBlock.FACING, facing).setValue(SpeakerPartBlock.PART, part), 3);
                }
            }
            level.playSound(null, origin, SoundEvents.NOTE_BLOCK_BASS.get(), SoundSource.BLOCKS, 1.0F, 0.8F);
            if (player == null || !player.getAbilities().instabuild) {
                ctx.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
