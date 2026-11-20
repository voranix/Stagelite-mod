package com.stagelite.data;

import com.stagelite.light.LightData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Escenas y shows guardados con el mundo.
 *
 * Escena = el "look" de cada grupo (color, intensidad, efecto, movimiento...).
 * Show   = lista de escenas con la duracion de cada una.
 */
public class StageData extends SavedData {
    private static final String NAME = "stagelite_data";

    public record ShowStep(String scene, double seconds) {
    }

    public final Map<String, Map<Integer, LightData>> scenes = new LinkedHashMap<>();
    public final Map<String, List<ShowStep>> shows = new LinkedHashMap<>();
    /** Nombres opcionales de los grupos (para no olvidar que es cada uno). */
    public final Map<Integer, String> groupNames = new TreeMap<>();

    /** Deja solo caracteres imprimibles, sin espacios de sobra, maximo 24. Vacio = sin nombre. */
    public static String cleanName(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (!Character.isISOControl(c) && c != '\u00a7') {
                sb.append(c);
            }
        }
        String t = sb.toString().trim();
        return t.length() > 24 ? t.substring(0, 24).trim() : t;
    }

    public void setGroupName(int group, String name) {
        String n = cleanName(name);
        if (n.isEmpty()) {
            groupNames.remove(group);
        } else {
            groupNames.put(group, n);
        }
        setDirty();
    }

    public static StageData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(StageData::load, StageData::new, NAME);
    }

    public static StageData load(CompoundTag root) {
        StageData data = new StageData();

        ListTag sceneList = root.getList("scenes", Tag.TAG_COMPOUND);
        for (int i = 0; i < sceneList.size(); i++) {
            CompoundTag sceneTag = sceneList.getCompound(i);
            Map<Integer, LightData> looks = new TreeMap<>();
            ListTag lookList = sceneTag.getList("looks", Tag.TAG_COMPOUND);
            for (int j = 0; j < lookList.size(); j++) {
                CompoundTag lookTag = lookList.getCompound(j);
                looks.put(lookTag.getInt("group"), LightData.fromTag(lookTag.getCompound("data")));
            }
            data.scenes.put(sceneTag.getString("name"), looks);
        }

        ListTag nameList = root.getList("groupNames", Tag.TAG_COMPOUND);
        for (int i = 0; i < nameList.size(); i++) {
            CompoundTag nt = nameList.getCompound(i);
            String n = cleanName(nt.getString("name"));
            if (!n.isEmpty()) {
                data.groupNames.put(nt.getInt("group"), n);
            }
        }

        ListTag showList = root.getList("shows", Tag.TAG_COMPOUND);
        for (int i = 0; i < showList.size(); i++) {
            CompoundTag showTag = showList.getCompound(i);
            List<ShowStep> steps = new ArrayList<>();
            ListTag stepList = showTag.getList("steps", Tag.TAG_COMPOUND);
            for (int j = 0; j < stepList.size(); j++) {
                CompoundTag stepTag = stepList.getCompound(j);
                steps.add(new ShowStep(stepTag.getString("scene"), stepTag.getDouble("seconds")));
            }
            data.shows.put(showTag.getString("name"), steps);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        ListTag sceneList = new ListTag();
        for (Map.Entry<String, Map<Integer, LightData>> scene : scenes.entrySet()) {
            CompoundTag sceneTag = new CompoundTag();
            sceneTag.putString("name", scene.getKey());
            ListTag lookList = new ListTag();
            for (Map.Entry<Integer, LightData> look : scene.getValue().entrySet()) {
                CompoundTag lookTag = new CompoundTag();
                lookTag.putInt("group", look.getKey());
                lookTag.put("data", look.getValue().toTag());
                lookList.add(lookTag);
            }
            sceneTag.put("looks", lookList);
            sceneList.add(sceneTag);
        }
        root.put("scenes", sceneList);

        ListTag showList = new ListTag();
        for (Map.Entry<String, List<ShowStep>> show : shows.entrySet()) {
            CompoundTag showTag = new CompoundTag();
            showTag.putString("name", show.getKey());
            ListTag stepList = new ListTag();
            for (ShowStep step : show.getValue()) {
                CompoundTag stepTag = new CompoundTag();
                stepTag.putString("scene", step.scene());
                stepTag.putDouble("seconds", step.seconds());
                stepList.add(stepTag);
            }
            showTag.put("steps", stepList);
            showList.add(showTag);
        }
        root.put("shows", showList);

        ListTag nameList = new ListTag();
        for (Map.Entry<Integer, String> e : groupNames.entrySet()) {
            CompoundTag nt = new CompoundTag();
            nt.putInt("group", e.getKey());
            nt.putString("name", e.getValue());
            nameList.add(nt);
        }
        root.put("groupNames", nameList);
        return root;
    }
}
