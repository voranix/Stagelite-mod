package com.stagelite.light;

import com.stagelite.blockentity.StageLightBlockEntity;

import java.util.function.Consumer;

/** Cambios a todas las luces cargadas de un grupo (los usa la consola). */
public final class GroupControl {
    private GroupControl() {
    }

    public static int edit(int group, Consumer<LightData> edit) {
        int count = 0;
        for (StageLightBlockEntity be : LightRegistry.all()) {
            if (be.data.group == group) {
                edit.accept(be.data);
                be.data.clamp();
                be.sync();
                count++;
            }
        }
        return count;
    }

    public static int editAll(Consumer<LightData> edit) {
        int count = 0;
        for (StageLightBlockEntity be : LightRegistry.all()) {
            edit.accept(be.data);
            be.data.clamp();
            be.sync();
            count++;
        }
        return count;
    }
}
