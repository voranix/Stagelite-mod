package com.stagelite.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.stagelite.StageLite;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.metadata.IIOMetadata;
import org.w3c.dom.Node;
import java.util.ArrayList;
import java.util.List;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Descarga y guarda las imagenes de los hologramas (solo en el cliente).
 *
 * Seguridad: solo http/https en los puertos normales, no se conecta a direcciones privadas o locales
 * (127.x, 192.168.x, 10.x...), maximo 8 MB por imagen y maximo 24 imagenes en memoria.
 * Solo imagenes (PNG, JPG, GIF animado, BMP). No admite video.
 */
public final class HologramImages {
    public enum State {
        LOADING, READY, FAILED
    }

    public static final class Entry {
        public volatile State state = State.LOADING;
        public ResourceLocation texture;
        public float aspect = 1f;
        private long lastUsed;
        private DynamicTexture dynamic;
        /** Solo GIF animado: cuadros, tiempo de cada uno (ms) y cuadro mostrado ahora. */
        private List<NativeImage> frames;
        private int[] delays;
        private int totalMs;
        private int current;
    }

    private static final int MAX_BYTES = 8 * 1024 * 1024;
    private static final int MAX_FRAMES = 120;
    private static final double MAX_ANIM_PIXELS = 16_000_000.0;
    private static final int MAX_ENTRIES = 24;
    private static final int MAX_SIDE = 512;
    private static final long MAX_PIXELS = 25_000_000L;

