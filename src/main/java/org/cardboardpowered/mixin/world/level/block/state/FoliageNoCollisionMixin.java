package org.cardboardpowered.mixin.world.level.block.state;

import org.cardboardpowered.util.FoliageBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Server-side: foliage has no collision so MineniteClient walk-through matches.
 * Outline / getShape is left alone (client handles that).
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class FoliageNoCollisionMixin {
    @Inject(
            method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"),
            cancellable = true)
    private void cardboard$emptyFoliageCollisionCached(
            BlockGetter level, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir) {
        if (FoliageBlocks.isFoliage((BlockState) (Object) this)) {
            cir.setReturnValue(Shapes.empty());
        }
    }

    @Inject(
            method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"),
            cancellable = true)
    private void cardboard$emptyFoliageCollision(
            BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (FoliageBlocks.isFoliage((BlockState) (Object) this)) {
            cir.setReturnValue(Shapes.empty());
        }
    }
}
