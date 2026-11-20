package com.stagelite.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.stagelite.StageLite;
import com.stagelite.block.StageLightBlock;
import com.stagelite.blockentity.StageLightBlockEntity;
import com.stagelite.client.HologramImages;
import com.stagelite.config.ClientConfig;
import com.stagelite.light.FixtureType;
import com.stagelite.light.FxMath;
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
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
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

    private static final ResourceLocation TEX_DISCO = new ResourceLocation(StageLite.MODID, "textures/block/disco_facet.png");
    private static final ResourceLocation TEX_HOLO_EMPTY = new ResourceLocation(StageLite.MODID, "textures/block/hologram_empty.png");
    private static final ResourceLocation TEX_HOLO_LOADING = new ResourceLocation(StageLite.MODID, "textures/block/hologram_loading.png");
    private static final ResourceLocation TEX_HOLO_ERROR = new ResourceLocation(StageLite.MODID, "textures/block/hologram_error.png");

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

        float time = (level.getGameTime() % 1_048_576L) + partialTick;
        boolean lit = d.on && d.intensity > 0;
        float bright = lit ? LightFx.brightness(d, time) : 0f;
        boolean beamVisible = lit && bright > 0.001f;
        boolean moves = type.moves();

        // Luces fijas inclinadas: el modelo del bloque se oculta (TILTED) y se dibuja aqui girado junto con el haz.
        if (type.tiltable() && !moves && (d.tilt != 0 || d.yaw != 0)) {
            renderTiltedBody(be, pose, buffers, packedLight, packedOverlay, facing, d.tilt, d.yaw);
        }

        int rgb = LightFx.color(d, time);
        float r = (rgb >> 16 & 255) / 255f;
        float g = (rgb >> 8 & 255) / 255f;
        float b = (rgb & 255) / 255f;

        // Dispositivos con dibujado propio (el resto de los modelos fijos ya estan en el JSON del bloque)
        switch (type) {
            case SMOKE:
            case FIRE:
            case FIREWORK:
                return; // solo particulas / cohetes
            case LASER:
                if (beamVisible) {
                    renderLaser(be, pose, buffers, d, facing, time, r, g, b, bright);
                }
                return;
            case DISCO:
                renderDisco(be.getBlockPos(), pose, buffers, packedLight, packedOverlay, d, time, lit, r, g, b, bright);
                return;
            case HOLOGRAM:
                renderHologram(be, pose, buffers, d, facing, time, r, g, b, bright);
                return;
            default:
                break;
        }

        // Sin haz y sin partes moviles no hay nada que dibujar.
        if (!moves && !beamVisible) {
            return;
        }

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(FACING_ROT[facing.get3DDataValue()]);
        if (!moves) {
            pose.mulPose(Axis.YP.rotationDegrees(d.yaw));
            pose.mulPose(Axis.XP.rotationDegrees(-d.tilt));
        }

        if (moves) {
            boolean moving = d.movement != Movement.NONE;
            float pan = moving ? LightFx.pan(d, time) : 0f;
            float tilt = moving ? LightFx.tilt(d, time) : 0f;
            float baseTilt = moving ? MOVING_BASE_TILT : 0f;

            // La horquilla gira alrededor del eje de la base (Z); la cabeza se inclina en X.
            pose.mulPose(Axis.ZP.rotationDegrees(pan));
            drawYoke(buffers, pose, packedLight, packedOverlay);
            pose.mulPose(Axis.YP.rotationDegrees(d.yaw));
            pose.mulPose(Axis.XP.rotationDegrees(baseTilt + tilt - d.tilt));
            int lensLight = lit ? LightTexture.FULL_BRIGHT : packedLight;
            drawHead(buffers, pose, packedLight, lensLight, packedOverlay, lit ? r : 0.35f, lit ? g : 0.35f, lit ? b : 0.35f);
        }

        if (beamVisible) {
            float length = (moves || d.tilt != 0 || d.yaw != 0) ? d.length : clippedLength(be, level, d, facing, type);
            if (length > 0.05f) {
                drawBeam(buffers, pose, d, type, length, r, g, b, bright);
            }
        }

        pose.popPose();
    }


    private static void renderTiltedBody(StageLightBlockEntity be, PoseStack pose, MultiBufferSource buffers,
                                          int light, int overlay, Direction facing, int tilt, int yaw) {
        BlockState base = be.getBlockState().setValue(StageLightBlock.TILTED, false);
        Quaternionf rot = FACING_ROT[facing.get3DDataValue()];
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(rot);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.mulPose(Axis.XP.rotationDegrees(-tilt));
        pose.mulPose(new Quaternionf(rot).conjugate());
        pose.translate(-0.5, -0.5, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(base, pose, buffers, light, overlay);
        pose.popPose();
    }

    // ------------------------------------------------------------------ laser

    private static void renderLaser(StageLightBlockEntity be, PoseStack pose, MultiBufferSource buffers, LightData d,
                                    Direction facing, float time, float r, float g, float b, float bright) {
        boolean moving = d.movement != Movement.NONE;
        float pan = moving ? LightFx.pan(d, time) : 0f;
        float tilt = moving ? LightFx.tilt(d, time) : 0f;

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(FACING_ROT[facing.get3DDataValue()]);
        pose.mulPose(Axis.YP.rotationDegrees(d.yaw));
        pose.mulPose(Axis.XP.rotationDegrees(-d.tilt));
        pose.mulPose(Axis.ZP.rotationDegrees(pan));
        pose.mulPose(Axis.XP.rotationDegrees(tilt));

        Matrix4f m = pose.last().pose();
        VertexConsumer vc = buffers.getBuffer(ModRenderTypes.BEAM);
        int n = 2 + d.extra * 2;
        float fan = Math.min(80f, d.spread * 2f);
        float start = FixtureType.LASER.beamStart();
        float a = 0.9f * (d.intensity / 100f) * bright;
        for (int i = 0; i < n; i++) {
            float ang = (float) Math.toRadians(n == 1 ? 0f : (i / (n - 1f) - 0.5f) * 2f * fan);
            float dx = FxMath.sin(ang);
            float dz = FxMath.cos(ang);
            float x0 = dx * start;
            float z0 = dz * start;
            float x1 = dx * (start + d.length);
            float z1 = dz * (start + d.length);
            // Nucleo fino (vertical y horizontal) y halo mas ancho y tenue
            ribbon(vc, m, x0, 0, z0, x1, 0, z1, 0, 1, 0, 0.012f, r, g, b, a, a * 0.6f);
            ribbon(vc, m, x0, 0, z0, x1, 0, z1, dz, 0, -dx, 0.012f, r, g, b, a, a * 0.6f);
            ribbon(vc, m, x0, 0, z0, x1, 0, z1, 0, 1, 0, 0.05f, r, g, b, a * 0.2f, a * 0.05f);
            ribbon(vc, m, x0, 0, z0, x1, 0, z1, dz, 0, -dx, 0.05f, r, g, b, a * 0.2f, a * 0.05f);
        }
        pose.popPose();
    }

    /** Cinta de dos caras entre dos puntos; (ox,oy,oz) es la direccion del ancho. */
    private static void ribbon(VertexConsumer vc, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz,
                               float ox, float oy, float oz, float w, float r, float g, float b, float a0, float a1) {
        float px = ox * w;
        float py = oy * w;
        float pz = oz * w;
        vertex(vc, m, ax - px, ay - py, az - pz, r, g, b, a0);
        vertex(vc, m, ax + px, ay + py, az + pz, r, g, b, a0);
        vertex(vc, m, bx + px, by + py, bz + pz, r, g, b, a1);
        vertex(vc, m, bx - px, by - py, bz - pz, r, g, b, a1);
        vertex(vc, m, bx - px, by - py, bz - pz, r, g, b, a1);
        vertex(vc, m, bx + px, by + py, bz + pz, r, g, b, a1);
        vertex(vc, m, ax + px, ay + py, az + pz, r, g, b, a0);
        vertex(vc, m, ax - px, ay - py, az - pz, r, g, b, a0);
    }

    // ------------------------------------------------------------------ bola disco

    private static final int DISCO_LAT = 8;
    private static final int DISCO_LON = 12;
    private static final float DISCO_R = 0.27f;

    private static void renderDisco(net.minecraft.core.BlockPos blockPos, PoseStack pose, MultiBufferSource buffers, int light, int overlay, LightData d,
                                    float time, boolean lit, float r, float g, float b, float bright) {
        float spin = time * d.speed * 1.2f;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(spin));

        // Bola de espejitos: cada faceta brilla en su momento
        PoseStack.Pose p = pose.last();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_DISCO));
        for (int la = 0; la < DISCO_LAT; la++) {
            float lat0 = (float) (Math.PI * (la / (float) DISCO_LAT - 0.5));
            float lat1 = (float) (Math.PI * ((la + 1) / (float) DISCO_LAT - 0.5));
            for (int lo = 0; lo < DISCO_LON; lo++) {
                float lon0 = (float) (2 * Math.PI * lo / DISCO_LON);
                float lon1 = (float) (2 * Math.PI * (lo + 1) / DISCO_LON);
                float sparkle = lit ? Math.max(0f, FxMath.sin(time * 0.35f + (la * 7 + lo * 13) * 1.7f)) : 0f;
                sparkle = sparkle * sparkle * sparkle;
                float base = lit ? 0.75f : 0.4f;
                float k = Math.min(1f, base + sparkle);
                // Plateado, con un toque del color actual; los destellos van a blanco.
                int cr = Math.round(255 * Math.min(1f, k * (0.75f + 0.25f * r) + sparkle * 0.5f));
                int cg = Math.round(255 * Math.min(1f, k * (0.75f + 0.25f * g) + sparkle * 0.5f));
                int cb = Math.round(255 * Math.min(1f, k * (0.75f + 0.25f * b) + sparkle * 0.5f));
                int facetLight = sparkle > 0.3f ? LightTexture.FULL_BRIGHT : (lit ? LightTexture.pack(11, 11) : light);
                float mlat = (lat0 + lat1) / 2f;
                float mlon = (lon0 + lon1) / 2f;
                float nx = FxMath.cos(mlat) * FxMath.cos(mlon);
                float ny = FxMath.sin(mlat);
                float nz = FxMath.cos(mlat) * FxMath.sin(mlon);
                quad(p, vc,
                        sx(lat0, lon0), sy(lat0), sz(lat0, lon0),
                        sx(lat1, lon0), sy(lat1), sz(lat1, lon0),
                        sx(lat1, lon1), sy(lat1), sz(lat1, lon1),
                        sx(lat0, lon1), sy(lat0), sz(lat0, lon1),
                        nx, ny, nz, facetLight, overlay, cr, cg, cb);
            }
        }
        pose.popPose();

        if (!lit || bright <= 0.001f) {
            return;
        }

        // Rayos de luz que giran con la bola
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        Matrix4f m = pose.last().pose();
        VertexConsumer lv = buffers.getBuffer(ModRenderTypes.BEAM);
        int rays = 6 + d.extra * 3;
        float a = 0.9f * (d.intensity / 100f) * bright;
        var cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        float camX = (float) (cam.x - (blockPos.getX() + 0.5));
        float camY = (float) (cam.y - (blockPos.getY() + 0.5));
        float camZ = (float) (cam.z - (blockPos.getZ() + 0.5));
        for (int i = 0; i < rays; i++) {
            float az = (float) Math.toRadians((i * 137.5f + spin * 1.5f) % 360f);
            float el = (float) Math.toRadians(-55f + 110f * ((i * 0.618f) % 1f));
            float ce = FxMath.cos(el);
            float dx = ce * FxMath.cos(az);
            float dy = FxMath.sin(el);
            float dz = ce * FxMath.sin(az);
            float len = d.length * (0.5f + 0.5f * ((i * 0.37f) % 1f));
            float cr = r;
            float cg = g;
            float cb = b;
            if (d.effect == com.stagelite.light.Effect.RAINBOW) {
                int c = Mth.hsvToRgb(((time * 0.01f * d.speed) + i / (float) rays) % 1f, 1f, 1f);
                cr = (c >> 16 & 255) / 255f;
                cg = (c >> 8 & 255) / 255f;
                cb = (c & 255) / 255f;
            }
            float ax = dx * DISCO_R;
            float ay = dy * DISCO_R;
            float az0 = dz * DISCO_R;
            float bx = dx * (DISCO_R + len);
            float by = dy * (DISCO_R + len);
            float bz = dz * (DISCO_R + len);
            // Cinta que siempre mira a la camara: nunca se ve "de canto"
            float mx = camX - (ax + bx) / 2f;
            float my = camY - (ay + by) / 2f;
            float mz = camZ - (az0 + bz) / 2f;
            float px = dy * mz - dz * my;
            float py = dz * mx - dx * mz;
            float pz = dx * my - dy * mx;
            float pl = Mth.sqrt(px * px + py * py + pz * pz);
            if (pl < 1.0e-4f) {
                continue;
            }
            px /= pl;
            py /= pl;
            pz /= pl;
            ribbon(lv, m, ax, ay, az0, bx, by, bz, px, py, pz, 0.045f, cr, cg, cb, a, a * 0.45f);
            ribbon(lv, m, ax, ay, az0, bx, by, bz, px, py, pz, 0.14f, cr, cg, cb, a * 0.25f, 0f);
        }
        pose.popPose();
    }

    private static float sx(float lat, float lon) {
        return DISCO_R * FxMath.cos(lat) * FxMath.cos(lon);
    }

    private static float sy(float lat) {
        return DISCO_R * FxMath.sin(lat);
    }

    private static float sz(float lat, float lon) {
        return DISCO_R * FxMath.cos(lat) * FxMath.sin(lon);
    }

    // ------------------------------------------------------------------ holograma

    private static void renderHologram(StageLightBlockEntity be, PoseStack pose, MultiBufferSource buffers, LightData d,
                                       Direction facing, float time, float r, float g, float b, float bright) {
        if (!d.on || d.intensity <= 0 || bright <= 0.001f || !ClientConfig.HOLOGRAMS_ENABLED.get()) {
            return;
        }
        ResourceLocation tex = TEX_HOLO_EMPTY;
        float aspect = 1f;
        if (!d.url.isEmpty()) {
            HologramImages.Entry e = HologramImages.get(d.url);
            switch (e.state) {
                case READY:
                    tex = e.texture;
                    aspect = e.aspect;
                    break;
                case LOADING:
                    tex = TEX_HOLO_LOADING;
                    break;
                default:
                    tex = TEX_HOLO_ERROR;
                    break;
            }
        }
        float height = 0.5f + d.extra * 0.5f;
        float width = height * aspect;
        float dist = 0.5f + height / 2f + 0.15f;
        float cx = 0.5f + facing.getStepX() * dist;
        float cy = 0.5f + facing.getStepY() * dist;
        float cz = 0.5f + facing.getStepZ() * dist;

        float yaw;
        if (d.billboard) {
            var cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            double dx = cam.x - (be.getBlockPos().getX() + cx);
            double dz = cam.z - (be.getBlockPos().getZ() + cz);
            yaw = (float) Math.atan2(dx, dz);
        } else {
            yaw = (float) Math.toRadians(d.range * 4f);
        }

        // Parpadeo suave tipo holograma
        float flicker = 0.88f + 0.12f * FxMath.sin(time * 0.9f) * FxMath.sin(time * 0.37f + 1f);
        int alpha = Math.round(255 * Math.min(1f, (d.intensity / 100f) * bright * flicker));
        int cr = Math.round(255 * r);
        int cg = Math.round(255 * g);
        int cb = Math.round(255 * b);

        pose.pushPose();
        pose.translate(cx, cy, cz);
        pose.mulPose(Axis.YP.rotation(yaw));
        PoseStack.Pose p = pose.last();
        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(tex));
        float hw = width / 2f;
        float hh = height / 2f;
        int full = LightTexture.FULL_BRIGHT;
        // Cara delantera y trasera (la trasera se ve igual, como un holograma real)
        quadAlpha(p, vc, -hw, -hh, 0f, -hw, hh, 0f, hw, hh, 0f, hw, -hh, 0f, 0f, 0f, 1f, full, cr, cg, cb, alpha);
        quadAlpha(p, vc, hw, -hh, 0f, hw, hh, 0f, -hw, hh, 0f, -hw, -hh, 0f, 0f, 0f, -1f, full, cr, cg, cb, alpha);
        pose.popPose();
    }

    private static void quadAlpha(PoseStack.Pose p, VertexConsumer vc,
                                  float ax, float ay, float az, float bx, float by, float bz,
                                  float cx, float cy, float cz, float dx, float dy, float dz,
                                  float nx, float ny, float nz, int light, int r, int g, int b, int a) {
        Matrix4f m = p.pose();
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        vc.vertex(m, ax, ay, az).color(r, g, b, a).uv(0f, 1f).overlayCoords(overlay).uv2(light).normal(p.normal(), nx, ny, nz).endVertex();
        vc.vertex(m, bx, by, bz).color(r, g, b, a).uv(0f, 0f).overlayCoords(overlay).uv2(light).normal(p.normal(), nx, ny, nz).endVertex();
        vc.vertex(m, cx, cy, cz).color(r, g, b, a).uv(1f, 0f).overlayCoords(overlay).uv2(light).normal(p.normal(), nx, ny, nz).endVertex();
        vc.vertex(m, dx, dy, dz).color(r, g, b, a).uv(1f, 1f).overlayCoords(overlay).uv2(light).normal(p.normal(), nx, ny, nz).endVertex();
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
        VertexConsumer vc = buffers.getBuffer(ModRenderTypes.BEAM);

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
