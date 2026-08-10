package org.cardboardpowered.mixin.server.network;

import com.mojang.authlib.GameProfile;

import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.SignatureValidator;
import net.minecraft.world.entity.player.ProfilePublicKey;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Trusts the player's chat signing key when a proxy vouched for the login.
 *
 * <p>{@code handleChatSessionUpdate} validates the key the client sends against
 * Mojang's services key and disconnects if it does not verify. That check does
 * not consult {@code enforce-secure-profile} - the property only decides whether
 * a key is required, so a key that is sent is always validated, and turning the
 * property off does not prevent this kick:
 *
 * <pre>
 * Failed to validate profile key: Invalid signature for profile public key.
 * baecomeover lost connection: Invalid signature for profile public key.
 * </pre>
 *
 * <p>Behind a proxy the backend is offline-mode and never speaks to the session
 * server, so it is in no position to make that judgement - the proxy is the party
 * that authenticated the player, and it validated the key when it did. Velocity
 * says as much in its protocol: forwarding version 4 is {@code
 * MODERN_LAZY_SESSION}, which exists so the backend defers session handling to
 * the proxy. Vanilla has the same notion, in {@code ProfilePublicKey.TRUSTED_CODEC}
 * for keys read from a source that is already trusted.
 *
 * <p>So when forwarding is on, the key is taken as given rather than verified.
 * The session still has to be well formed, and the expiry check ahead of this
 * still applies. Unproxied servers are untouched and validate exactly as before.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin_ProxyChatSession {

    @Redirect(method = "handleChatSessionUpdate",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/RemoteChatSession$Data;"
                            + "validate(Lcom/mojang/authlib/GameProfile;Lnet/minecraft/util/SignatureValidator;)"
                            + "Lnet/minecraft/network/chat/RemoteChatSession;"))
    private RemoteChatSession cardboard$trustProxyVouchedSession(RemoteChatSession.Data data,
                                                                 GameProfile profile,
                                                                 SignatureValidator validator)
            throws ProfilePublicKey.ValidationException {
        if (org.spigotmc.SpigotConfig.proxied()) {
            return new RemoteChatSession(data.sessionId(), new ProfilePublicKey(data.profilePublicKey()));
        }
        return data.validate(profile, validator);
    }
}
