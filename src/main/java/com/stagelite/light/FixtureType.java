package com.stagelite.light;

import net.minecraft.util.Mth;

/** Tipos de dispositivo. Cada uno es un bloque distinto con su propio modelo y valores por defecto. */
public enum FixtureType {
    /** Foco tipo par en horquilla. */
    SPOT("spot_light", 0.26f, 0.10f),
    /** Cabeza movil: la cabeza gira de verdad (se dibuja en el renderer). */
    MOVING_HEAD("moving_head", 0.28f, 0.10f),
    /** Panel LED de luz ancha. */
    LED_PANEL("led_panel", -0.19f, 0.38f),
    /** Estroboscopio. */
    STROBE("strobe_light", -0.12f, 0.30f),
    /** Laser: abanico de rayos finos. */
    LASER("laser", 0.25f, 0.02f),
    /** Maquina de humo (particulas). */
    SMOKE("smoke_machine", 0.5f, 0f),
    /** Maquina de fuego (llamas visuales, no queman). */
    FIRE("fire_machine", 0.5f, 0f),
    /** Lanzador de fuegos artificiales. */
    FIREWORK("firework_launcher", 0.5f, 0f),
    /** Bola disco con reflejos. */
    DISCO("disco_ball", 0f, 0f),
    /** Holograma: muestra una imagen desde un link web. */
    HOLOGRAM("hologram", 0.5f, 0f),
    /** Baldosa de pista de baile: se une con las vecinas y forma patrones. */
    FLOOR("dance_floor", 0f, 0f),
    /** Parlante: reproduce musica y la voz de los microfonos de su grupo. */
    SPEAKER("speaker", 0f, 0f),
    /** Parlante mediano: 2 de ancho x 2 de alto. */
    SPEAKER_M("speaker_medium", 0f, 0f),
    /** Parlante grande: 4 de ancho x 3 de alto. */
    SPEAKER_L("speaker_large", 0f, 0f),
    /** Microfono: capta la voz de los jugadores cercanos (Simple Voice Chat) y la manda a los parlantes de su grupo. */
    MIC("microphone", 0f, 0f),
    /** Pantalla de video (MCEF): link a un video con sonido, volumen, velocidad y tamano. */
    SCREEN("video_screen", 0f, 0f),
    /** Modulo de pantalla: se une con los vecinos (mismo lado) hasta 24x24 y forma una sola pantalla grande. */
    WALL("video_wall", 0f, 0f),
    /** Monitor de TV: solo se coloca en paredes. */
    TV("video_tv", 0f, 0f),
    /** Marco / letrero multimedia: imagen o video pegado a la pared. */
    FRAME("video_frame", 0f, 0f),
    /** Proyector: proyecta la imagen o el video sobre la primera superficie que encuentra. */
    PROJECTOR("video_projector", 0f, 0f);

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

    /** Tiene horquilla y cabeza animada. */
    public boolean moves() {
        return this == MOVING_HEAD;
    }

    /** Admite movimiento (pan/tilt). */
    public boolean animates() {
        return hasBeam() || this == LASER;
    }

    /** Dibuja un cono de luz clasico. */
    public boolean hasBeam() {
        return this == SPOT || this == MOVING_HEAD || this == LED_PANEL || this == STROBE;
    }

    /** Admite color. */
    public boolean usesColor() {
        return this != FIRE && !isAudio() && !isVideo();
    }

    /** Usa los faders de ancho y largo del haz. */
    public boolean usesBeamSize() {
        return hasBeam() || this == LASER || this == DISCO;
    }

    /** Necesita ejecutarse cada tick (particulas, fuegos artificiales). */
    public boolean ticks() {
        return this == SMOKE || this == FIRE || this == FIREWORK || isSpeaker();
    }

    /** Clave del fader extra (null si no tiene). */
    public String extraKey() {
        switch (this) {
            case LASER:
                return "extra_laser";
            case SMOKE:
                return "extra_smoke";
            case FIRE:
                return "extra_fire";
            case FIREWORK:
                return "extra_firework";
            case DISCO:
                return "extra_disco";
            case HOLOGRAM:
                return "extra_hologram";
            case SPEAKER:
            case SPEAKER_M:
            case SPEAKER_L:
                return "extra_speaker";
            case SCREEN:
            case FRAME:
            case PROJECTOR:
                return "extra_screen";
            case MIC:
                return "extra_mic";
            default:
                return null;
        }
    }

    /**
     * true = el modelo fijo del bloque se oculta y el renderer lo dibuja girado (luces fijas con inclinacion,
     * giro o movimiento automatico). El laser mueve solo su abanico, no su cuerpo.
     */
    public boolean bodyTilted(LightData d) {
        if (!tiltable() || moves()) {
            return false;
        }
        return d.tilt != 0 || d.yaw != 0 || (d.movement != Movement.NONE && this != LASER);
    }

