package com.stagelite.light;

import com.stagelite.blockentity.StageLightBlockEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lista de luces cargadas en el servidor (solo las que estan en chunks cargados).
 * Los comandos y los shows trabajan sobre esta lista.
 */
public final class LightRegistry {
    private static final Map<ResourceKey<Level>, Set<StageLightBlockEntity>> LIGHTS = new ConcurrentHashMap<>();

    private static final Set<StageLightBlockEntity> MICS = ConcurrentHashMap.newKeySet();
    private static final Set<StageLightBlockEntity> SPEAKERS = ConcurrentHashMap.newKeySet();

    public static Set<StageLightBlockEntity> mics() {
        return MICS;
    }

    public static Set<StageLightBlockEntity> speakers() {
        return SPEAKERS;
    }

    private LightRegistry() {
    }

    public static void add(StageLightBlockEntity be) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        LIGHTS.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(be);
        FixtureType t = be.type();
        if (t == FixtureType.MIC) {
            MICS.add(be);
        } else if (t.isSpeaker()) {
            SPEAKERS.add(be);
        }
    }

    public static void remove(StageLightBlockEntity be) {
        MICS.remove(be);
        SPEAKERS.remove(be);
        for (Set<StageLightBlockEntity> set : LIGHTS.values()) {
            set.remove(be);
        }
    }

    /** Copia de todas las luces cargadas, de todas las dimensiones. */
    public static List<StageLightBlockEntity> all() {
        List<StageLightBlockEntity> out = new ArrayList<>();
        for (Set<StageLightBlockEntity> set : LIGHTS.values()) {
            for (StageLightBlockEntity be : set) {
                if (!be.isRemoved()) {
                    out.add(be);
                }
            }
        }
        return out;
    }

    public static void clear() {
        MICS.clear();
        SPEAKERS.clear();
        LIGHTS.clear();
    }
}
