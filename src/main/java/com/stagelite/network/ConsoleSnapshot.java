package com.stagelite.network;

import com.stagelite.blockentity.StageLightBlockEntity;
import com.stagelite.data.ShowPlayer;
import com.stagelite.data.StageData;
import com.stagelite.light.FixtureType;
import com.stagelite.light.LightData;
import com.stagelite.light.LightRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Foto del estado de los grupos que se envia a la consola. */
public class ConsoleSnapshot {
    public static final class Group {
        public final int number;
        public final LightData look;
        public final int[] counts = new int[FixtureType.values().length];

        public Group(int number, LightData look) {
            this.number = number;
            this.look = look;
        }
    }

    public final List<Group> groups = new ArrayList<>();
    public final List<String> scenes = new ArrayList<>();
    public final List<String> shows = new ArrayList<>();
    public String playing = "";

    public static ConsoleSnapshot build(MinecraftServer server) {
        ConsoleSnapshot snap = new ConsoleSnapshot();
        Map<Integer, Group> map = new TreeMap<>();
        for (StageLightBlockEntity be : LightRegistry.all()) {
            int g = be.data.group;
            if (g <= 0) {
                continue;
            }
            Group group = map.get(g);
            if (group == null) {
                LightData copy = new LightData();
                copy.copyFrom(be.data);
                group = new Group(g, copy);
                map.put(g, group);
            }
            group.counts[be.type().ordinal()]++;
        }
        snap.groups.addAll(map.values());
        StageData data = StageData.get(server);
        snap.scenes.addAll(data.scenes.keySet());
        snap.shows.addAll(data.shows.keySet());
        snap.playing = ShowPlayer.isPlaying() && ShowPlayer.currentName() != null ? ShowPlayer.currentName() : "";
        return snap;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(groups.size());
        for (Group g : groups) {
            buf.writeVarInt(g.number);
            buf.writeNbt(g.look.toTag());
            for (int c : g.counts) {
                buf.writeVarInt(c);
            }
        }
        buf.writeVarInt(scenes.size());
        for (String s : scenes) {
            buf.writeUtf(s, 64);
        }
        buf.writeVarInt(shows.size());
        for (String s : shows) {
            buf.writeUtf(s, 64);
        }
        buf.writeUtf(playing, 64);
    }

    public static ConsoleSnapshot read(FriendlyByteBuf buf) {
        ConsoleSnapshot snap = new ConsoleSnapshot();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            int number = buf.readVarInt();
            CompoundTag tag = buf.readNbt();
            Group g = new Group(number, LightData.fromTag(tag == null ? new CompoundTag() : tag));
            for (int k = 0; k < g.counts.length; k++) {
                g.counts[k] = buf.readVarInt();
            }
            snap.groups.add(g);
        }
        int sc = buf.readVarInt();
        for (int i = 0; i < sc; i++) {
            snap.scenes.add(buf.readUtf(64));
        }
        int sh = buf.readVarInt();
        for (int i = 0; i < sh; i++) {
            snap.shows.add(buf.readUtf(64));
        }
        snap.playing = buf.readUtf(64);
        return snap;
    }
}
