package com.stagelite.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Configuracion del cliente (rendimiento visual). */
public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue RENDER_DISTANCE;
    public static final ForgeConfigSpec.IntValue BEAM_SIDES;
    public static final ForgeConfigSpec.BooleanValue HOLOGRAMS_ENABLED;

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
        SPEC = b.build();
    }

    private ClientConfig() {
    }
}
