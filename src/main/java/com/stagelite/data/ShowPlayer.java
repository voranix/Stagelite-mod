package com.stagelite.data;

import com.stagelite.StageLite;
import com.stagelite.blockentity.StageLightBlockEntity;
import com.stagelite.light.LightData;
import com.stagelite.light.LightRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Reproduce un show (lista de escenas con tiempos). Solo hay uno activo a la vez. */
@Mod.EventBusSubscriber(modid = StageLite.MODID)
public final class ShowPlayer {
    private static List<StageData.ShowStep> steps;
    private static String currentName;
    private static int index;
    private static long ticksLeft;
    private static boolean loop;

    private ShowPlayer() {
    }

    /** Devuelve false si el show no existe o esta vacio. */
    public static boolean play(MinecraftServer server, String name, boolean loopIt) {
        List<StageData.ShowStep> list = StageData.get(server).shows.get(name);
        if (list == null || list.isEmpty()) {
            return false;
        }
        steps = new ArrayList<>(list);
        currentName = name;
        loop = loopIt;
        index = 0;
        startStep(server);
        return true;
    }

    public static void stop() {
        steps = null;
        currentName = null;
        index = 0;
        ticksLeft = 0;
        loop = false;
    }

    public static boolean isPlaying() {
        return steps != null;
    }

    public static String currentName() {
        return currentName;
    }

    /** Aplica el look de cada grupo de la escena a las luces cargadas. Devuelve cuantas luces cambio. */
    public static int applyScene(Map<Integer, LightData> looks) {
        int count = 0;
        for (StageLightBlockEntity be : LightRegistry.all()) {
            LightData look = looks.get(be.data.group);
            if (look != null) {
                be.data.copyLook(look);
                be.sync();
                count++;
            }
        }
        return count;
    }

    private static void startStep(MinecraftServer server) {
        StageData.ShowStep step = steps.get(index);
        Map<Integer, LightData> looks = StageData.get(server).scenes.get(step.scene());
        if (looks != null) {
            applyScene(looks);
        }
        ticksLeft = Math.max(1L, Math.round(step.seconds() * 20.0));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || steps == null) {
            return;
        }
        if (--ticksLeft > 0) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            stop();
            return;
        }
        index++;
        if (index >= steps.size()) {
            if (loop) {
                index = 0;
            } else {
                stop();
                return;
            }
        }
        startStep(server);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        stop();
        LightRegistry.clear();
    }
}
