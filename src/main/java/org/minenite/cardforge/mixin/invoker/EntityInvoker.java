package org.minenite.cardforge.mixin.invoker;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor for methods NeoForge will not let an access transformer widen,
 * because widening them would narrow an existing subclass override.
 * Fabric's runtime access widener had no such restriction.
 */
@Mixin(net.minecraft.world.entity.Entity.class)
public interface EntityInvoker {

    @Invoker("getFireImmuneTicks")
    int cardforge$getFireImmuneTicks();

    @Invoker("getSwimHighSpeedSplashSound")
    net.minecraft.sounds.SoundEvent cardforge$getSwimHighSpeedSplashSound();

    @Invoker("getSwimSound")
    net.minecraft.sounds.SoundEvent cardforge$getSwimSound();

    @Invoker("getSwimSplashSound")
    net.minecraft.sounds.SoundEvent cardforge$getSwimSplashSound();
}
