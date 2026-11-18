package com.stagelite.network;

import com.stagelite.config.StageLiteConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.network.PacketDistributor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Imagenes locales de los hologramas, guardadas en la carpeta del mundo (stagelite_images/).
 * Solo el staff puede subir; cualquier jugador puede pedirlas (las necesita para verlas).
 * Todo corre en el hilo del servidor.
 */
public final class ImageStore {
    private static final int MAX_FILES = 500;

    private static final class Upload {
        final String hash;
        final byte[][] parts;
        int received;
        long bytes;

        Upload(String hash, int total) {
            this.hash = hash;
            this.parts = new byte[total][];
        }
    }

    private static final Map<UUID, Upload> UPLOADS = new HashMap<>();
    private static final Map<String, Long> LAST_REQUEST = new HashMap<>();

    private ImageStore() {
    }

    private static Path dir(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("stagelite_images");
    }

    public static void onUpload(ServerPlayer player, String hash, int index, int total, byte[] data) {
        if (!StageLiteConfig.canEdit(player) || !ImageIds.validHash(hash)) {
            return;
        }
        long max = StageLiteConfig.MAX_IMAGE_KB.get() * 1024L;
        int maxParts = (int) (max / ImageIds.CHUNK) + 1;
        if (total < 1 || total > maxParts || index < 0 || index >= total || data.length > ImageIds.CHUNK) {
            UPLOADS.remove(player.getUUID());
            return;
        }
        Upload up = UPLOADS.get(player.getUUID());
        if (index == 0 || up == null || !up.hash.equals(hash) || up.parts.length != total) {
            if (index != 0) {
                return;
            }
            up = new Upload(hash, total);
            UPLOADS.put(player.getUUID(), up);
        }
        if (up.parts[index] != null) {
            return;
        }
        up.parts[index] = data;
        up.received++;
        up.bytes += data.length;
        if (up.bytes > max) {
            UPLOADS.remove(player.getUUID());
            return;
        }
        if (up.received < total) {
            return;
        }
        UPLOADS.remove(player.getUUID());
        byte[] all = new byte[(int) up.bytes];
        int pos = 0;
        for (byte[] part : up.parts) {
            System.arraycopy(part, 0, all, pos, part.length);
            pos += part.length;
        }
        if (!ImageIds.isImage(all) || !ImageIds.hash(all).equals(hash)) {
            return;
        }
        try {
            Path d = dir(player.getServer());
            Files.createDirectories(d);
            Path file = d.resolve(hash);
            if (!Files.exists(file)) {
                try (var list = Files.list(d)) {
                    if (list.count() >= MAX_FILES) {
                        return;
                    }
                }
                Files.write(file, all);
            }
        } catch (IOException ignored) {
            // no se pudo guardar: los demas jugadores veran el holograma como "error"
        }
    }

    public static void onRequest(ServerPlayer player, String hash) {
        if (!ImageIds.validHash(hash)) {
            return;
        }
        String key = player.getUUID() + hash;
        long now = System.currentTimeMillis();
        Long last = LAST_REQUEST.get(key);
        if (last != null && now - last < 5000) {
            return;
        }
        if (LAST_REQUEST.size() > 5000) {
            LAST_REQUEST.clear();
        }
        LAST_REQUEST.put(key, now);

        byte[] bytes = null;
        try {
            Path file = dir(player.getServer()).resolve(hash);
            if (Files.isRegularFile(file) && Files.size(file) <= StageLiteConfig.MAX_IMAGE_KB.get() * 1024L) {
                bytes = Files.readAllBytes(file);
            }
        } catch (IOException ignored) {
            bytes = null;
        }
        if (bytes == null) {
            send(player, new ImageChunkPacket(hash, 0, 0, new byte[0]));
            return;
        }
        int total = (bytes.length + ImageIds.CHUNK - 1) / ImageIds.CHUNK;
        for (int i = 0; i < total; i++) {
            int from = i * ImageIds.CHUNK;
            int to = Math.min(bytes.length, from + ImageIds.CHUNK);
            send(player, new ImageChunkPacket(hash, i, total, java.util.Arrays.copyOfRange(bytes, from, to)));
        }
    }

    private static void send(ServerPlayer player, ImageChunkPacket packet) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void clear() {
        UPLOADS.clear();
        LAST_REQUEST.clear();
    }
}
