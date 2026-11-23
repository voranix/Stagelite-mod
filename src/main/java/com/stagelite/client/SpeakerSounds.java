package com.stagelite.client;

import com.stagelite.blockentity.StageLightBlockEntity;
import com.stagelite.light.LightData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.Map;
import java.util.WeakHashMap;

/** Cliente: musica en bucle de cada parlante (discos del juego). El alcance sale del fader y el volumen de la intensidad. */
public final class SpeakerSounds {
    private static final String[] NAMES = {"13", "cat", "blocks", "chirp", "far", "mall", "mellohi", "stal",
            "strad", "ward", "11", "wait", "otherside", "relic", "5", "pigstep"};

    private static final class Playing {
        SimpleSoundInstance sound;
        int track;
        float volume;
    }

    private static final Map<StageLightBlockEntity, Playing> PLAYING = new WeakHashMap<>();

    private SpeakerSounds() {
    }

    public static String trackName(int track) {
        return track >= 1 && track <= NAMES.length ? NAMES[track - 1] : "-";
    }

    public static void update(StageLightBlockEntity be) {
        LightData d = be.data;
        boolean want = d.on && d.intensity > 0 && d.track > 0 && d.track <= NAMES.length && !d.linked;
        Playing cur = PLAYING.get(be);
        float volume = Math.max(0.05f, d.extra * 8f / 16f) * Math.max(0.05f, d.intensity / 100f);
        // el alcance depende de "volume" con atenuacion lineal (16 bloques por unidad)
        float range = d.extra * 8f;
        float vol = range / 16f;
        if (!want) {
            stop(be);
            return;
        }
        if (cur != null && cur.track == d.track && Math.abs(cur.volume - vol) < 0.001f
                && Minecraft.getInstance().getSoundManager().isActive(cur.sound)) {
            return;
        }
        stop(be);
        Playing p = new Playing();
        p.track = d.track;
        p.volume = vol;
        p.sound = new SimpleSoundInstance(new ResourceLocation("minecraft", "music_disc." + NAMES[d.track - 1]),
                SoundSource.RECORDS, vol * Math.max(0.05f, d.intensity / 100f), 1f, RandomSource.create(), true, 0,
                SoundInstance.Attenuation.LINEAR,
                be.getBlockPos().getX() + 0.5, be.getBlockPos().getY() + 0.5, be.getBlockPos().getZ() + 0.5, false);
        Minecraft.getInstance().getSoundManager().play(p.sound);
        PLAYING.put(be, p);
    }

    public static void stop(StageLightBlockEntity be) {
        Playing cur = PLAYING.remove(be);
        if (cur != null) {
            Minecraft.getInstance().getSoundManager().stop(cur.sound);
        }
    }
}