    /** Admite inclinacion fija. */
    public boolean tiltable() {
        return hasBeam() || this == LASER;
    }

    /** Tiene boton de proporcion y tamaño propio (los modulos y la TV ya fijan su forma). */
    public boolean usesAspect() {
        return isVideo() && this != WALL && this != TV;
    }

    public boolean isVideo() {
        return this == SCREEN || this == FRAME || this == PROJECTOR || this == WALL || this == TV;
    }

    public boolean isAudio() {
        return isSpeaker() || this == MIC;
    }

    public boolean isSpeaker() {
        return this == SPEAKER || this == SPEAKER_M || this == SPEAKER_L;
    }

    /** Ancho en bloques (parlantes grandes). */
    public int partsW() {
        return this == SPEAKER_M ? 2 : this == SPEAKER_L ? 4 : 1;
    }

    /** Alto en bloques (parlantes grandes). */
    public int partsH() {
        return this == SPEAKER_M ? 2 : this == SPEAKER_L ? 3 : 1;
    }

    /** Cuanto suma al audio de una pantalla conectada (los grandes suenan mas fuerte). */
    public double audioGain() {
        return this == SPEAKER_M ? 1.5 : this == SPEAKER_L ? 2.0 : 1.0;
    }

    public boolean isFloor() {
        return this == FLOOR;
    }

    public boolean usesUrl() {
        return this == HOLOGRAM || isVideo();
    }

    public static FixtureType byId(int index) {
        FixtureType[] all = values();
        return all[Math.floorMod(index, all.length)];
    }

    /** Nivel de luz de bloque (0..15) que emite este dispositivo con su estado actual. */
    public int lightLevel(LightData d) {
        if (!d.on) {
            return 0;
        }
        switch (this) {
            case SMOKE:
            case FIREWORK:
            case SPEAKER:
            case SPEAKER_M:
            case SPEAKER_L:
            case MIC:
            case SCREEN:
            case FRAME:
            case PROJECTOR:
            case WALL:
            case TV:
                return 0;
            case FIRE:
                return 12;
            case DISCO:
                return Mth.ceil(d.intensity * 8 / 100f);
            case HOLOGRAM:
                return Mth.ceil(d.intensity * 6 / 100f);
            case FLOOR:
                return Mth.ceil(d.intensity * 9 / 100f);
            case LASER:
                return Mth.ceil(d.intensity * 5 / 100f);
            default:
                return d.blockLightLevel();
        }
    }

    /** Valores iniciales al colocar el dispositivo. */
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
            case LASER:
                d.color = 0x00FF40;
                d.spread = 20;
                d.length = 40;
                d.extra = 5;
                d.movement = Movement.SWEEP;
                d.range = 30;
                d.speed = 4;
                break;
            case SMOKE:
                d.color = 0xFFFFFF;
                d.extra = 5;
                d.intensity = 60;
                break;
            case FIRE:
                d.extra = 5;
                d.intensity = 100;
                break;
            case FIREWORK:
                d.color = 0xFF3030;
                d.extra = 2;
                d.speed = 3;
                break;
            case DISCO:
                d.spread = 25;
                d.length = 12;
                d.extra = 5;
                d.speed = 3;
                d.effect = Effect.RAINBOW;
                break;
            case FRAME:
                d.intensity = 70;
                d.speed = 4;
                d.length = 16;
                d.extra = 2;
                d.billboard = true;
                break;
            case PROJECTOR:
                d.intensity = 70;
                d.speed = 4;
                d.length = 32;
                d.range = 30; // distancia de proyeccion
                d.extra = 4;
                d.billboard = true;
                break;
            case WALL:
                d.intensity = 70;
                d.speed = 4;
                d.length = 32;
                d.billboard = true;
                break;
            case TV:
                d.intensity = 70;
                d.speed = 4;
                d.length = 16;
                d.billboard = true;
                break;
            case SCREEN:
                d.intensity = 70;
                d.speed = 4;
                d.length = 24;
                d.extra = 3;
                d.billboard = true; // aqui significa "bucle"
                break;
            case SPEAKER_M:
                d.intensity = 80;
                d.extra = 7;
                break;
            case SPEAKER_L:
                d.intensity = 90;
                d.extra = 9;
                break;
            case SPEAKER:
                d.intensity = 80;
                d.extra = 5;
                break;
            case MIC:
                d.extra = 3;
                break;
            case FLOOR:
                d.color = 0xFF2090;
                d.effect = Effect.CHECKER;
                d.speed = 5;
                break;
            case HOLOGRAM:
                d.color = 0xFFFFFF;
                d.extra = 4;
                d.billboard = true;
                break;
            default:
                break;
        }
    }
}
