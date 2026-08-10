package org.minenite.cardforge.proxy;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import net.minecraft.resources.Identifier;

/**
 * Velocity's modern player-information forwarding, implemented natively.
 *
 * <p>The legacy BungeeCord scheme smuggles the player's address, UUID and skin
 * through the handshake hostname, separated by NUL bytes. That field is also
 * where mod loaders put their own marker, so on a modded server the two collide
 * and the handshake is ambiguous at best.
 *
 * <p>Modern forwarding avoids the hostname entirely. During login the server
 * sends a login plugin request on {@code velocity:player_info}; the proxy
 * answers with an HMAC-SHA256 signature followed by the forwarded identity. The
 * exchange is authenticated with a shared secret, so a backend that is reachable
 * directly cannot be impersonated the way an offline-mode one can.
 *
 * <p>This class owns the wire format and the verification. The login flow that
 * uses it lives in the mixin on {@code ServerLoginPacketListenerImpl}.
 */
public final class VelocityForwarding {

    /** The channel Velocity listens on for the forwarding handshake. */
    public static final Identifier CHANNEL = Identifier.parse("velocity:player_info");

    /**
     * Highest format version this implementation understands.
     *
     * <p>Velocity names these 1 = default, 2 = with key, 3 = with key v2, 4 =
     * lazy session. Its {@code findForwardingVersion} caps the request at 4 and
     * then, for a client at 1.19.3 or newer, answers 4 if 4 was requested and
     * otherwise falls back to 1 - so for a modern client, asking for 2 or 3 gets
     * the same payload as asking for 1, and only 4 means anything different.
     *
     * <p>Version 4 writes no key fields at all: the key block is written only for
     * versions 2 and 3, so the payload is laid out exactly as version 1 and the
     * decoder below needs no extra cases. What it changes is responsibility -
     * lazy session tells the proxy this backend defers chat session handling
     * rather than validating the player's signing key itself, which a backend
     * behind a proxy cannot do, being offline-mode and never in contact with the
     * session server.
     */
    public static final int MAX_SUPPORTED_VERSION = 4;

    private static final int SIGNATURE_LENGTH = 32;
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private VelocityForwarding() {
    }

    /** The request sent to the proxy; the body is a single byte, our max version. */
    public record Request(int version) implements CustomQueryPayload {

        @Override
        public Identifier id() {
            return CHANNEL;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeByte(this.version);
        }
    }

    /** What the proxy told us about the player, once the signature checked out. */
    public record ForwardedPlayer(int version, String address, GameProfile profile) {
    }

    /**
     * Verifies and decodes a forwarding response.
     *
     * @param buffer the answer payload, positioned at the signature
     * @param secret the shared secret from Velocity's forwarding.secret
     * @return the forwarded player
     * @throws IllegalStateException if the signature does not verify, which means
     *         either the secret is wrong or the data did not come from the proxy
     */
    public static ForwardedPlayer decode(FriendlyByteBuf buffer, String secret) {
        byte[] signature = new byte[SIGNATURE_LENGTH];
        buffer.readBytes(signature);

        // Everything after the signature is what was signed. Read it without
        // consuming, so the fields can be parsed afterwards.
        int dataStart = buffer.readerIndex();
        byte[] data = new byte[buffer.readableBytes()];
        buffer.getBytes(dataStart, data);

        if (!verify(signature, data, secret)) {
            throw new IllegalStateException(
                    "Forwarding signature did not verify. The proxy's forwarding.secret and this server's "
                            + "velocity secret must match exactly.");
        }

        int version = buffer.readVarInt();
        if (version > MAX_SUPPORTED_VERSION) {
            // The proxy must not answer above what was requested; if it does, the
            // remaining fields may not be laid out as expected.
            throw new IllegalStateException("Proxy replied with forwarding version " + version
                    + " but this server requested at most " + MAX_SUPPORTED_VERSION);
        }

        String address = buffer.readUtf(Short.MAX_VALUE);
        UUID uuid = buffer.readUUID();
        String name = buffer.readUtf(16);

        // GameProfile is a record here, so the properties go in at construction
        // rather than being mutated afterwards.
        // PropertyMap wraps a multimap and has no no-arg constructor here.
        com.google.common.collect.Multimap<String, Property> collected =
                com.google.common.collect.LinkedHashMultimap.create();
        int propertyCount = buffer.readVarInt();
        for (int i = 0; i < propertyCount; i++) {
            String propertyName = buffer.readUtf(Short.MAX_VALUE);
            String value = buffer.readUtf(Short.MAX_VALUE);
            String propertySignature = buffer.readBoolean() ? buffer.readUtf(Short.MAX_VALUE) : null;
            collected.put(propertyName,
                    propertySignature == null ? new Property(propertyName, value)
                            : new Property(propertyName, value, propertySignature));
        }

        return new ForwardedPlayer(version, address,
                new GameProfile(uuid, name, new com.mojang.authlib.properties.PropertyMap(collected)));
    }

    private static boolean verify(byte[] signature, byte[] data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] expected = mac.doFinal(data);
            // Constant-time comparison: this is an authentication check, and a
            // timing-sensitive one would leak the secret a byte at a time.
            return java.security.MessageDigest.isEqual(expected, signature);
        } catch (Exception failed) {
            return false;
        }
    }
}
