package com.stagelite.config;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeConfigSpec;

/** Configuracion comun (servidor). */
public final class StageLiteConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue PERMISSION_LEVEL;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        PERMISSION_LEVEL = b
                .comment("Nivel de permiso necesario para editar luces y usar /stagelite.",
                        "2 = operadores (por defecto). Puedes darle OP a tu staff o usar un plugin de permisos que lo mapee.")
                .defineInRange("permissionLevel", 2, 0, 4);
        SPEC = b.build();
    }

    private StageLiteConfig() {
    }

    public static boolean canEdit(Player player) {
        return player.hasPermissions(PERMISSION_LEVEL.get());
    }
}
