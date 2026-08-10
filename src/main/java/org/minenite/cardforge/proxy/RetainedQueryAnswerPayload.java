package org.minenite.cardforge.proxy;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;

/**
 * A login query answer that keeps its bytes.
 *
 * <p>Vanilla's equivalent discards them during decode, which leaves nothing for a
 * handler to inspect. Holding a copy is what makes a login-time handshake
 * possible; see the mixin on {@code ServerboundCustomQueryAnswerPacket}.
 *
 * @param data the response body, already copied out of the network buffer
 */
public record RetainedQueryAnswerPayload(byte[] data) implements CustomQueryAnswerPayload {

    /** True when the proxy answered at all; an absent response has no bytes. */
    public boolean isPresent() {
        return this.data != null && this.data.length > 0;
    }

    public FriendlyByteBuf toBuffer() {
        return new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(this.data));
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBytes(this.data);
    }
}
