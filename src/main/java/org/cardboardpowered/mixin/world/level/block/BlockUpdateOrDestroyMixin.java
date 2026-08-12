package org.cardboardpowered.mixin.world.level.block;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fire {@link BlockPhysicsEvent} when neighbor updates would replace/destroy a
 * block (saplings, torches, carpets, etc.). Vanilla never called Bukkit here.
 */
@Mixin(Block.class)
public class BlockUpdateOrDestroyMixin {

    @Inject(
            method = "updateOrDestroy(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;II)V",
            at = @At("HEAD"),
            cancellable = true)
    private static void cardboard$blockPhysics(
            BlockState oldState,
            BlockState newState,
            LevelAccessor level,
            BlockPos pos,
            int flags,
            int maxUpdateDepth,
            CallbackInfo ci) {
        if (oldState == newState) {
            return;
        }
        if (!(level instanceof Level world) || world.isClientSide()) {
            return;
        }
        BlockPhysicsEvent event = new BlockPhysicsEvent(
                CraftBlock.at(world, pos),
                CraftBlockData.fromData(newState));
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
