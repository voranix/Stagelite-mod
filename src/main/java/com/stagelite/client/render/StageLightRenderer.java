package com.stagelite.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.stagelite.StageLite;
import com.stagelite.block.StageLightBlock;
import com.stagelite.blockentity.StageLightBlockEntity;
import com.stagelite.config.ClientConfig;
import com.stagelite.light.FixtureType;
import com.stagelite.light.LightData;
import com.stagelite.light.LightFx;
import com.stagelite.light.Movement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Dibuja:
 *  - el haz de luz (cono translucido y aditivo) de las 4 luces;
 *  - la parte movil de la cabeza movil (horquilla que gira y cabeza que se inclina).
 *
 * Sistema de coordenadas local: origen en el centro del bloque, +Z hacia donde mira la luz.
 * El haz es solo visual: la iluminacion real viene del nivel de luz del bloque.
 */
public class StageLightRenderer implements BlockEntityRenderer<StageLightBlockEntity> {
    private static final ResourceLocation TEX_BODY = new ResourceLocation(StageLite.MODID, "textures/block/stage_light_body.png");
    private static final ResourceLocation TEX_ACCENT = new ResourceLocation(StageLite.MODID, "textures/block/stage_light_accent.png");
    private static final ResourceLocation TEX_LENS = new ResourceLocation(StageLite.MODID, "textures/block/stage_light_lens.png");

    /** Inclinacion base de la cabeza movil cuando se mueve (para que el barrido se note). */
    private static final float MOVING_BASE_TILT = 35f;

    private static final int MAX_SIDES = 12;
    private static final float[] COS = new float[MAX_SIDES + 1];
    private static final float[] SIN = new float[MAX_SIDES + 1];
    private static int tableSides = -1;

    /** Rotaciones para que +Z local apunte hacia cada direccion. Indice = get3DDataValue(). */
    private static final Quaternionf[] FACING_ROT = new Quaternionf[6];

    static {
        FACING_ROT[Direction.DOWN.get3DDataValue()] = new Quaternionf().rotateX((float) Math.toRadians(90));
        FACING_ROT[Direction.UP.get3DDataValue()] = new Quaternionf().rotateX((float) Math.toRadians(-90));
        FACING_ROT[Direction.NORTH.get3DDataValue()] = new Quaternionf().rotateY((float) Math.toRadians(180));
        FACING_ROT[Direction.SOUTH.get3DDataValue()] = new Quaternionf();
        FACING_ROT[Direction.WEST.get3DDataValue()] = new Quaternionf().rotateY((float) Math.toRadians(270));
        FACING_ROT[Direction.EAST.get3DDataValue()] = new Quaternionf().rotateY((float) Math.toRadians(90));
    }

    public StageLightRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(StageLightBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        Block block = be.getBlockState().getBlock();
        if (!(block instanceof StageLightBlock lightBlock)) {
            return;
        }

        LightData d = be.data;
        FixtureType type = lightBlock.fixtureType();
        Direction facing = be.getBlockState().getValue(StageLightBlock.FACING);

        float time = level.getGameTime() + partialTick;
        boolean lit = d.on && d.intensity > 0;
        float bright = lit ? LightFx.brightness(d, time) : 0f;
        boolean beamVisible = lit && bright > 0.001f;
        boolean moves = type.moves();

        // Los modelos fijos ya estan en el JSON del bloque: sin haz no hay nada que dibujar.
        if (!moves && !beamVisible) {
            return;
        }

        int rgb = LightFx.color(d, time);
        float r = (rgb >> 16 & 255) / 255f;
        float g = (rgb >> 8 & 255) / 255f;
        float b = (rgb & 255) / 255f;

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(FACING_ROT[facing.get3DDataValue()]);

        if (moves) {
            boolean moving = d.movement != Movement.NONE;
            float pan = moving ? LightFx.pan(d, time) : 0f;
            float tilt = moving ? LightFx.tilt(d, time) : 0f;
            float baseTilt = moving ? MOVING_BASE_TILT : 0f;

            // La horquilla gira alrededor del eje de la base (Z); la cabeza se inclina en X.
            pose.mulPose(Axis.ZP.rotationDegrees(pan));
            drawYoke(buffers, pose, packedLight, packedOverlay);
            pose.mulPose(Axis.XP.rotationDegrees(baseTilt + tilt));
            int lensLight = lit ? LightTexture.FULL_BRIGHT : packedLight;
            drawHead(buffers, pose, packedLight, lensLight, packedOverlay, lit ? r : 0.35f, lit ? g : 0.35f, lit ? b : 0.35f);
        }

        if (beamVisible) {
            float length = moves ? d.length : clippedLength(be, level, d, facing, type);
            if (length > 0.05f) {
                drawBeam(buffers, pose, d, type, length, r, g, b, bright);
            }
        }

        pose.popPose();
    }

