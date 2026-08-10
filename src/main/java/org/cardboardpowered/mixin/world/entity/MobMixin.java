package org.cardboardpowered.mixin.world.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.bukkit.event.entity.EntityTargetEvent;
import org.cardboardpowered.bridge.world.entity.MobBridge;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.cardboardpowered.bridge.world.entity.EntityBridge;
import org.cardboardpowered.bridge.server.level.ServerLevelBridge;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobMixin extends LivingEntity implements MobBridge, EntityBridge {
    @Shadow
    @Nullable
    public LivingEntity target;

    @Shadow
    public abstract @Nullable LivingEntity getTarget();

    protected MobMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    public void setTargetCraftBukkit(LivingEntity livingEntity, CallbackInfo ci) {
        // Was passing the shadow field `target` - the mob's *current* target - rather
        // than `livingEntity`, the one being set. cardboard$setTarget opens with
        // `if (this.getTarget() == target) return false`, so it compared the field to
        // itself, returned immediately every time, and EntityTargetLivingEntityEvent
        // never fired at all. Vanilla targeting still ran, so mobs behaved normally
        // and nothing looked wrong; plugins simply could not see or veto targeting.
        boolean set = this.cardboard$setTarget(livingEntity, EntityTargetEvent.TargetReason.UNKNOWN);
        if (set) { // Let the other mods call their @Inject if set is false.
            ci.cancel();
        }
    }

    @Unique
    private static final java.util.concurrent.atomic.AtomicBoolean cardboard$warnedUnknownTarget =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    @Override
    public boolean cardboard$setTarget(@Nullable LivingEntity target, EntityTargetEvent.@Nullable TargetReason reason) {
        if (this.getTarget() == target) {
            return false;
        }
        if (reason != null) {
            if (reason == EntityTargetEvent.TargetReason.UNKNOWN && this.getTarget() != null && target == null) {
                reason = this.getTarget().isAlive() ? EntityTargetEvent.TargetReason.FORGOT_TARGET : EntityTargetEvent.TargetReason.TARGET_DIED;
            }
            if (reason == EntityTargetEvent.TargetReason.UNKNOWN && cardboard$warnedUnknownTarget.compareAndSet(false, true)) {
                // Fires on every generic setTarget call, i.e. constantly once mobs are active.
                // Report it once per run so the signal survives without flooding the log.
                ((ServerLevelBridge)this.level()).getCraftServer().getLogger().log(java.util.logging.Level.WARNING,
                        "Unknown target reason, please report on the issue tracker (further occurrences suppressed)", new Exception());
            }
            CraftLivingEntity ctarget = null;
            if (target != null) {
                ctarget = (CraftLivingEntity) ((org.cardboardpowered.bridge.world.entity.EntityBridge) (Object) target).getBukkitEntity();
            }
            org.bukkit.event.entity.EntityTargetLivingEntityEvent event = new org.bukkit.event.entity.EntityTargetLivingEntityEvent(this.getBukkitEntity(), ctarget, reason);
            if (!event.callEvent()) {
                return false;
            }

            if (event.getTarget() != null) {
                target = ((CraftLivingEntity) event.getTarget()).getHandle();
            } else {
                target = null;
            }
        }

        // NeoForge fires LivingChangeTargetEvent inside setTarget and lets a mod
        // redirect or veto the new target. The hook above cancels that method to run
        // this instead, so the mod event never fired and no mod could influence
        // targeting - invisibly, since mobs still acquired targets normally.
        //
        // The plugin has already had its say; the mod now gets the same chance on the
        // resulting target, which keeps both able to veto.
        try {
            net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent changeTarget =
                    net.neoforged.neoforge.common.CommonHooks.onLivingChangeTarget((Mob) (Object) this, target,
                            net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent.LivingTargetType.MOB_TARGET);
            if (changeTarget.isCanceled()) {
                return false;
            }
            target = changeTarget.getNewAboutToBeSetTarget();
        } catch (Throwable t) {
            org.cardboardpowered.CardboardMod.LOGGER.warning("Could not fire LivingChangeTargetEvent: " + t);
        }

        this.target = target;
        return true;
        // CraftBukkit end
    }
}
