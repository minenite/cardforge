package org.cardboardpowered.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Raw Bukkit plugin-message body on a NeoForge custom payload.
 *
 * <p>26.2 {@code DiscardedPayload} no longer carries bytes (its encoder writes
 * nothing), so plugin channels have to be a real payload type.
 */
public record BukkitRawPayload(Identifier id, byte[] data) implements CustomPacketPayload {
    public static Type<BukkitRawPayload> typeOf(Identifier id) {
        return new Type<>(id);
    }

    public static <T extends FriendlyByteBuf> StreamCodec<T, BukkitRawPayload> codec(Identifier id) {
        return StreamCodec.of(
                (buf, payload) -> buf.writeBytes(payload.data() == null ? new byte[0] : payload.data()),
                buf -> {
                    byte[] data = new byte[buf.readableBytes()];
                    buf.readBytes(data);
                    return new BukkitRawPayload(id, data);
                });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return typeOf(this.id);
    }
}