    private static final Map<String, Entry> CACHE = new HashMap<>();
    private static int counter;
    private static final ExecutorService POOL = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "StageLite-hologram");
        t.setDaemon(true);
        return t;
    });

    private HologramImages() {
    }

    /** Llamar solo desde el hilo de render. Devuelve siempre una entrada (cargando, lista o fallida). */
    public static Entry get(String url) {
        Entry e = CACHE.get(url);
        if (e == null) {
            evictIfNeeded();
            e = new Entry();
            CACHE.put(url, e);
            final Entry entry = e;
            POOL.submit(() -> load(url, entry));
        }
        long now = System.currentTimeMillis();
        e.lastUsed = now;
        if (e.state == State.READY && e.frames != null) {
            animate(e, now);
        }
        return e;
    }

    /** Pasa al cuadro del GIF que toca segun la hora. */
    private static void animate(Entry e, long now) {
        int t = (int) (now % e.totalMs);
        int idx = 0;
        int acc = 0;
        for (int i = 0; i < e.delays.length; i++) {
            acc += e.delays[i];
            if (t < acc) {
                idx = i;
                break;
            }
        }
        if (idx != e.current && e.dynamic != null && e.dynamic.getPixels() != null) {
            e.dynamic.getPixels().copyFrom(e.frames.get(idx));
            e.dynamic.upload();
            e.current = idx;
        }
    }

    private static void free(Entry e) {
        if (e.texture != null) {
            Minecraft.getInstance().getTextureManager().release(e.texture);
        }
        if (e.frames != null) {
            for (NativeImage f : e.frames) {
                f.close();
            }
            e.frames = null;
        }
    }

    private static void evictIfNeeded() {
        if (CACHE.size() < MAX_ENTRIES) {
            return;
        }
        String oldestKey = null;
        long oldest = Long.MAX_VALUE;
        for (Map.Entry<String, Entry> me : CACHE.entrySet()) {
            if (me.getValue().state != State.LOADING && me.getValue().lastUsed < oldest) {
                oldest = me.getValue().lastUsed;
                oldestKey = me.getKey();
            }
        }
        if (oldestKey != null) {
            free(CACHE.remove(oldestKey));
        }
    }

    /** Vacia todo (al salir del mundo). */
    public static void clear() {
        Iterator<Entry> it = CACHE.values().iterator();
        while (it.hasNext()) {
            free(it.next());
            it.remove();
        }
    }

    // ------------------------------------------------------------------ descarga (hilo aparte)

    private static void load(String url, Entry entry) {
        try {
            byte[] bytes = fetch(url);
            List<BufferedImage> imgs = new ArrayList<>();
            List<Integer> delays = new ArrayList<>();
            decode(bytes, imgs, delays);
            if (imgs.isEmpty()) {
                throw new IOException("not an image");
            }
            int w = imgs.get(0).getWidth();
            int h = imgs.get(0).getHeight();
            float scale = Math.min(1f, MAX_SIDE / (float) Math.max(w, h));
            if (imgs.size() > 1) {
                scale = Math.min(scale, (float) Math.sqrt(MAX_ANIM_PIXELS / ((double) imgs.size() * w * h)));
            }
            int tw = Math.max(1, Math.round(w * scale));
            int th = Math.max(1, Math.round(h * scale));
            List<NativeImage> frames = new ArrayList<>();
            for (BufferedImage img : imgs) {
                frames.add(toNative(img, tw, th, scale));
            }
            int[] delayArr = new int[delays.size()];
            int total = 0;
            for (int i = 0; i < delayArr.length; i++) {
                delayArr[i] = delays.get(i);
                total += delayArr[i];
            }
            final int totalMs = Math.max(1, total);
            final float aspect = Math.max(0.25f, Math.min(4f, tw / (float) th));
            Minecraft.getInstance().execute(() -> {
                try {
                    NativeImage first = new NativeImage(tw, th, false);
                    first.copyFrom(frames.get(0));
                    DynamicTexture tex = new DynamicTexture(first);
                    ResourceLocation loc = new ResourceLocation(StageLite.MODID,
                            "holo/" + Integer.toHexString(url.hashCode()) + "_" + (counter++));
                    Minecraft.getInstance().getTextureManager().register(loc, tex);
                    entry.dynamic = tex;
                    entry.texture = loc;
                    entry.aspect = aspect;
                    if (frames.size() > 1) {
                        entry.frames = frames;
                        entry.delays = delayArr;
                        entry.totalMs = totalMs;
                        entry.current = 0;
                    } else {
                        frames.get(0).close();
                    }
                    entry.state = State.READY;
                } catch (Exception ex) {
                    entry.state = State.FAILED;
                }
            });
        } catch (Exception ex) {
            entry.state = State.FAILED;
        }
    }

    private static NativeImage toNative(BufferedImage img, int tw, int th, float scale) {
        int w = img.getWidth();
        int h = img.getHeight();
        NativeImage ni = new NativeImage(tw, th, false);
        for (int y = 0; y < th; y++) {
            int sy = Math.min(h - 1, (int) (y / scale));
            for (int x = 0; x < tw; x++) {
                int sx = Math.min(w - 1, (int) (x / scale));
                int argb = img.getRGB(sx, sy);
                int a = argb >>> 24;
                int r = (argb >> 16) & 255;
                int g = (argb >> 8) & 255;
                int b = argb & 255;
                ni.setPixelRGBA(x, y, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }
        return ni;
    }

    /** Lee una imagen (1 cuadro) o un GIF animado (varios cuadros ya compuestos, con sus tiempos en ms). */
    private static void decode(byte[] bytes, List<BufferedImage> out, List<Integer> delays) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                return;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, false, false);
                if ("gif".equalsIgnoreCase(reader.getFormatName())) {
                    readGif(reader, out, delays);
                    if (!out.isEmpty()) {
                        return;
                    }
                }
                long w = reader.getWidth(0);
                long h = reader.getHeight(0);
                if (w <= 0 || h <= 0 || w * h > MAX_PIXELS) {
                    return;
                }
                out.add(reader.read(0));
                delays.add(100);
            } finally {
                reader.dispose();
            }
        }
    }

    private static void readGif(ImageReader reader, List<BufferedImage> out, List<Integer> delays) throws IOException {
        int n = Math.min(reader.getNumImages(true), MAX_FRAMES);
        int sw = 0;
        int sh = 0;
        IIOMetadata sm = reader.getStreamMetadata();
        if (sm != null) {
            Node root = sm.getAsTree("javax_imageio_gif_stream_1.0");
            Node lsd = child(root, "LogicalScreenDescriptor");
            if (lsd != null) {
                sw = intAttr(lsd, "logicalScreenWidth");
                sh = intAttr(lsd, "logicalScreenHeight");
            }
        }
        if (sw <= 0 || sh <= 0) {
            sw = reader.getWidth(0);
            sh = reader.getHeight(0);
        }
        if (sw <= 0 || sh <= 0 || (long) sw * sh > MAX_PIXELS) {
            return;
        }
        // Limite de memoria mientras se decodifica: se recortan cuadros si el GIF es muy grande.
        n = Math.max(1, Math.min(n, (int) (24_000_000L / ((long) sw * sh))));
        BufferedImage canvas = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < n; i++) {
            BufferedImage frame = reader.read(i);
            int left = 0;
            int top = 0;
            int delay = 100;
            String disposal = "none";
            IIOMetadata fm = reader.getImageMetadata(i);
            if (fm != null) {
                Node root = fm.getAsTree("javax_imageio_gif_image_1.0");
                Node desc = child(root, "ImageDescriptor");
                if (desc != null) {
                    left = intAttr(desc, "imageLeftPosition");
                    top = intAttr(desc, "imageTopPosition");
                }
                Node gce = child(root, "GraphicControlExtension");
                if (gce != null) {
                    int d = intAttr(gce, "delayTime") * 10;
                    delay = d < 20 ? 100 : d;
                    Node dm = gce.getAttributes().getNamedItem("disposalMethod");
                    if (dm != null) {
                        disposal = dm.getNodeValue();
                    }
                }
            }
            BufferedImage before = null;
            if ("restoreToPrevious".equals(disposal)) {
                before = copy(canvas);
            }
            Graphics2D g = canvas.createGraphics();
            g.drawImage(frame, left, top, null);
            g.dispose();
            out.add(copy(canvas));
            delays.add(delay);
            if ("restoreToBackgroundColor".equals(disposal)) {
                Graphics2D g2 = canvas.createGraphics();
                g2.setComposite(AlphaComposite.Clear);
                g2.fillRect(left, top, frame.getWidth(), frame.getHeight());
                g2.dispose();
            } else if (before != null) {
                canvas = before;
            }
        }
    }

    private static BufferedImage copy(BufferedImage src) {
        BufferedImage c = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = c.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return c;
    }

    private static Node child(Node parent, String name) {
        for (Node c = parent.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (name.equals(c.getNodeName())) {
                return c;
            }
        }
        return null;
    }

    private static int intAttr(Node node, String name) {
        Node a = node.getAttributes().getNamedItem(name);
        if (a == null) {
            return 0;
        }
        try {
            return Integer.parseInt(a.getNodeValue());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static byte[] fetch(String urlText) throws IOException {
        URL url = new URL(urlText);
        for (int hop = 0; hop < 4; hop++) {
            checkUrl(url);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(5000);
            c.setReadTimeout(8000);
            c.setRequestProperty("User-Agent", "StageLite/0.2 (Minecraft mod)");
            int code = c.getResponseCode();
            if (code >= 300 && code < 400) {
                String loc = c.getHeaderField("Location");
                c.disconnect();
                if (loc == null) {
                    throw new IOException("redirect without location");
                }
                url = new URL(url, loc);
                continue;
            }
            if (code != 200) {
                c.disconnect();
                throw new IOException("http " + code);
            }
            if (c.getContentLengthLong() > MAX_BYTES) {
                c.disconnect();
                throw new IOException("too big");
            }
            try (InputStream in = c.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    if (out.size() > MAX_BYTES) {
                        throw new IOException("too big");
                    }
                }
                return out.toByteArray();
            } finally {
                c.disconnect();
            }
        }
        throw new IOException("too many redirects");
    }

    private static void checkUrl(URL url) throws IOException {
        String proto = url.getProtocol();
        if (!"http".equals(proto) && !"https".equals(proto)) {
            throw new IOException("bad protocol");
        }
        int port = url.getPort();
        if (port != -1 && port != 80 && port != 443) {
            throw new IOException("bad port");
        }
        for (InetAddress a : InetAddress.getAllByName(url.getHost())) {
            byte[] raw = a.getAddress();
            boolean uniqueLocal = raw.length == 16 && (raw[0] & 0xFE) == 0xFC;
            if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress()
                    || a.isSiteLocalAddress() || a.isMulticastAddress() || uniqueLocal) {
                throw new IOException("private address");
            }
        }
    }
}
