package com.stagelite.light;

import net.minecraft.util.Mth;

/**
 * Calculo de efectos a partir del tiempo (en ticks). Todo es determinista, asi el cliente
 * puede dibujar la animacion sin recibir nada de la red mientras no cambie la configuracion.
 */
public final class LightFx {
    private LightFx() {
    }

    /** Multiplicador de brillo 0..1 segun el efecto. */
    public static float brightness(LightData d, float ticks) {
        float sec = ticks / 20f;
        switch (d.effect) {
            case PULSE: {
                float phase = sec * d.speed * 0.2f;
                return 0.15f + 0.85f * (0.5f + 0.5f * FxMath.sin(phase * Mth.TWO_PI));
            }
            case BEAT: {
                // velocidad 1..10 -> 70..160 BPM
                double beats = sec * (60 + d.speed * 10) / 60.0;
                float frac = (float) (beats - Math.floor(beats));
                return 0.10f + 0.90f * (float) Math.exp(-4.0 * frac);
            }
            case STROBE: {
                float phase = sec * d.speed * 1.5f;
                float frac = phase - (float) Math.floor(phase);
                return frac < 0.25f ? 1f : 0f;
            }
            default:
                return 1f;
        }
    }

    /** Color RGB final (0xRRGGBB) segun el efecto. */
    public static int color(LightData d, float ticks) {
        if (d.effect == Effect.RAINBOW) {
            float phase = ticks / 20f * d.speed * 0.1f;
            float hue = phase - (float) Math.floor(phase);
            return Mth.hsvToRgb(hue, 1f, 1f) & 0xFFFFFF;
        }
        return d.color & 0xFFFFFF;
    }

    private static float frac(double v) {
        return (float) (v - Math.floor(v));
    }

    private static float wave01(double cycles) {
        return 0.5f + 0.5f * FxMath.sin((float) ((cycles - Math.floor(cycles)) * Math.PI * 2.0));
    }

    /** Cambia el tono de un color RGB (shift 0..1). */
    public static int hueShift(int rgb, float shift) {
        float r = (rgb >> 16 & 255) / 255f;
        float g = (rgb >> 8 & 255) / 255f;
        float b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float h = 0f;
        if (delta > 1e-5f) {
            if (max == r) {
                h = ((g - b) / delta) / 6f;
            } else if (max == g) {
                h = (2f + (b - r) / delta) / 6f;
            } else {
                h = (4f + (r - g) / delta) / 6f;
            }
        }
        float s = max <= 0f ? 0f : delta / max;
        return Mth.hsvToRgb(frac(h + shift), s, max) & 0xFFFFFF;
    }

    private static int scale(int rgb, float k) {
        k = Mth.clamp(k, 0f, 1f);
        int r = (int) ((rgb >> 16 & 255) * k);
        int g = (int) ((rgb >> 8 & 255) * k);
        int b = (int) ((rgb & 255) * k);
        return (r << 16) | (g << 8) | b;
    }

    /**
     * Color final de una baldosa de pista (ya multiplicado por brillo e intensidad).
     * (bx,bz) = posicion del bloque; (cx,cz) = centro de la pista conectada.
     */
    public static int floorColor(LightData d, int bx, int bz, float cx, float cz, float ticks) {
        float intensity = d.intensity / 100f;
        if (!d.effect.floorOnly()) {
            return scale(color(d, ticks), brightness(d, ticks) * intensity);
        }
        double ph = (ticks / 20.0) * d.speed * 0.25;
        float u = bx + 0.5f - cx;
        float v = bz + 0.5f - cz;
        int base = d.color & 0xFFFFFF;
        switch (d.effect) {
            case WAVE:
                return scale(base, (0.08f + 0.92f * wave01(ph - u / 5.0)) * intensity);
            case RIPPLE: {
                float dist = (float) Math.sqrt(u * u + v * v);
                float w = wave01(ph - dist / 2.5);
                return scale(base, (0.06f + 0.94f * w * w) * intensity);
            }
            case CHECKER: {
                int par = Math.floorMod(bx + bz + (int) Math.floor(ph * 2.0), 2);
                return scale(par == 0 ? base : hueShift(base, 0.5f), intensity);
            }
            case SPIRAL: {
                float dist = (float) Math.sqrt(u * u + v * v);
                float ang = (float) (Math.atan2(v, u) / (Math.PI * 2.0));
                return scale(Mth.hsvToRgb(frac(ang + dist * 0.08f - ph * 0.5), 1f, 1f) & 0xFFFFFF, intensity);
            }
            case STRIPES:
                return scale(Mth.hsvToRgb(frac((bx + bz) * 0.07 - ph * 0.5), 1f, 1f) & 0xFFFFFF, intensity);
            case SPARKLE: {
                int h = bx * 73856093 ^ bz * 19349663 ^ (int) Math.floor(ph * 4.0) * 83492791;
                h ^= h >>> 13;
                h *= 0x5bd1e995;
                h ^= h >>> 15;
                boolean lit = (h & 0xFF) < 80;
                int rgb = Mth.hsvToRgb(((h >>> 8) & 0xFF) / 255f, 1f, 1f) & 0xFFFFFF;
                return scale(lit ? rgb : base, (lit ? 1f : 0.05f) * intensity);
            }
            default:
                return scale(base, intensity);
        }
    }

    private static float movePhase(LightData d, float ticks) {
        double cycles = (ticks / 20.0) * d.moveSpeed * 0.08;
        return (float) ((cycles - Math.floor(cycles)) * Math.PI * 2.0);
    }

    /** Giro horizontal (grados) del movimiento. */
    public static float pan(LightData d, float ticks) {
        float p = movePhase(d, ticks);
        switch (d.movement) {
            case SWEEP:
                return d.range * FxMath.sin(p);
            case CIRCLE:
                return d.range * FxMath.cos(p);
            case EIGHT:
                return d.range * FxMath.sin(p);
            case SPIN:
                return (float) Math.toDegrees(p % (Math.PI * 2.0));
            default:
                return 0f;
        }
    }

    /** Inclinacion vertical (grados) del movimiento. */
    public static float tilt(LightData d, float ticks) {
        float p = movePhase(d, ticks);
        switch (d.movement) {
            case NOD:
                return d.range * FxMath.sin(p);
            case CIRCLE:
                return d.range * FxMath.sin(p);
            case EIGHT:
                return d.range * 0.5f * FxMath.sin(2f * p);
            default:
                return 0f;
        }
    }
}
