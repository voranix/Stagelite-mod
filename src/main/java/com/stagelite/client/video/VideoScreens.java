package com.stagelite.client.video;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.stagelite.StageLite;
import com.stagelite.block.StageLightBlock;
import com.stagelite.blockentity.StageLightBlockEntity;
import com.stagelite.config.ClientConfig;
import com.stagelite.light.LightData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Pantallas de video. Todo es del cliente: el servidor solo guarda los datos.
 * Solo se reproducen las pantallas cercanas (maximo configurable) para no gastar rendimiento.
 */
@Mod.EventBusSubscriber(modid = StageLite.MODID, value = Dist.CLIENT)
public final class VideoScreens {
    private static final Set<StageLightBlockEntity> TRACKED = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<StageLightBlockEntity, Session> ACTIVE = new HashMap<>();

    private static final class Session {
        Object browser;
        String url = "";
        int restart;
        int tex = -1;
        String lastState = "";
        int age;
        int sinceSend;
    }

    private VideoScreens() {
    }

    public static void track(StageLightBlockEntity be) {
        TRACKED.add(be);
    }

    public static void untrack(StageLightBlockEntity be) {
        TRACKED.remove(be);
        Session s = ACTIVE.remove(be);
        if (s != null && s.browser != null) {
            McefBridge.close(s.browser);
        }
    }

