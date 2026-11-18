package com.stagelite.light;

/** Tipos de luz. Cada uno es un bloque distinto con su propio modelo y valores por defecto. */
public enum FixtureType {
    /** Foco tipo par en horquilla. */
    SPOT("spot_light", 0.26f, 0.10f),
    /** Cabeza movil: la cabeza gira de verdad (se dibuja en el renderer). */
    MOVING_HEAD("moving_head", 0.28f, 0.10f),
    /** Panel LED de luz ancha. */
    LED_PANEL("led_panel", -0.19f, 0.38f),
    /** Estroboscopio. */
    STROBE("strobe_light", -0.12f, 0.30f);

    private final String id;
    private final float beamStart;
    private final float beamRadius;

    FixtureType(String id, float beamStart, float beamRadius) {
        this.id = id;
        this.beamStart = beamStart;
        this.beamRadius = beamRadius;
    }

    public String id() {
        return id;
    }

    /** Distancia desde el centro del bloque (hacia donde mira) donde empieza el haz. */
    public float beamStart() {
        return beamStart;
    }

    /** Radio del haz en su origen. */
    public float beamRadius() {
        return beamRadius;
    }

    public boolean moves() {
        return this == MOVING_HEAD;
    }

    public static FixtureType byId(int index) {
        FixtureType[] all = values();
        return all[Math.floorMod(index, all.length)];
    }

    /** Valores iniciales al colocar la luz. */
    public void applyDefaults(LightData d) {
        switch (this) {
            case SPOT:
                d.spread = 12;
                d.length = 24;
                break;
            case MOVING_HEAD:
                d.spread = 8;
                d.length = 32;
                d.movement = Movement.SWEEP;
                d.range = 40;
                d.speed = 5;
                break;
            case LED_PANEL:
                d.spread = 28;
                d.length = 10;
                break;
            case STROBE:
                d.spread = 30;
                d.length = 16;
                d.effect = Effect.STROBE;
                d.speed = 6;
                break;
            default:
                break;
        }
    }
}
