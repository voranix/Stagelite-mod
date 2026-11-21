package com.stagelite.block;

import net.minecraft.util.StringRepresentable;

/** Las 17 piezas del molinete 5x5 (x hacia el este, z hacia el sur, relativas al bloque central). */
public enum MolinetePart implements StringRepresentable {
    X0_Z0("x0_z0", 0, 0),
    X0_ZM1("x0_zm1", 0, -1),
    X0_ZM2("x0_zm2", 0, -2),
    X0_ZP1("x0_zp1", 0, 1),
    X0_ZP2("x0_zp2", 0, 2),
    XM1_Z0("xm1_z0", -1, 0),
    XM1_ZM1("xm1_zm1", -1, -1),
    XM1_ZP1("xm1_zp1", -1, 1),
    XM2_Z0("xm2_z0", -2, 0),
    XM2_ZM2("xm2_zm2", -2, -2),
    XM2_ZP2("xm2_zp2", -2, 2),
    XP1_Z0("xp1_z0", 1, 0),
    XP1_ZM1("xp1_zm1", 1, -1),
    XP1_ZP1("xp1_zp1", 1, 1),
    XP2_Z0("xp2_z0", 2, 0),
    XP2_ZM2("xp2_zm2", 2, -2),
    XP2_ZP2("xp2_zp2", 2, 2);

    private final String id;
    public final int dx;
    public final int dz;

    MolinetePart(String id, int dx, int dz) {
        this.id = id;
        this.dx = dx;
        this.dz = dz;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public boolean isCenter() {
        return dx == 0 && dz == 0;
    }

    /** Dentro del cuadrado central de 3x3 (la base). */
    public boolean isInner() {
        return Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
    }
}
