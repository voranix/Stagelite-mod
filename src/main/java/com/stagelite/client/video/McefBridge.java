package com.stagelite.client.video;

import java.lang.reflect.Method;

/**
 * Puente por reflexion con MCEF (com.cinemamod.mcef). Asi StageLite compila y funciona sin MCEF:
 * si no esta instalado, las pantallas simplemente no reproducen.
 */
final class McefBridge {
    private static Boolean available;
    private static Class<?> mcef;

    private McefBridge() {
    }

    static boolean available() {
        if (available == null) {
            try {
                mcef = Class.forName("com.cinemamod.mcef.MCEF");
            } catch (Throwable t) {
                available = Boolean.FALSE;
                return false;
            }
        }
        if (Boolean.FALSE.equals(available)) {
            return false;
        }
        try {
            Method init = mcef.getMethod("isInitialized");
            if (!Boolean.TRUE.equals(init.invoke(null))) {
                return false; // todavia descargando Chromium: se vuelve a preguntar mas tarde
            }
        } catch (NoSuchMethodException ignored) {
            // versiones sin isInitialized
        } catch (Throwable t) {
            return false;
        }
        available = Boolean.TRUE;
        return true;
    }

    /** Crea un navegador sin fondo transparente. Devuelve null si falla. */
    static Object create(String url, int w, int h) {
        try {
            Object browser;
            try {
                browser = mcef.getMethod("createBrowser", String.class, boolean.class).invoke(null, url, false);
            } catch (NoSuchMethodException e) {
                browser = mcef.getMethod("createBrowser", String.class, boolean.class, int.class, int.class)
                        .invoke(null, url, false, w, h);
            }
            resize(browser, w, h);
            return browser;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Method method(Object o, String name, Class<?>... args) throws NoSuchMethodException {
        return o.getClass().getMethod(name, args);
    }

    static void resize(Object b, int w, int h) {
        try {
            method(b, "resize", int.class, int.class).invoke(b, w, h);
        } catch (Throwable ignored) {
        }
    }

    static int textureId(Object b) {
        try {
            Object renderer = method(b, "getRenderer").invoke(b);
            return (Integer) renderer.getClass().getMethod("getTextureID").invoke(renderer);
        } catch (Throwable t) {
            return -1;
        }
    }

    static void js(Object b, String code) {
        try {
            method(b, "executeJavaScript", String.class, String.class, int.class).invoke(b, code, "stagelite", 0);
        } catch (Throwable ignored) {
        }
    }

    static void load(Object b, String url) {
        try {
            method(b, "loadURL", String.class).invoke(b, url);
        } catch (Throwable ignored) {
        }
    }

    /** Un clic simulado: Chromium lo cuenta como gesto del usuario y deja reproducir con sonido. */
    static void click(Object b, int x, int y) {
        try {
            method(b, "sendMousePress", int.class, int.class, int.class).invoke(b, x, y, 0);
            method(b, "sendMouseRelease", int.class, int.class, int.class).invoke(b, x, y, 0);
        } catch (Throwable ignored) {
        }
    }

    static void close(Object b) {
        try {
            method(b, "close").invoke(b);
        } catch (Throwable ignored) {
        }
    }
}
