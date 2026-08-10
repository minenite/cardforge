package org.cardboardpowered.mixin.server.network;

import com.mojang.authlib.GameProfile;

import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;

import org.minenite.cardforge.proxy.RetainedQueryAnswerPayload;
import org.minenite.cardforge.proxy.VelocityForwarding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Velocity modern forwarding, on the backend side.
 *
 * <p>Legacy BungeeCord forwarding packs the player's address, UUID and skin into
 * the handshake hostname. A mod loader marks that same field, so on a modded
 * server the two collide - which is exactly why the inherited legacy path could
 * not be made to work here.
 *
 * <p>Modern forwarding leaves the handshake alone. When the client says hello,
 * the server asks the proxy for the player's real identity over a login plugin
 * channel, and the proxy answers with an HMAC-SHA256 signature over the data. A
 * shared secret authenticates the exchange, so unlike an offline-mode backend
 * this cannot be impersonated by connecting to the backend directly.
 *
 * <p>The login then continues exactly as vanilla would for an offline server,
 * except the profile is the real one rather than a name-derived stand-in - so
 * skins, UUIDs and signed chat all behave.
 */
@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginPacketListenerImplMixin_Velocity {

    @Shadow
    private String requestedUsername;

    @Shadow
    @org.spongepowered.asm.mixin.Final
    private Connection connection;

    @Shadow
    public abstract void disconnect(Component reason);

    @Shadow
    private void startClientVerification(GameProfile profile) {
    }

    @Unique
    private int cardboard$velocityTransactionId = -1;

    /**
     * Asks the proxy who this is, instead of proceeding with the vanilla login.
     *
     * <p>Injected before the online-mode branch: with forwarding enabled the
     * backend must not attempt its own session authentication, because the session
     * was established between the client and the proxy.
     */
    @Inject(method = "handleHello", at = @At("TAIL"), cancellable = true)
    private void cardboard$requestForwardedIdentity(ServerboundHelloPacket packet, CallbackInfo ci) {
        if (!org.spigotmc.SpigotConfig.velocityModern) {
            return;
        }
        // A fresh id per connection; the answer is matched against it so another
        // login plugin exchange cannot be mistaken for ours.
        this.cardboard$velocityTransactionId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        this.connection.send(new ClientboundCustomQueryPacket(
                this.cardboard$velocityTransactionId,
                new VelocityForwarding.Request(VelocityForwarding.MAX_SUPPORTED_VERSION)));
        ci.cancel();
    }

    @Inject(method = "handleCustomQueryPacket", at = @At("HEAD"), cancellable = true)
    private void cardboard$acceptForwardedIdentity(ServerboundCustomQueryAnswerPacket packet, CallbackInfo ci) {
        if (!org.spigotmc.SpigotConfig.velocityModern
                || packet.transactionId() != this.cardboard$velocityTransactionId) {
            return;
        }
        ci.cancel();

        if (!(packet.payload() instanceof RetainedQueryAnswerPayload retained) || !retained.isPresent()) {
            // No answer means we are not actually behind a Velocity proxy, or it has
            // forwarding switched off. Refusing is the safe reading: with forwarding
            // enabled this server is offline-mode and must not admit an unverified
            // player.
            this.disconnect(Component.literal(
                    "This server only accepts connections through its proxy."));
            return;
        }

        FriendlyByteBuf buffer = retained.toBuffer();
        try {
            VelocityForwarding.ForwardedPlayer forwarded =
                    VelocityForwarding.decode(buffer, org.spigotmc.SpigotConfig.velocitySecret);

            // The player's real address, so bans, logs and plugins see the client
            // rather than the proxy.
            java.net.SocketAddress remote = this.connection.getRemoteAddress();
            int port = remote instanceof java.net.InetSocketAddress inet ? inet.getPort() : 0;
            this.connection.address = new java.net.InetSocketAddress(forwarded.address(), port);

            this.requestedUsername = forwarded.profile().name();
            this.startClientVerification(forwarded.profile());
        } catch (Exception refused) {
            org.cardboardpowered.CardboardMod.LOGGER.warning(
                    "Rejected a forwarded login: " + refused.getMessage());
            this.disconnect(Component.literal("Invalid proxy forwarding data."));
        } finally {
            buffer.release();
        }
    }
}
