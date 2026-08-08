package org.minenite.cardforge.mixin.invoker;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor for methods NeoForge will not let an access transformer widen,
 * because widening them would narrow an existing subclass override.
 * Fabric's runtime access widener had no such restriction.
 */
@Mixin(net.minecraft.world.item.component.ResolvableProfile.class)
public interface ResolvableProfileInvoker {

    @Invoker("unpack")
    com.mojang.datafixers.util.Either cardforge$unpack();
}
