package org.cardboardpowered.mixin.server.network;

import net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Leaves chat signing to the proxy instead of half-enforcing it on the backend.
 *
 * <p>A backend behind a proxy is offline-mode and never speaks to the session
 * server, so it cannot police the player's signing key - the proxy authenticated
 * the player and is the party that can. Vanilla has no notion of that split, and
 * tries twice to enforce it:
 *
 * <ol>
 *   <li>{@code handleChatSessionUpdate} validates the key the client sends and
 *       disconnects if it does not verify. This ignores
 *       {@code enforce-secure-profile}, which only decides whether a key is
 *       <em>required</em>, so switching that off does not prevent the kick:
 *       <em>Invalid signature for profile public key.</em></li>
 *   <li>Every chat message is then verified against that key, so accepting the
 *       key but leaving the rest alone only moves the failure one step later, to
 *       <em>Chat had an invalid signature. Please try reconnecting.</em></li>
 * </ol>
 *
 * <p>Both come from installing a signing session the backend has no way to
 * verify, so the session is not installed at all. The listener is constructed
 * with {@code SignedMessageChain.Decoder.unsigned}, which is vanilla's own path
 * for a player without a key, and leaving it in place means chat is carried
 * unsigned - accepted, because a proxied backend runs with
 * {@code enforce-secure-profile} off.
 *
 * <p>The tradeoff is real and worth stating: messages from a proxied player are
 * not cryptographically signed end to end, so the client shows no "secure chat"
 * indicator and reports cannot be attributed by signature. That is inherent to
 * proxying - the proxy terminates the authenticated session - and is what the
 * MODERN_LAZY_SESSION forwarding version exists to express.
 *
 * <p>Unproxied servers are untouched and enforce signing exactly as before.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin_ProxyChatSession {

    @Inject(method = "handleChatSessionUpdate", at = @At("HEAD"), cancellable = true)
    private void cardboard$skipProxiedChatSession(ServerboundChatSessionUpdatePacket packet, CallbackInfo ci) {
        if (org.spigotmc.SpigotConfig.proxied()) {
            ci.cancel();
        }
    }
}
