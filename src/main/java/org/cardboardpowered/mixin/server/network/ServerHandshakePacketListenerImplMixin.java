package org.cardboardpowered.mixin.server.network;

import org.cardboardpowered.bridge.network.ConnectionBridge;
import org.cardboardpowered.bridge.server.network.ServerLoginPacketListenerImplBridge;

import me.isaiah.common.GameVersion;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.handshake.ClientIntent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ServerHandshakePacketListenerImpl.class)
public class ServerHandshakePacketListenerImplMixin {

    private static final com.google.gson.Gson gson = new com.google.gson.Gson(); // Spigot

    @Shadow
    public Connection connection;

    @Inject(at = @At("TAIL"), method = "handleIntention")
    public void onHandshake_Bungee(ClientIntentionPacket packet, CallbackInfo ci) {
    	org.cardboardpowered.CardboardMod.LOGGER.info("[bungee] handshake hook ran: intent=" + packet.intention()
    			+ " proto=" + packet.protocolVersion() + " serverProto=" + net.minecraft.SharedConstants.getProtocolVersion()
    			+ " bungeeFlag=" + org.spigotmc.SpigotConfig.bungee
    			+ " host=" + packet.hostName().replace("\0", "<NUL>"));
    	org.minenite.cardforge.proxy.ProxyTrace.log("handshake: intent=" + packet.intention()
    			+ " proto=" + packet.protocolVersion() + " server=" + net.minecraft.SharedConstants.getProtocolVersion()
    			+ " host=" + packet.hostName().replace("\0", "<NUL>"));
    	if (packet.intention() == ClientIntent.LOGIN) {
            // Ask Minecraft, not iCommonLib. GameVersion.INSTANCE is only ever
            // populated by FabricServer.getGameVersion(), a Fabric-only class that
            // nothing on NeoForge calls, so it was always null here and every single
            // login died with a NullPointerException before reaching the login
            // listener. SharedConstants is the authoritative source and needs no
            // platform-specific bootstrap.
            int serverProtocol = net.minecraft.SharedConstants.getProtocolVersion();

            if (packet.protocolVersion() > serverProtocol) {
            } else if (packet.protocolVersion() < serverProtocol) {
            } else {
                if (org.spigotmc.SpigotConfig.bungee) {
                    String[] split = packet.hostName().split("\00");
                    // Temporary: the connection dies inside this handshake with nothing
                    // logged on either side, so log what actually arrived.
                    // Spigot requires exactly 3 or 4 segments - host, ip, uuid and
                    // optionally the signed properties - and silently returns otherwise.
                    // A modded server never satisfies that: NeoForge appends its own
                    // marker to the handshake hostname to signal a modded connection, so
                    // a proxied handshake carries extra segments, fell into the else, and
                    // the login listener never received its hostname. Nothing was logged
                    // and the connection simply died.
                    //
                    // The first three segments are the forwarding payload wherever it
                    // came from; anything after the properties belongs to the loader and
                    // is not ours to interpret.
                    if (split.length >= 3) {
                        connection.address = new java.net.InetSocketAddress(split[1], ((java.net.InetSocketAddress) connection.getRemoteAddress()).getPort());
                        ((ConnectionBridge) (Object) connection).setSpoofedUUID(fromString( split[2] ));
                        if (split.length >= 4) {
                            try {
                                ((ConnectionBridge) (Object) connection).setSpoofedProfile(gson.fromJson(split[3], com.mojang.authlib.properties.Property[].class));
                            } catch (Exception malformed) {
                                // A loader marker rather than a properties array. Losing the
                                // skin is survivable; losing the connection is not.
                                org.cardboardpowered.CardboardMod.LOGGER.warning(
                                        "Ignoring unparseable forwarded profile properties: " + malformed);
                            }
                        }
                    } else {
                        // Genuinely not a forwarded handshake. Say so, rather than
                        // dropping the player with no explanation.
                        org.cardboardpowered.CardboardMod.LOGGER.warning(
                                "settings.bungeecord is enabled but a connection arrived without forwarding data ("
                                        + split.length + " segment(s)). Is this server reachable directly?");
                        return;
                    }
                }
                try {
                    ((ServerLoginPacketListenerImplBridge)((ServerLoginPacketListenerImpl) this.connection.getPacketListener())).setHostname(packet.hostName() + ":" + packet.port()); // Bukkit - set hostname
                } catch (Throwable t) {
                    org.cardboardpowered.CardboardMod.LOGGER.warning("[bungee] setHostname failed: " + t);
                    throw t;
                }
            }
        }
    }


    private UUID fromString(final String input) {
        return UUID.fromString(input.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
    }

}
