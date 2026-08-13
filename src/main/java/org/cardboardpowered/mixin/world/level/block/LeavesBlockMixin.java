package org.cardboardpowered.mixin.world.level.block;

import org.cardboardpowered.util.MixinInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Decorative / map leaves never decay. Covers oak, spruce, birch, jungle,
 * acacia, dark oak, mangrove, cherry, pale oak, azalea — every
 * {@link LeavesBlock} subclass (26.2 {@code randomTick} only drops + removes).
 *
 * <p>The iCommon {@code LeavesDecayEvent} mixin targeted
 * {@code LeavesBlock.dropResources}, which does not exist (it lives on
 * {@code Block}), so the Bukkit cancel never ran and leaves still vanished.
 */
@MixinInfo(events = {"LeavesDecayEvent"})
@Mixin(LeavesBlock.class)
public class LeavesBlockMixin {

    @Inject(method = "isRandomlyTicking", at = @At("HEAD"), cancellable = true)
    private void cardboard$noLeafRandomTicks(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "decaying", at = @At("HEAD"), cancellable = true)
    private void cardboard$neverDecaying(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void cardboard$noLeafDecayTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        ci.cancel();
    }
}
