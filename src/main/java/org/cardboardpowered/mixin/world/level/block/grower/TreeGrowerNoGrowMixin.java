package org.cardboardpowered.mixin.world.level.block.grower;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * Belt-and-suspenders: BOP (and others) may override {@code SaplingBlock.advanceTree},
 * but all saplings still grow through {@link TreeGrower#growTree}.
 */
@Mixin(TreeGrower.class)
public class TreeGrowerNoGrowMixin {

    @Inject(method = "growTree", at = @At("HEAD"), cancellable = true)
    private void cardboard$noTreeGrow(
            ServerLevel level,
            ChunkGenerator generator,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
