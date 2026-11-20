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
