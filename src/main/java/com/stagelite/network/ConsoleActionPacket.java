package com.stagelite.network;

import com.stagelite.block.LightConsoleBlock;
import com.stagelite.config.StageLiteConfig;
import com.stagelite.data.ShowPlayer;
import com.stagelite.data.StageData;
import com.stagelite.light.Effect;
import com.stagelite.light.GroupControl;
import com.stagelite.light.LightData;
import com.stagelite.light.Movement;
import com.stagelite.blockentity.StageLightBlockEntity;
import com.stagelite.light.LightRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

/** Cliente -> servidor: una accion de la consola. El servidor valida permiso, distancia y que sea una consola. */
public class ConsoleActionPacket {
    public static final int GROUP_ON = 0;
    public static final int GROUP_COLOR = 1;
    public static final int GROUP_INTENSITY = 2;
    public static final int GROUP_EFFECT = 3;
    public static final int GROUP_MOVE = 4;
    public static final int GROUP_SPEED = 5;
    public static final int BLACKOUT = 6;
    public static final int ALL_ON = 7;
    public static final int SCENE_SAVE = 8;
    public static final int SCENE_LOAD = 9;
    public static final int SHOW_PLAY = 10;
    public static final int SHOW_STOP = 11;
    public static final int REFRESH = 12;
    public static final int SCENE_DELETE = 13;
    public static final int GROUP_TILT = 14;

    private final BlockPos pos;
    private final int action;
    private final int group;
    private final int value;
    private final String name;

    public ConsoleActionPacket(BlockPos pos, int action, int group, int value, String name) {
        this.pos = pos;
        this.action = action;
        this.group = group;
        this.value = value;
        this.name = name;
    }

    public static void encode(ConsoleActionPacket m, FriendlyByteBuf buf) {
        buf.writeBlockPos(m.pos);
        buf.writeVarInt(m.action);
        buf.writeVarInt(m.group);
        buf.writeInt(m.value);
        buf.writeUtf(m.name, 64);
    }

    public static ConsoleActionPacket decode(FriendlyByteBuf buf) {
        return new ConsoleActionPacket(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readInt(), buf.readUtf(64));
    }

    public static void handle(ConsoleActionPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || !StageLiteConfig.canEdit(sender)) {
                return;
            }
            ServerLevel level = sender.serverLevel();
            if (!level.isLoaded(m.pos) || sender.distanceToSqr(Vec3.atCenterOf(m.pos)) > 144.0
                    || !(level.getBlockState(m.pos).getBlock() instanceof LightConsoleBlock)) {
                return;
            }
            m.run(sender);
        });
        ctx.get().setPacketHandled(true);
    }

    private void run(ServerPlayer sender) {
        var server = sender.getServer();
        if (server == null) {
            return;
        }
        boolean refresh = false;
        switch (action) {
            case GROUP_ON:
                GroupControl.edit(group, d -> d.on = value != 0);
                break;
            case GROUP_COLOR:
                GroupControl.edit(group, d -> d.color = value & 0xFFFFFF);
                break;
            case GROUP_INTENSITY:
                GroupControl.edit(group, d -> d.intensity = value);
                break;
            case GROUP_EFFECT:
                GroupControl.edit(group, d -> d.effect = Effect.byId(value));
                break;
            case GROUP_MOVE:
                GroupControl.edit(group, d -> d.movement = Movement.byId(value));
                break;
            case GROUP_SPEED:
                GroupControl.edit(group, d -> d.speed = value);
                break;
            case GROUP_TILT:
                GroupControl.edit(group, d -> d.tilt = value);
                break;
            case BLACKOUT:
                GroupControl.editAll(d -> d.on = false);
                break;
            case ALL_ON:
                GroupControl.editAll(d -> d.on = true);
                break;
            case SCENE_SAVE: {
                String n = clean(name);
                if (n.isEmpty()) {
                    return;
                }
                Map<Integer, LightData> looks = new TreeMap<>();
                for (StageLightBlockEntity be : LightRegistry.all()) {
                    int g = be.data.group;
                    if (g > 0 && !looks.containsKey(g)) {
                        LightData copy = new LightData();
                        copy.copyFrom(be.data);
                        looks.put(g, copy);
                    }
                }
                if (!looks.isEmpty()) {
                    StageData data = StageData.get(server);
                    data.scenes.put(n, looks);
                    data.setDirty();
                }
                refresh = true;
                break;
            }
            case SCENE_LOAD: {
                Map<Integer, LightData> looks = StageData.get(server).scenes.get(name);
                if (looks != null) {
                    ShowPlayer.applyScene(looks);
                }
                break;
            }
            case SCENE_DELETE: {
                StageData data = StageData.get(server);
                if (data.scenes.remove(name) != null) {
                    data.setDirty();
                }
                refresh = true;
                break;
            }
            case SHOW_PLAY:
                ShowPlayer.play(server, name, value != 0);
                refresh = true;
                break;
            case SHOW_STOP:
                ShowPlayer.stop();
                refresh = true;
                break;
            case REFRESH:
                refresh = true;
                break;
            default:
                return;
        }
        if (refresh) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sender),
                    new OpenConsolePacket(pos, ConsoleSnapshot.build(server)));
        }
    }

    /** Solo letras, numeros, _ y -; maximo 32 caracteres (igual que los comandos "word"). */
    private static String clean(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
                sb.append(c);
            }
            if (sb.length() >= 32) {
                break;
            }
        }
        return sb.toString();
    }
}
