package com.stagelite.item;

import com.stagelite.blockentity.StageLightBlockEntity;
import com.stagelite.config.StageLiteConfig;
import com.stagelite.light.FixtureType;
import com.stagelite.light.LightRegistry;
import com.stagelite.network.ModNetwork;
import com.stagelite.network.OpenLightScreenPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Control remoto de pantallas de video.
 *  - Clic derecho apuntando a una pantalla: la enlaza (tambien si hay una pared detras).
 *  - Clic derecho en cualquier otro lado: abre el panel de la pantalla enlazada, desde lejos.
 *  - Agachado + clic derecho (sin apuntar a una pantalla): desenlaza.
 */
public class VideoRemoteItem extends Item {
    public static final double REACH = 64.0;
    /** Distancia maxima a la que el control puede editar una pantalla enlazada. */
    public static final double CONTROL_RANGE = 96.0;

    public VideoRemoteItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Nullable
    public static BlockPos linked(ItemStack stack, Level level) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("Link") || !tag.getString("Dim").equals(level.dimension().location().toString())) {
            return null;
        }
        return BlockPos.of(tag.getLong("Link"));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().contains("Link");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("Link")) {
            BlockPos p = BlockPos.of(tag.getLong("Link"));
            lines.add(Component.translatable("item.stagelite.video_remote.linked", p.getX(), p.getY(), p.getZ()).withStyle(ChatFormatting.GREEN));
        } else {
            lines.add(Component.translatable("item.stagelite.video_remote.unlinked").withStyle(ChatFormatting.YELLOW));
        }
        lines.add(Component.translatable("item.stagelite.video_remote.help").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (!level.isClientSide && ctx.getPlayer() instanceof ServerPlayer sp
                && level.getBlockEntity(ctx.getClickedPos()) instanceof StageLightBlockEntity speaker
                && speaker.type() == FixtureType.SPEAKER) {
            return handleSpeaker(level, sp, ctx.getItemInHand(), speaker);
        }
        return handle(level, ctx.getPlayer(), ctx.getItemInHand());
    }

    /** Clic derecho a un parlante con el control enlazado: lo conecta a esa pantalla (agachado: lo desconecta). */
    private InteractionResult handleSpeaker(Level level, ServerPlayer sp, ItemStack stack, StageLightBlockEntity speaker) {
        if (!StageLiteConfig.canEdit(sp)) {
            sp.displayClientMessage(Component.translatable("message.stagelite.no_permission"), true);
            return InteractionResult.FAIL;
        }
        com.stagelite.light.LightData nd = new com.stagelite.light.LightData();
        nd.copyFrom(speaker.data);
        if (sp.isShiftKeyDown()) {
            nd.linked = false;
            speaker.applyAndSync(nd);
            sp.displayClientMessage(Component.translatable("message.stagelite.speaker_unlinked"), true);
            return InteractionResult.CONSUME;
        }
        BlockPos screen = linked(stack, level);
        if (screen == null || !(level.getBlockEntity(screen) instanceof StageLightBlockEntity be) || !be.type().isVideo()) {
            sp.displayClientMessage(Component.translatable("message.stagelite.speaker_need_screen").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.CONSUME;
        }
        nd.linked = true;
        nd.link = screen.asLong();
        speaker.applyAndSync(nd);
        sp.displayClientMessage(Component.translatable("message.stagelite.speaker_linked").withStyle(ChatFormatting.GREEN), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return new InteractionResultHolder<>(handle(level, player, stack), stack);
    }

    private InteractionResult handle(Level level, @Nullable Player player, ItemStack stack) {
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!StageLiteConfig.canEdit(sp)) {
            sp.displayClientMessage(Component.translatable("message.stagelite.no_permission"), true);
            return InteractionResult.FAIL;
        }
        BlockPos linked = linked(stack, level);
        StageLightBlockEntity aimed = aimedScreen(level, sp);

        if (aimed != null && !aimed.getBlockPos().equals(linked)) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putLong("Link", aimed.getBlockPos().asLong());
            tag.putString("Dim", level.dimension().location().toString());
            sp.displayClientMessage(Component.translatable("message.stagelite.remote_linked").withStyle(ChatFormatting.GREEN), true);
            return InteractionResult.CONSUME;
        }
        if (aimed == null && sp.isShiftKeyDown() && linked != null) {
            stack.getOrCreateTag().remove("Link");
            sp.displayClientMessage(Component.translatable("message.stagelite.remote_unlinked"), true);
            return InteractionResult.CONSUME;
        }
        if (linked == null) {
            sp.displayClientMessage(Component.translatable("message.stagelite.remote_hint").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.CONSUME;
        }
        if (!level.isLoaded(linked) || !(level.getBlockEntity(linked) instanceof StageLightBlockEntity be) || !be.type().isVideo()) {
            sp.displayClientMessage(Component.translatable("message.stagelite.remote_missing").withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }
        if (sp.distanceToSqr(Vec3.atCenterOf(linked)) > CONTROL_RANGE * CONTROL_RANGE) {
            sp.displayClientMessage(Component.translatable("message.stagelite.remote_far").withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                new OpenLightScreenPacket(linked, be.type().ordinal(), be.data.toTag()));
        return InteractionResult.CONSUME;
    }

    /** Pantalla de video a la que apunta el jugador: se prueba contra la imagen (no contra el bloque). */
    @Nullable
    public static StageLightBlockEntity aimedScreen(Level level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        StageLightBlockEntity best = null;
        double bestT = REACH;
        for (StageLightBlockEntity be : LightRegistry.all()) {
            if (be.getLevel() != level || !be.type().isVideo()) {
                continue;
            }
            com.stagelite.video.DisplayPlane plane = com.stagelite.video.DisplayPlane.of(level, be);
            if (plane == null) {
                continue;
            }
            double t = plane.hit(eye, look, bestT);
            if (t > 0) {
                best = be;
                bestT = t;
            }
        }
        return best;
    }
}
