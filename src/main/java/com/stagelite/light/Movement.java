package com.stagelite.light;

import java.util.Locale;

/** Tipo de movimiento del haz (como una cabeza movil). */
public enum Movement {
    NONE("none"),
    SWEEP("sweep"),
    NOD("nod"),
    CIRCLE("circle"),
    EIGHT("eight"),
    /** Giro continuo de 360 grados. */
    SPIN("spin");

    private final String id;

    Movement(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** Clave de traduccion. */
    public String key() {
        return "movement.stagelite." + id;
    }

    public Movement next() {
        Movement[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public static Movement byId(int index) {
        Movement[] all = values();
        return all[Math.floorMod(index, all.length)];
    }

    /** Devuelve null si el nombre no existe. */
    public static Movement byName(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        for (Movement m : values()) {
            if (m.id.equals(n)) {
                return m;
            }
        }
        return null;
    }
}
