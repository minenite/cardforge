package org.minenite.cardforge.mixin.invoker;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the chunks the server believes this player has been sent.
 *
 * <p>The field is private and there is no getter, so the API that asks which
 * chunks a player can see had nothing to answer with and returned an empty set -
 * which reads as "none", not as "unknown".
 */
@Mixin(net.minecraft.server.level.ServerPlayer.class)
public interface ServerPlayerAccessor {

    @Accessor("chunkTrackingView")
    net.minecraft.server.level.ChunkTrackingView cardforge$chunkTrackingView();
}
