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

    @Shadow
    private com.mojang.authlib.GameProfile authenticatedProfile;

    @Shadow
    private ServerLoginPacketListenerImpl.State state;

    @Unique
    private int cardboard$velocityTransactionId = -1;


    /**
     * Asks the proxy who this is, instead of proceeding with the vanilla login.
     *
     * <p>Injected before the online-mode branch: with forwarding enabled the
     * backend must not attempt its own session authentication, because the session
     * was established between the client and the proxy.
     */
    @Inject(method = "handleHello", at = @At("HEAD"))
    private void cardboard$traceHello(ServerboundHelloPacket packet, CallbackInfo ci) {
        // At HEAD, not TAIL: TAIL injects at the final return only, so a method
        // with an early branch traces nothing and reads as "never called".
        org.minenite.cardforge.proxy.ProxyTrace.log("handleHello HEAD: name=" + packet.name()
                + " velocityModern=" + org.spigotmc.SpigotConfig.velocityModern
                + " secretLen=" + org.spigotmc.SpigotConfig.velocitySecret.length());
    }

    /**
     * Asks the proxy who this is, before vanilla decides for itself.
     *
     * <p>At HEAD, not TAIL. Vanilla's offline branch builds a profile from the
     * name and starts verification with it, and the inherited Spigot path then
     * fires its login events from a thread that re-stores whatever profile it
     * captured. Both would race the proxy's answer, and when they won the player
     * entered the world under a name-hashed UUID with no skin - which is why the
     * skin appeared on some joins and not others.
     *
     * <p>Cancelling here means neither runs: no stand-in profile is ever created,
     * and the login resumes only when the forwarded identity arrives.
     */
    @Inject(method = "handleHello", at = @At("HEAD"), cancellable = true)
    private void cardboard$requestForwardedIdentity(ServerboundHelloPacket packet, CallbackInfo ci) {
        if (!org.spigotmc.SpigotConfig.velocityModern) {
            return;
        }
        // Vanilla sets this in the body being skipped, and everything downstream
        // expects it to be populated.
        this.requestedUsername = packet.name();
        // A fresh id per connection; the answer is matched against it so another
        // login plugin exchange cannot be mistaken for ours.
        this.cardboard$velocityTransactionId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        this.connection.send(new ClientboundCustomQueryPacket(
                this.cardboard$velocityTransactionId,
                new VelocityForwarding.Request(VelocityForwarding.MAX_SUPPORTED_VERSION)));

        org.minenite.cardforge.proxy.ProxyTrace.log("sent forwarding request, txn="
                + this.cardboard$velocityTransactionId);
        ci.cancel();
    }

    @Inject(method = "handleCustomQueryPacket", at = @At("HEAD"), cancellable = true)
    private void cardboard$acceptForwardedIdentity(ServerboundCustomQueryAnswerPacket packet, CallbackInfo ci) {
        org.minenite.cardforge.proxy.ProxyTrace.log("answer received: txn=" + packet.transactionId()
                + " expected=" + this.cardboard$velocityTransactionId
                + " payload=" + packet.payload().getClass().getSimpleName());
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

            org.minenite.cardforge.proxy.ProxyTrace.log("verified: name=" + forwarded.profile().name()
                    + " uuid=" + forwarded.profile().id() + " addr=" + forwarded.address()
                    + " props=" + forwarded.profile().properties().size()
                    + " keys=" + forwarded.profile().properties().keySet());
            this.requestedUsername = forwarded.profile().name();
            this.startClientVerification(forwarded.profile());

            // The Bukkit login events normally fire from a thread started in
            // handleHello, which is skipped under forwarding because no profile
            // exists yet. Fire them here instead, with the identity the proxy
            // vouched for, so plugins see the real player. On its own thread: the
            // synchronous event is handed to the server thread and waited on.
            final com.mojang.authlib.GameProfile verified = forwarded.profile();
            new Thread(() -> ((org.cardboardpowered.bridge.network.LoginEventsBridge) this)
                    .cardboard$fireLoginEvents(verified), "Forwarded Login Verifier").start();
            // Confirm the call actually stored it: this mixin reaches
            // startClientVerification through a shadow, and a shadow that failed to
            // bind would silently do nothing.
            org.minenite.cardforge.proxy.ProxyTrace.log("after startClientVerification: stored="
                    + (this.authenticatedProfile == null ? "null"
                            : this.authenticatedProfile.id() + " props=" + this.authenticatedProfile.properties().size())
                    + " state=" + this.state);
        } catch (Exception refused) {
            org.minenite.cardforge.proxy.ProxyTrace.log("REJECTED: " + refused);
            org.cardboardpowered.CardboardMod.LOGGER.warning(
                    "Rejected a forwarded login: " + refused.getMessage());
            this.disconnect(Component.literal("Invalid proxy forwarding data."));
        } finally {
            buffer.release();
        }
    }
}
