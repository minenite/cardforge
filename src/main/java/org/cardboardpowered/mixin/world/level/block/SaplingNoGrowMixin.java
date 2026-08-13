package org.cardboardpowered.mixin.world.level.block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Warz decoration saplings must never become trees (random tick or bone meal).
 * Covers vanilla + subclasses that call {@code super} / {@link net.minecraft.world.level.block.grower.TreeGrower}.
 */
@Mixin(SaplingBlock.class)
public class SaplingNoGrowMixin {

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void cardboard$noSaplingRandomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "advanceTree", at = @At("HEAD"), cancellable = true)
    private void cardboard$noAdvanceTree(
            ServerLevel level, BlockPos pos, BlockState state, RandomSource random, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "performBonemeal", at = @At("HEAD"), cancellable = true)
    private void cardboard$noBonemealTree(
            ServerLevel level, RandomSource random, BlockPos pos, BlockState state, CallbackInfo ci) {
        ci.cancel();
    }
}
