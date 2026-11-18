package com.stagelite.config;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeConfigSpec;

/** Configuracion comun (servidor). */
public final class StageLiteConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue PERMISSION_LEVEL;
    public static final ForgeConfigSpec.BooleanValue FIREWORKS_ENABLED;
    public static final ForgeConfigSpec.IntValue MAX_IMAGE_KB;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        PERMISSION_LEVEL = b
                .comment("Nivel de permiso necesario para editar luces y usar /stagelite.",
                        "2 = operadores (por defecto). Puedes darle OP a tu staff o usar un plugin de permisos que lo mapee.")
                .defineInRange("permissionLevel", 2, 0, 4);
        FIREWORKS_ENABLED = b
                .comment("Permite que los lanzadores de fuegos artificiales disparen cohetes reales (hacen dano cerca de la explosion).")
                .define("fireworksEnabled", true);
        MAX_IMAGE_KB = b
                .comment("Tamano maximo (en KB) de las imagenes locales que el staff sube al holograma. Se guardan en la carpeta stagelite_images del mundo.")
                .defineInRange("maxImageKB", 2048, 64, 8192);
        SPEC = b.build();
    }

    private StageLiteConfig() {
    }

    public static boolean canEdit(Player player) {
        return player.hasPermissions(PERMISSION_LEVEL.get());
    }
}
