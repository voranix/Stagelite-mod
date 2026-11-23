package com.stagelite.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Configuracion del cliente (rendimiento visual). */
public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue RENDER_DISTANCE;
    public static final ForgeConfigSpec.IntValue BEAM_SIDES;
    public static final ForgeConfigSpec.BooleanValue HOLOGRAMS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue VIDEOS_ENABLED;
    public static final ForgeConfigSpec.IntValue VIDEO_MAX_SCREENS;
    public static final ForgeConfigSpec.IntValue VIDEO_WIDTH;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        RENDER_DISTANCE = b
                .comment("Distancia maxima (en bloques) a la que se dibujan los haces de luz.")
                .defineInRange("beamRenderDistance", 96, 16, 256);
        BEAM_SIDES = b
                .comment("Calidad del haz: numero de lados del cono (menos = mas rapido, 3 a 12).")
                .defineInRange("beamSides", 10, 3, 12);
        HOLOGRAMS_ENABLED = b
                .comment("Descargar y mostrar las imagenes de los hologramas. Ponlo en false si no quieres que tu cliente se conecte a links externos.")
                .define("hologramsEnabled", true);
        VIDEOS_ENABLED = b
                .comment("Reproducir los videos de las pantallas (necesita el mod MCEF). Ponlo en false si no quieres que tu cliente cargue videos externos.")
                .define("videosEnabled", true);
        VIDEO_MAX_SCREENS = b
                .comment("Maximo de pantallas de video reproduciendose a la vez en tu cliente (las mas cercanas). Menos = menos lag.")
                .defineInRange("videoMaxScreens", 2, 1, 4);
        VIDEO_WIDTH = b
                .comment("Resolucion horizontal del video (el alto sale en 16:9). Menos = mas fluido.")
                .defineInRange("videoWidth", 854, 320, 1920);
        SPEC = b.build();
    }

    private ClientConfig() {
    }
}
