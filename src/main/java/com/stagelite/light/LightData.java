package com.stagelite.light;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

/**
 * Estado completo de una luz. Es la unica fuente de verdad: el servidor lo guarda
 * y el cliente lo usa para dibujar (efectos y movimiento se calculan con el tiempo
 * del juego, asi no hace falta enviar paquetes cada tick).
 */
public class LightData {
    public static final int MIN_LENGTH = 4;
    public static final int MAX_LENGTH = 64;

    public boolean on = true;
    /** Color RGB 0xRRGGBB. */
    public int color = 0xFFFFFF;
    /** 0..100 */
    public int intensity = 100;
    /** Semi-angulo del haz en grados, 1..40. */
    public int spread = 10;
    /** Largo del haz en bloques. */
    public int length = 24;
    public Effect effect = Effect.STATIC;
    public Movement movement = Movement.NONE;
    /** 1..10 */
    public int speed = 5;
    /** Amplitud del movimiento en grados, 0..90. */
    public int range = 40;
    /** 0 = sin grupo, 1..99 = grupo. */
    public int group = 0;
    /** Inclinacion fija en grados, -90..90 (positivo = hacia arriba). */
    public int tilt = 0;
    /** 1..10. Segun el dispositivo: rayos del laser, humo, altura del fuego, ritmo de fuegos, tamano del holograma... */
    public int extra = 5;
    /** Holograma: link directo a una imagen. */
    public String url = "";
    /** Holograma: true = siempre mira al jugador. */
    public boolean billboard = true;
    public static final int MAX_URL = 256;

    public void clamp() {
        color &= 0xFFFFFF;
        intensity = Mth.clamp(intensity, 0, 100);
        spread = Mth.clamp(spread, 1, 40);
        length = Mth.clamp(length, MIN_LENGTH, MAX_LENGTH);
        speed = Mth.clamp(speed, 1, 10);
        range = Mth.clamp(range, 0, 90);
        group = Mth.clamp(group, 0, 99);
        extra = Mth.clamp(extra, 1, 10);
        tilt = Mth.clamp(tilt, -90, 90);
        url = cleanUrl(url);
    }

    /** Solo http/https, sin espacios ni caracteres raros, maximo 256. Si no es valido queda vacio. */
    public static String cleanUrl(String u) {
        if (u == null) {
            return "";
        }
        u = u.trim();
        if (u.length() > MAX_URL) {
            return "";
        }
        if (u.isEmpty()) {
            return "";
        }
        String low = u.toLowerCase(java.util.Locale.ROOT);
        if (!(low.startsWith("http://") || low.startsWith("https://"))) {
            return "";
        }
        for (int i = 0; i < u.length(); i++) {
            char c = u.charAt(i);
            if (c <= 32 || c >= 127) {
                return "";
            }
        }
        return u;
    }

    public void copyFrom(LightData o) {
        copyLook(o);
        this.length = o.length;
        this.group = o.group;
        this.extra = o.extra;
        this.url = o.url;
        this.billboard = o.billboard;
    }

    /** Copia solo el "look" (lo que guarda una escena): no cambia el grupo ni el largo. */
    public void copyLook(LightData o) {
        this.on = o.on;
        this.color = o.color;
        this.intensity = o.intensity;
        this.spread = o.spread;
        this.effect = o.effect;
        this.movement = o.movement;
        this.speed = o.speed;
        this.range = o.range;
        this.tilt = o.tilt;
    }

    public CompoundTag toTag() {
        CompoundTag t = new CompoundTag();
        save(t);
        return t;
    }

    public void save(CompoundTag t) {
        t.putBoolean("on", on);
        t.putInt("color", color);
        t.putInt("intensity", intensity);
        t.putInt("spread", spread);
        t.putInt("length", length);
        t.putInt("effect", effect.ordinal());
        t.putInt("movement", movement.ordinal());
        t.putInt("speed", speed);
        t.putInt("range", range);
        t.putInt("group", group);
        t.putInt("extra", extra);
        t.putInt("tilt", tilt);
        t.putString("url", url);
        t.putBoolean("billboard", billboard);
    }

    public void load(CompoundTag t) {
        if (t.contains("on")) on = t.getBoolean("on");
        if (t.contains("color")) color = t.getInt("color");
        if (t.contains("intensity")) intensity = t.getInt("intensity");
        if (t.contains("spread")) spread = t.getInt("spread");
        if (t.contains("length")) length = t.getInt("length");
        if (t.contains("effect")) effect = Effect.byId(t.getInt("effect"));
        if (t.contains("movement")) movement = Movement.byId(t.getInt("movement"));
        if (t.contains("speed")) speed = t.getInt("speed");
        if (t.contains("range")) range = t.getInt("range");
        if (t.contains("group")) group = t.getInt("group");
        if (t.contains("extra")) extra = t.getInt("extra");
        if (t.contains("tilt")) tilt = t.getInt("tilt");
        if (t.contains("url")) url = t.getString("url");
        if (t.contains("billboard")) billboard = t.getBoolean("billboard");
        clamp();
    }

    public static LightData fromTag(CompoundTag t) {
        LightData d = new LightData();
        d.load(t);
        return d;
    }

    /** Nivel de luz de bloque (0..15) segun encendido e intensidad. */
    public int blockLightLevel() {
        return on ? Mth.ceil(intensity * 15 / 100f) : 0;
    }
}
