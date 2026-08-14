package org.cardboardpowered.network;

import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;
import org.cardboardpowered.bridge.server.level.ServerPlayerBridge;

/**
 * Bridges Bukkit plugin messaging onto NeoForge custom payloads.
 *
 * <p>These are not registered as NeoForge {@code RegisterPayloadHandlersEvent}
 * payloads. Doing that puts them in channel negotiation, and Velocity transfers
 * from lobby then die with {@code Incompatible client! Please use NeoForge …}
 * before play starts. Codec + dispatch live in {@code NetworkRegistryMixin}.
 */
public final class BukkitPluginChannels {
    private BukkitPluginChannels() {
    }

    public static boolean isBukkitChannel(Identifier id, PacketFlow flow) {
        if (id == null || "minecraft".equals(id.getNamespace())) {
            return false;
        }
        if ("pvpgunminus".equals(id.getNamespace())) {
            return true;
        }
        Messenger messenger = messenger();
        if (messenger == null) {
            return false;
        }
        String channel = id.toString();
        if (flow == PacketFlow.SERVERBOUND) {
            return messenger.getIncomingChannels().contains(channel);
        }
        if (flow == PacketFlow.CLIENTBOUND) {
            return messenger.getOutgoingChannels().contains(channel);
        }
        return messenger.getIncomingChannels().contains(channel)
                || messenger.getOutgoingChannels().contains(channel);
    }

    public static boolean dispatchIncoming(ServerCommonPacketListener listener, BukkitRawPayload payload) {
        if (payload == null || payload.id() == null) {
            return false;
        }
        if (!(listener instanceof ServerGamePacketListenerImpl play) || play.player == null) {
            return false;
        }
        Messenger messenger = messenger();
        if (messenger == null) {
            return false;
        }
        String channel = payload.id().toString();
        if (!messenger.getIncomingChannels().contains(channel)) {
            return false;
        }
        ServerPlayer nms = play.player;
        byte[] data = payload.data() == null ? new byte[0] : payload.data();
        nms.level().getServer().execute(() -> {
            Player player = (Player) ((ServerPlayerBridge) (Object) nms).getBukkitEntity();
            if (player != null && player.isOnline()) {
                messenger.dispatchIncomingMessage(player, channel, data);
            }
        });
        return true;
    }

    private static Messenger messenger() {
        try {
            if (Bukkit.getServer() == null) {
                return null;
            }
            return Bukkit.getMessenger();
        } catch (Throwable ignored) {
            return null;
        }
    }
}
