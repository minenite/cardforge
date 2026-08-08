package org.cardboardpowered.mixin.server.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.cardboardpowered.impl.CardboardServerListPingEvent;
import org.cardboardpowered.util.MixinInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.SharedConstants;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerStatusPacketListenerImpl;
import net.minecraft.server.players.NameAndId;

@MixinInfo(events = {"ServerListPingEvent"})
@Mixin(ServerStatusPacketListenerImpl.class)
public class ServerStatusPacketListenerImplMixin {

    // @Shadow private static Text REQUEST_HANDLED;
    @Shadow private Connection connection;
    @Shadow private boolean hasRequestedStatus;

    /**
     * @author Cardboard
     * @reason ServerListPingEvent
     */
    @Overwrite
    public void handleStatusRequest(ServerboundStatusRequestPacket packetstatusinstart) {
        if (this.hasRequestedStatus) {
            // this.connection.disconnect(new LiteralText("BF: Response sent!"));
        	this.connection.disconnect(Component.nullToEmpty("BF: Response sent!"));
        } else {
            this.hasRequestedStatus = true;
            MinecraftServer server = CraftServer.server;

            CardboardServerListPingEvent event = new CardboardServerListPingEvent(connection, server);
            CraftServer.INSTANCE.getPluginManager().callEvent(event);

            ArrayList<NameAndId> profiles = new ArrayList<NameAndId>(event.players.length);
            for (Object player : event.players) {
                if (player != null) {
                    profiles.add(((ServerPlayer) player).nameAndId());
                }
            }
            

            List<NameAndId> profiles2 = (server.hidesOnlinePlayers()) ? Collections.emptyList() : profiles;
            ServerStatus.Players samp = new ServerStatus.Players(event.getMaxPlayers(), profiles.size(), profiles2);

            ServerStatus ping = new ServerStatus(
                    CraftChatMessage.fromString(event.getMotd(), true)[0],
                    Optional.of(samp),
                    Optional.of(new ServerStatus.Version(server.getServerModName() + " " + server.getServerVersion(), SharedConstants.getCurrentVersion().protocolVersion())),
                    (event.icon.value != null) ? Optional.of(new ServerStatus.Favicon(event.icon.value)) : Optional.empty(),
                    server.enforceSecureProfile());

            this.connection.send(new ClientboundStatusResponsePacket(ping));
        }
    }

   // @Shadow public ClientConnection getConnection() {return null;}
   // @Shadow public void onDisconnected(Text reason) {}
  //  @Shadow public void onPing(QueryPingC2SPacket packet) {}

}