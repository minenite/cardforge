package org.minenite.cardforge.mixin.invoker;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor for methods NeoForge will not let an access transformer widen,
 * because widening them would narrow an existing subclass override.
 * Fabric's runtime access widener had no such restriction.
 */
@Mixin(net.minecraft.server.MinecraftServer.class)
public interface MinecraftServerInvoker {

    @Invoker("selectLevelLoadFocusPos")
    net.minecraft.core.GlobalPos cardforge$selectLevelLoadFocusPos();
}
