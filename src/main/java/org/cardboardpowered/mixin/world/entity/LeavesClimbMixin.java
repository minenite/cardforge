package org.cardboardpowered.mixin.world.entity;

import org.cardboardpowered.util.FoliageBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Players climb leaf blocks like vines. Leaves already have empty collision,
 * so without a solid wall behind them {@code horizontalCollision} never fires —
 * boost upward when the player is moving or jumping inside leaves.
 */
@Mixin(LivingEntity.class)
public abstract class LeavesClimbMixin {
    @Shadow
    protected boolean jumping;

    @Shadow
    public float xxa;

    @Shadow
    public float zza;

    @Inject(method = "onClimbable", at = @At("RETURN"), cancellable = true)
    private void cardboard$climbLeaves(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player) || self.isSpectator()) {
            return;
        }
        if (playerInLeaves(self)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "handleRelativeFrictionAndCalculateMovement", at = @At("RETURN"), cancellable = true)
    private void cardboard$boostThroughLeaves(
            Vec3 input, float friction, CallbackInfoReturnable<Vec3> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player) || self.isSpectator()) {
            return;
        }
        if (!playerInLeaves(self)) {
            return;
        }
        Vec3 movement = cir.getReturnValue();
        if (movement.y >= 0.2) {
            return;
        }
        if (this.jumping || this.xxa != 0.0F || this.zza != 0.0F) {
            cir.setReturnValue(new Vec3(movement.x, 0.2, movement.z));
        }
    }

    private static boolean playerInLeaves(LivingEntity self) {
        BlockPos feet = self.blockPosition();
        BlockState atFeet = self.getInBlockState();
        return FoliageBlocks.isLeaves(atFeet)
                || FoliageBlocks.isLeaves(self.level().getBlockState(feet.above()));
    }
}
