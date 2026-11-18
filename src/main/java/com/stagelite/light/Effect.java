package com.stagelite.light;

import java.util.Locale;

/** Efecto de color/brillo de una luz. */
public enum Effect {
    STATIC("static"),
    PULSE("pulse"),
    STROBE("strobe"),
    RAINBOW("rainbow");

    private final String id;

    Effect(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** Clave de traduccion. */
    public String key() {
        return "effect.stagelite." + id;
    }

    public Effect next() {
        Effect[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public static Effect byId(int index) {
        Effect[] all = values();
        return all[Math.floorMod(index, all.length)];
    }

    /** Devuelve null si el nombre no existe. */
    public static Effect byName(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        for (Effect e : values()) {
            if (e.id.equals(n)) {
                return e;
            }
        }
        return null;
    }
}
