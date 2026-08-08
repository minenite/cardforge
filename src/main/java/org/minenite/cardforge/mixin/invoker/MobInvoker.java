package org.minenite.cardforge.mixin.invoker;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor for methods NeoForge will not let an access transformer widen,
 * because widening them would narrow an existing subclass override.
 * Fabric's runtime access widener had no such restriction.
 */
@Mixin(net.minecraft.world.entity.Mob.class)
public interface MobInvoker {

    @Invoker("getAmbientSound")
    net.minecraft.sounds.SoundEvent cardforge$getAmbientSound();

    // Mob only exposes setPersistenceRequired(), which can set the flag but never
    // clear it, so Bukkit's setRemoveWhenFarAway(true) needs the field directly.
    @org.spongepowered.asm.mixin.gen.Accessor("persistenceRequired")
    void cardforge$setPersistenceRequired(boolean value);
}
