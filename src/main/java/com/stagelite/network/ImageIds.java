package com.stagelite.network;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

/** Utilidades comunes para las imagenes locales del holograma (se identifican por el hash de su contenido). */
public final class ImageIds {
    /** Tamano de cada trozo enviado por red (el limite de Forge para paquetes al servidor es ~32 KB). */
    public static final int CHUNK = 30000;
    /** Maximo que el cliente intenta subir (el servidor aplica su propio limite en la config). */
    public static final int MAX_CLIENT_BYTES = 2 * 1024 * 1024;
    public static final String PREFIX = "local:";
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{24}");

    private ImageIds() {
    }

    public static boolean validHash(String s) {
        return s != null && HASH.matcher(s).matches();
    }

    /** Primeros 12 bytes del SHA-256, en hexadecimal (24 caracteres). */
    public static String hash(byte[] data) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 12; i++) {
                sb.append(String.format("%02x", d[i] & 255));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** PNG, JPG, GIF o BMP segun los primeros bytes. */
    public static boolean isImage(byte[] b) {
        if (b == null || b.length < 12) {
            return false;
        }
        boolean png = (b[0] & 255) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
        boolean jpg = (b[0] & 255) == 0xFF && (b[1] & 255) == 0xD8 && (b[2] & 255) == 0xFF;
        boolean gif = b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8';
        boolean bmp = b[0] == 'B' && b[1] == 'M';
        return png || jpg || gif || bmp;
    }

    @SuppressWarnings("unused")
    private static final java.nio.charset.Charset UNUSED = StandardCharsets.UTF_8;
}
