package org.cardboardpowered.mixin.world.entity.item;

import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fire {@link org.bukkit.event.entity.EntityChangeBlockEvent} before a gravity
 * block leaves its cell. Without this, plugins cannot cancel sand/gravel falls.
 */
@Mixin(FallingBlockEntity.class)
public class FallingBlockEntityMixin {

    @Inject(
            method = "fall",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"),
            cancellable = true)
    private static void cardboard$cancelFall(
            Level level,
            BlockPos pos,
            BlockState state,
            CallbackInfoReturnable<FallingBlockEntity> cir,
            @Local FallingBlockEntity entity) {
        if (!CraftEventFactory.callEntityChangeBlockEvent(
                entity, pos, state.getFluidState().createLegacyBlock())) {
            // Leave the original block in place; do not spawn the falling entity.
            cir.setReturnValue(entity);
        }
    }
}
