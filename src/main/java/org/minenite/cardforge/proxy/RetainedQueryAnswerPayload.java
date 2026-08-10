package org.minenite.cardforge.proxy;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;

/**
 * A login query answer that keeps its bytes.
 *
 * <p>Vanilla's equivalent discards them during decode, leaving nothing for a
 * handler to inspect, which is why a login-time handshake cannot be built from a
 * handler alone.
 *
 * <p>The wire form is a boolean followed by the data, and vanilla skips both
 * together as one opaque blob - it never separates the flag. A client that does
 * not recognise the channel answers with just that byte set to false, so reading
 * the blob as data means reading the flag as the first byte of the payload.
 * That is exactly what happened: a 32-byte signature read out of a 1-byte
 * buffer.
 *
 * @param understood whether the other end recognised the channel
 * @param data       the response body, already copied out of the network buffer
 */
public record RetainedQueryAnswerPayload(boolean understood, byte[] data) implements CustomQueryAnswerPayload {

    /** True when the other end answered with an actual body. */
    public boolean isPresent() {
        return this.understood && this.data != null && this.data.length > 0;
    }

    public FriendlyByteBuf toBuffer() {
        return new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(this.data));
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.understood);
        buffer.writeBytes(this.data);
    }
}
