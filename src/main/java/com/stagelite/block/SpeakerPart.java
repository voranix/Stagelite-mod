package com.stagelite.block;

import com.stagelite.light.FixtureType;
import net.minecraft.util.StringRepresentable;

/** Piezas (todas menos la de control) de los parlantes de varios bloques. x hacia la derecha (visto de frente), y hacia arriba. */
public enum SpeakerPart implements StringRepresentable {
    M_1_0(FixtureType.SPEAKER_M, "m", 1, 0), M_0_1(FixtureType.SPEAKER_M, "m", 0, 1), M_1_1(FixtureType.SPEAKER_M, "m", 1, 1),
    L_1_0(FixtureType.SPEAKER_L, "l", 1, 0), L_2_0(FixtureType.SPEAKER_L, "l", 2, 0), L_3_0(FixtureType.SPEAKER_L, "l", 3, 0),
    L_0_1(FixtureType.SPEAKER_L, "l", 0, 1), L_1_1(FixtureType.SPEAKER_L, "l", 1, 1), L_2_1(FixtureType.SPEAKER_L, "l", 2, 1), L_3_1(FixtureType.SPEAKER_L, "l", 3, 1),
    L_0_2(FixtureType.SPEAKER_L, "l", 0, 2), L_1_2(FixtureType.SPEAKER_L, "l", 1, 2), L_2_2(FixtureType.SPEAKER_L, "l", 2, 2), L_3_2(FixtureType.SPEAKER_L, "l", 3, 2);

    public final FixtureType type;
    public final int x;
    public final int y;
    private final String id;

    SpeakerPart(FixtureType type, String size, int x, int y) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.id = size + "_" + x + "_" + y;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static SpeakerPart of(FixtureType type, int x, int y) {
        for (SpeakerPart p : values()) {
            if (p.type == type && p.x == x && p.y == y) {
                return p;
            }
        }
        return null;
    }
}