    // ------------------------------------------------------------------ cabeza movil

    private static void drawYoke(MultiBufferSource buffers, PoseStack pose, int light, int overlay) {
        PoseStack.Pose p = pose.last();
        VertexConsumer body = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_BODY));
        box(p, body, -0.05f, -0.05f, -0.375f, 0.05f, 0.05f, -0.30f, light, overlay, 255, 255, 255); // cuello
        box(p, body, -0.26f, -0.07f, -0.30f, 0.26f, 0.07f, -0.22f, light, overlay, 255, 255, 255);  // travesano
        VertexConsumer accent = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_ACCENT));
        box(p, accent, 0.20f, -0.10f, -0.30f, 0.26f, 0.10f, 0.10f, light, overlay, 255, 255, 255);   // brazos
        box(p, accent, -0.26f, -0.10f, -0.30f, -0.20f, 0.10f, 0.10f, light, overlay, 255, 255, 255);
    }

    private static void drawHead(MultiBufferSource buffers, PoseStack pose, int light, int lensLight, int overlay,
                                 float r, float g, float b) {
        PoseStack.Pose p = pose.last();
        VertexConsumer body = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_BODY));
        box(p, body, -0.14f, -0.14f, -0.16f, 0.14f, 0.14f, 0.20f, light, overlay, 255, 255, 255);
        VertexConsumer accent = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_ACCENT));
        box(p, accent, -0.16f, -0.16f, 0.20f, 0.16f, 0.16f, 0.25f, light, overlay, 255, 255, 255);   // aro frontal
        box(p, accent, 0.14f, -0.04f, -0.04f, 0.20f, 0.04f, 0.04f, light, overlay, 255, 255, 255);   // ejes
        box(p, accent, -0.20f, -0.04f, -0.04f, -0.14f, 0.04f, 0.04f, light, overlay, 255, 255, 255);
        // Lente: iluminada y teñida con el color actual
        VertexConsumer lens = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_LENS));
        quad(p, lens, -0.11f, -0.11f, 0.255f, -0.11f, 0.11f, 0.255f, 0.11f, 0.11f, 0.255f, 0.11f, -0.11f, 0.255f,
                0f, 0f, 1f, lensLight, overlay, Math.round(r * 255), Math.round(g * 255), Math.round(b * 255));
    }

    /** Caja con las 6 caras (textura completa en cada una). */
    private static void box(PoseStack.Pose p, VertexConsumer vc, float x0, float y0, float z0,
                            float x1, float y1, float z1, int light, int overlay, int r, int g, int b) {
        quad(p, vc, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, 0, 0, -1, light, overlay, r, g, b); // -Z
        quad(p, vc, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, 0, 0, 1, light, overlay, r, g, b);  // +Z
        quad(p, vc, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, -1, 0, 0, light, overlay, r, g, b); // -X
        quad(p, vc, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, 1, 0, 0, light, overlay, r, g, b);  // +X
        quad(p, vc, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, 0, -1, 0, light, overlay, r, g, b); // -Y
        quad(p, vc, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0, light, overlay, r, g, b);  // +Y
    }

    private static void quad(PoseStack.Pose p, VertexConsumer vc,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float nx, float ny, float nz, int light, int overlay, int r, int g, int b) {
        Matrix4f m = p.pose();
        vc.vertex(m, ax, ay, az).color(r, g, b, 255).uv(0f, 1f).overlayCoords(overlay).uv2(light).normal(p.normal(), nx, ny, nz).endVertex();
        vc.vertex(m, bx, by, bz).color(r, g, b, 255).uv(0f, 0f).overlayCoords(overlay).uv2(light).normal(p.normal(), nx, ny, nz).endVertex();
        vc.vertex(m, cx, cy, cz).color(r, g, b, 255).uv(1f, 0f).overlayCoords(overlay).uv2(light).normal(p.normal(), nx, ny, nz).endVertex();
        vc.vertex(m, dx, dy, dz).color(r, g, b, 255).uv(1f, 1f).overlayCoords(overlay).uv2(light).normal(p.normal(), nx, ny, nz).endVertex();
    }

    // ------------------------------------------------------------------ haz

    private static void drawBeam(MultiBufferSource buffers, PoseStack pose, LightData d, FixtureType type,
                                 float length, float r, float g, float b, float bright) {
        float alphaStart = 0.45f * (d.intensity / 100f) * bright;
        float alphaEnd = 0.03f * (d.intensity / 100f) * bright;

        float r0 = type.beamRadius();
        float radiusEnd = r0 + length * (float) Math.tan(Math.toRadians(d.spread));
        float z0 = type.beamStart();
        float z1 = z0 + length;

        Matrix4f m = pose.last().pose();
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());

        int sides = ClientConfig.BEAM_SIDES.get();
        ensureTable(sides);

        for (int i = 0; i < sides; i++) {
            float c0 = COS[i], s0 = SIN[i], c1 = COS[i + 1], s1 = SIN[i + 1];
            float ax = r0 * c0, ay = r0 * s0;
            float bx = r0 * c1, by = r0 * s1;
            float cx = radiusEnd * c1, cy = radiusEnd * s1;
            float dx = radiusEnd * c0, dy = radiusEnd * s0;

            // Cara exterior
            vertex(vc, m, ax, ay, z0, r, g, b, alphaStart);
            vertex(vc, m, bx, by, z0, r, g, b, alphaStart);
            vertex(vc, m, cx, cy, z1, r, g, b, alphaEnd);
            vertex(vc, m, dx, dy, z1, r, g, b, alphaEnd);
            // Cara interior (orden inverso, por si el culling esta activo)
            vertex(vc, m, dx, dy, z1, r, g, b, alphaEnd);
            vertex(vc, m, cx, cy, z1, r, g, b, alphaEnd);
            vertex(vc, m, bx, by, z0, r, g, b, alphaStart);
            vertex(vc, m, ax, ay, z0, r, g, b, alphaStart);
        }
    }

    /** Recalcula la tabla de seno/coseno solo si cambia el numero de lados. */
    private static void ensureTable(int sides) {
        if (sides == tableSides) {
            return;
        }
        for (int i = 0; i <= sides; i++) {
            float ang = (float) (2 * Math.PI * i / sides);
            COS[i] = (float) Math.cos(ang);
            SIN[i] = (float) Math.sin(ang);
        }
        tableSides = sides;
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z,
                               float r, float g, float b, float a) {
        vc.vertex(m, x, y, z).color(r, g, b, a).endVertex();
    }

    /**
     * Largo del haz cortado por bloques solidos (luces que no se mueven).
     * Se guarda un segundo (20 ticks) para no gastar rendimiento.
     */
    private static float clippedLength(StageLightBlockEntity be, Level level, LightData d,
                                       Direction facing, FixtureType type) {
        long now = level.getGameTime();
        if (be.clippedLength >= 0f && be.clipTime != Long.MIN_VALUE && Math.abs(now - be.clipTime) < 20) {
            return Math.min(be.clippedLength, d.length);
        }
        Entity viewer = Minecraft.getInstance().player;
        if (viewer == null) {
            return d.length;
        }

        double startOffset = 0.55;
        Vec3 start = Vec3.atCenterOf(be.getBlockPos()).add(
                facing.getStepX() * startOffset, facing.getStepY() * startOffset, facing.getStepZ() * startOffset);
        Vec3 end = start.add(facing.getStepX() * (double) d.length,
                facing.getStepY() * (double) d.length, facing.getStepZ() * (double) d.length);

        BlockHitResult hit = level.clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer));

        float result = d.length;
        if (hit.getType() != HitResult.Type.MISS) {
            float fromCenter = (float) (startOffset + hit.getLocation().distanceTo(start));
            result = Math.max(0f, Math.min(d.length, fromCenter - type.beamStart()));
        }
        be.clippedLength = result;
        be.clipTime = now;
        return result;
    }

    @Override
    public int getViewDistance() {
        return ClientConfig.RENDER_DISTANCE.get();
    }
}
