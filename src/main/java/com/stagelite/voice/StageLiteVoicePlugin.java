package com.stagelite.voice;

import com.stagelite.blockentity.StageLightBlockEntity;
import com.stagelite.light.FixtureType;
import com.stagelite.light.LightRegistry;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Integracion opcional con Simple Voice Chat: la voz de quien habla cerca de un microfono sale por
 * todos los parlantes de su mismo grupo. Si Simple Voice Chat no esta instalado, esta clase nunca se carga.
 */
@ForgeVoicechatPlugin
public class StageLiteVoicePlugin implements VoicechatPlugin {
    private static final int MAX_SPEAKERS_PER_MIC = 24;
    private static final Map<String, LocationalAudioChannel> CHANNELS = new ConcurrentHashMap<>();

    @Override
    public String getPluginId() {
        return "stagelite";
    }

    @Override
    public void initialize(VoicechatApi api) {
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMic);
    }

    private void onMic(MicrophonePacketEvent event) {
        VoicechatConnection connection = event.getSenderConnection();
        VoicechatServerApi api = event.getVoicechat();
        if (connection == null || api == null) {
            return;
        }
        Object raw = connection.getPlayer().getPlayer();
        if (!(raw instanceof ServerPlayer sender)) {
            return;
        }
        byte[] opus = event.getPacket().getOpusEncodedData();
        if (opus == null || opus.length == 0) {
            return;
        }

        for (StageLightBlockEntity mic : LightRegistry.mics()) {
            if (mic.isRemoved() || !mic.data.on || mic.data.group <= 0 || mic.getLevel() != sender.level()) {
                continue;
            }
            double radius = 1 + mic.data.extra;
            if (sender.distanceToSqr(mic.getBlockPos().getX() + 0.5, mic.getBlockPos().getY() + 0.5,
                    mic.getBlockPos().getZ() + 0.5) > radius * radius) {
                continue;
            }
            int sent = 0;
            for (StageLightBlockEntity speaker : LightRegistry.speakers()) {
                if (speaker.isRemoved() || !speaker.data.on || speaker.data.intensity <= 0
                        || speaker.data.group != mic.data.group || speaker.getLevel() != mic.getLevel()) {
                    continue;
                }
                send(api, sender, speaker, opus);
                if (++sent >= MAX_SPEAKERS_PER_MIC) {
                    break;
                }
            }
        }
    }

    private static void send(VoicechatServerApi api, ServerPlayer sender, StageLightBlockEntity speaker, byte[] opus) {
        BlockPos pos = speaker.getBlockPos();
        String key = pos.asLong() + ":" + sender.getUUID();
        if (CHANNELS.size() > 512) {
            CHANNELS.clear();
        }
        LocationalAudioChannel channel = CHANNELS.computeIfAbsent(key, k -> {
            UUID id = UUID.nameUUIDFromBytes(("stagelite:" + k).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return api.createLocationalAudioChannel(id, api.fromServerLevel(speaker.getLevel()),
                    api.createPosition(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
        });
        if (channel != null) {
            channel.setDistance(Math.max(8f, speaker.data.extra * 8f));
            channel.send(opus);
        }
    }
}