    // ------------------------------------------------------------------ logica (cada 5 ticks)

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            closeAll();
            return;
        }
        if (mc.level.getGameTime() % 5 != 0) {
            return;
        }
        if (!ClientConfig.VIDEOS_ENABLED.get()) {
            closeAll();
            return;
        }
        if ((TRACKED.isEmpty() && ACTIVE.isEmpty()) || !McefBridge.available()) {
            return;
        }
        Vec3 eye = mc.player.getEyePosition();

        // candidatas: encendidas, con link y dentro de alcance (+margen para no abrir/cerrar en el limite)
        List<StageLightBlockEntity> wanted = new ArrayList<>();
        for (StageLightBlockEntity be : new ArrayList<>(TRACKED)) {
            if (be.isRemoved() || be.getLevel() != mc.level) {
                continue;
            }
            LightData d = be.data;
            if (!d.on || d.url.isEmpty()) {
                continue;
            }
            double reach = d.length + (ACTIVE.containsKey(be) ? 32 : 16);
            if (dist2(be, eye) <= reach * reach) {
                wanted.add(be);
            }
        }
        wanted.sort((a, b) -> Double.compare(dist2(a, eye), dist2(b, eye)));
        int max = ClientConfig.VIDEO_MAX_SCREENS.get();
        if (wanted.size() > max) {
            wanted = new ArrayList<>(wanted.subList(0, max));
        }

        // cerrar las que ya no corresponden
        for (StageLightBlockEntity be : new ArrayList<>(ACTIVE.keySet())) {
            if (!wanted.contains(be)) {
                Session s = ACTIVE.remove(be);
                if (s.browser != null) {
                    McefBridge.close(s.browser);
                }
            }
        }

        for (StageLightBlockEntity be : wanted) {
            LightData d = be.data;
            String url = normalize(d.url);
            if (url == null) {
                continue; // link corto de TikTok: se esta resolviendo
            }
            Session s = ACTIVE.get(be);
            if (s == null) {
                int w = ClientConfig.VIDEO_WIDTH.get();
                Object browser = McefBridge.create(url, w, w * 9 / 16);
                if (browser == null) {
                    continue;
                }
                s = new Session();
                s.browser = browser;
                s.url = d.url;
                s.restart = d.track;
                ACTIVE.put(be, s);
            } else if (!s.url.equals(d.url) || s.restart != d.track) {
                s.url = d.url;
                s.restart = d.track;
                s.age = 0;
                s.lastState = "";
                McefBridge.load(s.browser, url);
            }
            s.age += 5;
            s.sinceSend += 5;
            s.tex = McefBridge.textureId(s.browser);

            // gesto de usuario simulado para permitir sonido (cuando la pagina ya cargo)
            if (s.age == 60 || s.age == 150) {
                McefBridge.click(s.browser, 8, 8);
            }

            // volumen segun distancia, velocidad, bucle
            double dist = Math.sqrt(dist2(be, eye));
            double fall = Math.max(0.0, 1.0 - dist / Math.max(1.0, d.length));
            double volume = d.intensity / 100.0 * fall * fall;
            String state = String.format(Locale.ROOT, "%.2f|%.2f|%b", volume, rate(d.speed), d.billboard);
            if (!state.equals(s.lastState) || s.sinceSend >= 40) {
                s.lastState = state;
                s.sinceSend = 0;
                McefBridge.js(s.browser, script(volume, rate(d.speed), d.billboard));
            }
        }
    }

    private static void closeAll() {
        for (Session s : ACTIVE.values()) {
            if (s.browser != null) {
                McefBridge.close(s.browser);
            }
        }
        ACTIVE.clear();
    }

    private static double dist2(StageLightBlockEntity be, Vec3 p) {
        return be.getBlockPos().distToCenterSqr(p.x, p.y, p.z);
    }

    /** speed 1..10 -> velocidad de reproduccion (4 = normal). */
    static double rate(int speed) {
        double[] table = {0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0, 3.0, 4.0};
        return table[Math.max(0, Math.min(9, speed - 1))];
    }

    private static String script(double volume, double rate, boolean loop) {
        return "(function(){var v=document.querySelector('video');if(!v)return;"
                + "document.documentElement.style.background='#000';if(document.body)document.body.style.background='#000';"
                + "v.controls=false;v.style.objectFit='contain';"
                + "v.muted=" + (volume <= 0.001) + ";v.volume=" + String.format(Locale.ROOT, "%.3f", Math.max(0, Math.min(1, volume))) + ";"
                + "v.playbackRate=" + String.format(Locale.ROOT, "%.2f", rate) + ";v.loop=" + loop + ";"
                + "if(v.paused){var p=v.play();if(p&&p.catch)p.catch(function(){});}})();";
    }

    private static final Map<String, String> SHORT_TIKTOK = new java.util.concurrent.ConcurrentHashMap<>();

    private static boolean isShortTikTok(String low) {
        return low.contains("vm.tiktok.com/") || low.contains("vt.tiktok.com/") || low.contains("tiktok.com/t/");
    }

    private static String tiktokPlayer(String id) {
        return "https://www.tiktok.com/player/v1/" + id + "?music_info=0&description=0&controls=0&autoplay=1&loop=1";
    }

    /** Sigue (en segundo plano) las redirecciones de un link corto de TikTok hasta el link completo. */
    private static void resolveShort(String url) {
        if (SHORT_TIKTOK.putIfAbsent(url, "") != null) {
            return;
        }
        Thread t = new Thread(() -> {
            String cur = url;
            try {
                for (int hop = 0; hop < 5; hop++) {
                    java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(cur).openConnection();
                    c.setInstanceFollowRedirects(false);
                    c.setConnectTimeout(5000);
                    c.setReadTimeout(5000);
                    c.setRequestProperty("User-Agent", "Mozilla/5.0");
                    int code = c.getResponseCode();
                    String loc = c.getHeaderField("Location");
                    c.disconnect();
                    if (code >= 300 && code < 400 && loc != null) {
                        cur = loc.startsWith("http") ? loc : new java.net.URL(new java.net.URL(cur), loc).toString();
                        String id = tiktokId(cur);
                        if (id != null) {
                            SHORT_TIKTOK.put(url, tiktokPlayer(id));
                            return;
                        }
                    } else {
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
            SHORT_TIKTOK.put(url, url); // no se pudo: se intenta cargar tal cual
        }, "stagelite-tiktok");
        t.setDaemon(true);
        t.start();
    }

    private static String tiktokId(String u) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("/(?:video|player/v1|embed/v2|embed)/(\\d{8,25})").matcher(u);
        return m.find() ? m.group(1) : null;
    }

    /** Convierte links de YouTube y TikTok a su reproductor embebido. Devuelve null mientras se resuelve un link corto. */
    static String normalize(String url) {
        String u = url.trim();
        String id = null;
        String low = u.toLowerCase(Locale.ROOT);
        if (low.contains("tiktok.com")) {
            if (isShortTikTok(low)) {
                String done = SHORT_TIKTOK.get(u);
                if (done == null) {
                    resolveShort(u);
                    return null;
                }
                return done.isEmpty() ? null : done;
            }
            String tid = tiktokId(u);
            if (tid != null) {
                return tiktokPlayer(tid);
            }
        }
        try {
            if (low.contains("youtube.com/watch")) {
                int i = u.indexOf("v=");
                if (i >= 0) {
                    id = cut(u.substring(i + 2));
                }
            } else if (low.contains("youtu.be/")) {
                id = cut(u.substring(low.indexOf("youtu.be/") + 9));
            } else if (low.contains("youtube.com/shorts/")) {
                id = cut(u.substring(low.indexOf("shorts/") + 7));
            } else if (low.contains("youtube.com/embed/")) {
                id = cut(u.substring(low.indexOf("embed/") + 6));
            }
        } catch (RuntimeException ignored) {
        }
        if (id != null && id.matches("[A-Za-z0-9_-]{6,20}")) {
            return "https://www.youtube.com/embed/" + id
                    + "?autoplay=1&controls=0&rel=0&modestbranding=1&playsinline=1&loop=1&playlist=" + id;
        }
        return u;
    }

    private static String cut(String s) {
        int end = s.length();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&' || c == '?' || c == '#' || c == '/') {
                end = i;
                break;
            }
        }
        return s.substring(0, end);
    }

    // ------------------------------------------------------------------ dibujado

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || ACTIVE.isEmpty()) {
            return;
        }
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        for (Map.Entry<StageLightBlockEntity, Session> e : ACTIVE.entrySet()) {
            StageLightBlockEntity be = e.getKey();
            Session s = e.getValue();
            if (s.tex < 0 || be.isRemoved()) {
                continue;
            }
            Direction f = be.getBlockState().getValue(StageLightBlock.FACING);
            if (f.getAxis() == Direction.Axis.Y) {
                f = Direction.NORTH;
            }
            Direction r = f.getCounterClockWise();
            double width = be.data.extra * 2.0;
            double hw = width / 2.0;
            double hh = width * 9.0 / 32.0;
            double cx = be.getBlockPos().getX() + 0.5 - f.getStepX() * 0.375 + f.getStepX() * 0.004;
            double cy = be.getBlockPos().getY() + 0.5;
            double cz = be.getBlockPos().getZ() + 0.5 - f.getStepZ() * 0.375 + f.getStepZ() * 0.004;

            pose.pushPose();
            pose.translate(-cam.x, -cam.y, -cam.z);
            Matrix4f m = pose.last().pose();
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, s.tex);
            RenderSystem.disableCull();
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
            BufferBuilder bb = Tesselator.getInstance().getBuilder();
            bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            float rx = (float) (r.getStepX() * hw);
            float rz = (float) (r.getStepZ() * hw);
            float uy = (float) hh;
            vtx(bb, m, cx - rx, cy + uy, cz - rz, 0f, 0f);
            vtx(bb, m, cx + rx, cy + uy, cz + rz, 1f, 0f);
            vtx(bb, m, cx + rx, cy - uy, cz + rz, 1f, 1f);
            vtx(bb, m, cx - rx, cy - uy, cz - rz, 0f, 1f);
            Tesselator.getInstance().end();
            RenderSystem.enableCull();
            pose.popPose();
        }
    }

    private static void vtx(BufferBuilder bb, Matrix4f m, double x, double y, double z, float u, float v) {
        bb.vertex(m, (float) x, (float) y, (float) z).uv(u, v).color(255, 255, 255, 255).endVertex();
    }
}
