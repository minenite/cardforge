package org.minenite.cardforge.mixin.invoker;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor for methods NeoForge will not let an access transformer widen,
 * because widening them would narrow an existing subclass override.
 * Fabric's runtime access widener had no such restriction.
 */
@Mixin(net.minecraft.world.entity.LivingEntity.class)
public interface LivingEntityInvoker {

    @Invoker("doAutoAttackOnTouch")
    void cardforge$doAutoAttackOnTouch(net.minecraft.world.entity.LivingEntity target);

    @Invoker("getHurtSound")
    net.minecraft.sounds.SoundEvent cardforge$getHurtSound(net.minecraft.world.damagesource.DamageSource source);

    @Invoker("getSoundVolume")
    float cardforge$getSoundVolume();
}
