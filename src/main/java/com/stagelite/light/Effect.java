package com.stagelite.light;

import java.util.Locale;

/** Efecto de color/brillo de una luz. */
public enum Effect {
    STATIC("static"),
    PULSE("pulse"),
    STROBE("strobe"),
    RAINBOW("rainbow"),
    // Patrones de la pista de baile (usan la posicion de cada baldosa para formar figuras)
    WAVE("wave"),
    RIPPLE("ripple"),
    CHECKER("checker"),
    SPIRAL("spiral"),
    STRIPES("stripes"),
    SPARKLE("sparkle"),
    /** Pulso seco al compas (BPM segun la velocidad), igual en todas las luces: sirve para ir al ritmo de la musica. */
    BEAT("beat");

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

    /** Solo tiene sentido en la pista de baile. */
    public boolean floorOnly() {
        return ordinal() > RAINBOW.ordinal() && this != BEAT;
    }

    /** Siguiente efecto; si floor es false se saltan los patrones de pista. */
    public Effect next(boolean floor) {
        Effect e = this;
        do {
            e = e.next();
        } while (!floor && e.floorOnly());
        return e;
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
