package com.stagelite.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.stagelite.StageLite;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
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
 * (127.x, 192.168.x, 10.x...), maximo 4 MB por imagen y maximo 24 imagenes en memoria.
 * Solo imagenes (PNG, JPG, GIF -primer cuadro-, BMP). No admite video.
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
    }

    private static final int MAX_BYTES = 4 * 1024 * 1024;
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
        e.lastUsed = System.currentTimeMillis();
        return e;
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
            Entry gone = CACHE.remove(oldestKey);
            if (gone.texture != null) {
                Minecraft.getInstance().getTextureManager().release(gone.texture);
            }
        }
    }

    /** Vacia todo (al salir del mundo). */
    public static void clear() {
        Iterator<Entry> it = CACHE.values().iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            if (e.texture != null) {
                Minecraft.getInstance().getTextureManager().release(e.texture);
            }
            it.remove();
        }
    }

    // ------------------------------------------------------------------ descarga (hilo aparte)

    private static void load(String url, Entry entry) {
        try {
            byte[] bytes = fetch(url);
            BufferedImage img = decode(bytes);
            if (img == null) {
                throw new IOException("not an image");
            }
            int w = img.getWidth();
            int h = img.getHeight();
            float scale = Math.min(1f, MAX_SIDE / (float) Math.max(w, h));
            int tw = Math.max(1, Math.round(w * scale));
            int th = Math.max(1, Math.round(h * scale));
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
            final float aspect = Math.max(0.25f, Math.min(4f, tw / (float) th));
            Minecraft.getInstance().execute(() -> {
                try {
                    DynamicTexture tex = new DynamicTexture(ni);
                    ResourceLocation loc = new ResourceLocation(StageLite.MODID,
                            "holo/" + Integer.toHexString(url.hashCode()) + "_" + (counter++));
                    Minecraft.getInstance().getTextureManager().register(loc, tex);
                    entry.dynamic = tex;
                    entry.texture = loc;
                    entry.aspect = aspect;
                    entry.state = State.READY;
                } catch (Exception ex) {
                    entry.state = State.FAILED;
                }
            });
        } catch (Exception ex) {
            entry.state = State.FAILED;
        }
    }

    private static BufferedImage decode(byte[] bytes) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                long w = reader.getWidth(0);
                long h = reader.getHeight(0);
                if (w <= 0 || h <= 0 || w * h > MAX_PIXELS) {
                    return null;
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
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
