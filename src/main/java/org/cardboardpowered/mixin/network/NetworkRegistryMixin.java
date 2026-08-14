package org.cardboardpowered.mixin.network;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.negotiation.NegotiableNetworkComponent;
import net.neoforged.neoforge.network.negotiation.NegotiationResult;
import net.neoforged.neoforge.network.negotiation.NetworkComponentNegotiator;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.cardboardpowered.network.BukkitPluginChannels;
import org.cardboardpowered.network.BukkitRawPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets a client join carrying mods this server does not have.
 *
 * <p>NeoForge negotiates channels in both directions, and the server refuses a
 * client whose mods require a channel it cannot offer:
 *
 * <pre>
 * Channel of mod "glitchcore" failed to connect:
 * This channel is missing on the server side, but required on the client!
 * </pre>
 *
 * <p>The right default for a single server, where that is a misconfiguration. It
 * is wrong for a network: one client connection visits several servers, so a
 * lobby deliberately kept free of content mods will always see clients carrying
 * the modpack the other servers need.
 *
 * <p>The client's own channels are dropped before negotiating, rather than the
 * result being forced to succeed afterwards. Forcing the result is the obvious
 * shortcut and it does not work: a failed negotiation carries no components, so
 * the connection is set up with nothing registered and dies moments later on
 * NeoForge's own internal channels.
 *
 * <pre>
 * Payload neoforge:extensible_enum_data may not be sent to the client!
 * </pre>
 *
 * <p>Removing the unmatched entries first leaves a negotiation that genuinely
 * succeeds, with every mutual channel - NeoForge's included - intact. The mod
 * whose channel was dropped simply has no server counterpart here, which is
 * already true; its content is not on this server either.
 *
 * <p>Off unless {@code settings.allow-mismatched-client-mods} is set, because it
 * gives up a real diagnostic: a player missing something they need finds out when
 * a feature does nothing rather than when they connect.
 */
@Mixin(NetworkRegistry.class)
public class NetworkRegistryMixin {

    @Redirect(
            method = "initializeNeoForgeConnection(Lnet/minecraft/network/protocol/configuration/ServerConfigurationPacketListener;Ljava/util/Map;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/network/negotiation/NetworkComponentNegotiator;"
                            + "negotiate(Ljava/util/List;Ljava/util/List;)"
                            + "Lnet/neoforged/neoforge/network/negotiation/NegotiationResult;"))
    private static NegotiationResult cardboard$ignoreClientOnlyChannels(
            List<NegotiableNetworkComponent> server, List<NegotiableNetworkComponent> client) {

        if (!org.spigotmc.SpigotConfig.allowMismatchedClientMods) {
            return NetworkComponentNegotiator.negotiate(server, client);
        }

        Set<Identifier> known = server.stream()
                .map(NegotiableNetworkComponent::id)
                .collect(Collectors.toSet());
        List<NegotiableNetworkComponent> mutual = client.stream()
                .filter(component -> known.contains(component.id()))
                .toList();

        if (mutual.size() != client.size()) {
            org.cardboardpowered.CardboardMod.LOGGER.info(
                    "Ignoring " + (client.size() - mutual.size())
                            + " client channel(s) this server does not have; the mods behind them are not here either");
        }
        return NetworkComponentNegotiator.negotiate(server, mutual);
    }

    /**
     * Bukkit plugin channels are not NeoForge payload registrations. Without a
     * codec their bodies are discarded on the way in and written as empty on
     * the way out, so companions never see scope / laser / hello packets.
     */
    @Inject(method = "getCodec", at = @At("RETURN"), cancellable = true)
    private static void cardboard$bukkitPluginCodec(
            Identifier id,
            ConnectionProtocol protocol,
            PacketFlow flow,
            CallbackInfoReturnable<StreamCodec<? super FriendlyByteBuf, ? extends CustomPacketPayload>> cir) {
        if (cir.getReturnValue() != null) {
            return;
        }
        if (!BukkitPluginChannels.isBukkitChannel(id, flow)) {
            return;
        }
        cir.setReturnValue(BukkitRawPayload.codec(id));
    }

    /**
     * Incoming plugin messages (hello, prone_req, …) must reach Bukkit listeners
     * instead of disconnecting as an unknown NeoForge channel.
     */
    @Inject(method = "handleModdedPayload", at = @At("HEAD"), cancellable = true)
    private static void cardboard$bukkitIncoming(
            ServerCommonPacketListener listener,
            ServerboundCustomPayloadPacket packet,
            CallbackInfo ci) {
        if (!(packet.payload() instanceof BukkitRawPayload raw)) {
            return;
        }
        BukkitPluginChannels.dispatchIncoming(listener, raw);
        // Always swallow: during configuration the listener is not play yet, and
        // falling through lets NeoForge kick "Incompatible client! Please use NeoForge …".
        ci.cancel();
    }

    /**
     * Plugin S2C is allowed even when the channel was dropped from NeoForge
     * negotiation (client-only companion mods).
     */
    @Inject(
            method = "checkPacket(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/protocol/common/ServerCommonPacketListener;)V",
            at = @At("HEAD"),
            cancellable = true)
    private static void cardboard$allowBukkitPluginSend(Packet<?> packet, ServerCommonPacketListener listener, CallbackInfo ci) {
        if (packet instanceof ClientboundCustomPayloadPacket custom
                && custom.payload() instanceof BukkitRawPayload) {
            ci.cancel();
        }
    }
}
