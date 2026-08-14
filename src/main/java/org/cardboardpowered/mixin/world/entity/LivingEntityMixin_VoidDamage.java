package org.cardboardpowered.mixin.world.entity;

import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies a world's void damage settings.
 *
 * <p>Vanilla hurts for a fixed four points below the world, and the API lets a
 * world say otherwise or switch it off. Those settings had nowhere to be read, so
 * they were stored and ignored - which reads as working.
 *
 * <p>A world that has not been configured is left entirely alone, so this changes
 * nothing until somebody asks it to.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin_VoidDamage {

    @Inject(method = "onBelowWorld", at = @At("HEAD"), cancellable = true)
    private void cardboard$voidDamage(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        org.bukkit.World world = ((org.cardboardpowered.bridge.world.entity.EntityBridge) (Object) self)
                .getBukkitEntity().getWorld();
        if (!(world instanceof org.cardboardpowered.impl.world.CraftWorld craft)
                || !craft.hasVoidDamageOverride()) {
            return;
        }
        if (!craft.isVoidDamageEnabled()) {
            ci.cancel();
            return;
        }
        // Below the world by more than this world asks for, and hurting by the
        // amount it asks for rather than vanilla's four.
        if (self.getY() >= self.level().getMinY() + craft.getVoidDamageMinBuildHeightOffset()) {
            ci.cancel();
            return;
        }
        self.hurt(self.damageSources().fellOutOfWorld(), craft.getVoidDamageAmount());
        ci.cancel();
    }
}
