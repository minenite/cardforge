package org.cardboardpowered.mixin.network;

import net.neoforged.neoforge.network.negotiation.NegotiationResult;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
 * <p>That is the right default for a single server, where a client with mods the
 * server lacks is a misconfiguration. It is wrong for a network: one client
 * connection visits several servers, so a lobby deliberately kept free of content
 * mods will always see clients carrying the modpack the other servers need.
 *
 * <p>Off unless {@code settings.allow-mismatched-client-mods} is set in
 * spigot.yml, because it gives up a real diagnostic - a player missing something
 * they need now finds out when a feature does nothing rather than when they
 * connect. The reasons are logged either way.
 *
 * <p>This is the server half of the same problem CardForge's companion client mod
 * solves from the other side. Both are needed: this one admits a client with
 * extra mods, that one lets a client cope with a server that has fewer.
 */
@Mixin(NetworkRegistry.class)
public class NetworkRegistryMixin {

    @Redirect(
            method = "initializeNeoForgeConnection(Lnet/minecraft/network/protocol/configuration/ServerConfigurationPacketListener;Ljava/util/Map;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/network/negotiation/NegotiationResult;success()Z"))
    private static boolean cardboard$allowClientsWithExtraMods(NegotiationResult result) {
        if (result.success() || !org.spigotmc.SpigotConfig.allowMismatchedClientMods) {
            return result.success();
        }
        org.cardboardpowered.CardboardMod.LOGGER.info(
                "Admitting a client whose mods this server does not have: " + result.failureReasons().keySet());
        return true;
    }
}
