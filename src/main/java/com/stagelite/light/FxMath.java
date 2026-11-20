package com.stagelite.light;

/**
 * Seno y coseno en precision doble. Mth.sin/Mth.cos usan una tabla y, cuando el angulo es muy grande
 * (mundos con muchas horas de juego), devuelven siempre ~0: los efectos animados se rompian.
 */
public final class FxMath {
    private FxMath() {
    }

    public static float sin(float radians) {
        return (float) Math.sin(radians % (Math.PI * 2.0));
    }

    public static float cos(float radians) {
        return (float) Math.cos(radians % (Math.PI * 2.0));
    }
}
