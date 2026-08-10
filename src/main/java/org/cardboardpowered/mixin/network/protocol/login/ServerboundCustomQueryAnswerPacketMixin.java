package org.cardboardpowered.mixin.network.protocol.login;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;

import org.minenite.cardforge.proxy.RetainedQueryAnswerPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps the bytes of a login plugin response instead of discarding them.
 *
 * <p>Vanilla has no use for the body of a login query answer, so
 * {@code readPayload} skips the bytes and returns
 * {@code DiscardedQueryAnswerPayload.INSTANCE}. By the time any handler sees the
 * packet the data is already gone, which makes it impossible to implement a
 * login-time handshake - Velocity's modern forwarding among them - from a
 * handler alone.
 *
 * <p>The bytes are retained here and read by the login listener. Anything that
 * does not care still sees a payload it can ignore, and the buffer is released
 * with the packet as usual.
 */
@Mixin(ServerboundCustomQueryAnswerPacket.class)
public class ServerboundCustomQueryAnswerPacketMixin {

    @Inject(method = "readPayload(ILnet/minecraft/network/FriendlyByteBuf;)"
            + "Lnet/minecraft/network/protocol/login/custom/CustomQueryAnswerPayload;",
            at = @At("HEAD"), cancellable = true)
    private static void cardboard$retainPayload(int transactionId, FriendlyByteBuf buffer,
                                                CallbackInfoReturnable<CustomQueryAnswerPayload> cir) {
        // The frame is a boolean followed by the body. Vanilla skips both together
        // as one blob, so reading the blob as data treats the flag as the payload's
        // first byte - which is how a 32-byte signature ended up being read from a
        // 1-byte buffer when a vanilla client answered "channel not understood".
        boolean understood = buffer.readableBytes() > 0 && buffer.readBoolean();
        byte[] data = new byte[buffer.readableBytes()];
        // Copy rather than retain the buffer: the packet's buffer is released once
        // decoding finishes, and holding a reference past that is a use-after-free.
        buffer.readBytes(data);
        cir.setReturnValue(new RetainedQueryAnswerPayload(understood, data));
    }
}
