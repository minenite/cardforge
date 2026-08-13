package org.cardboardpowered.mixin.world.level.block;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.NetherFungusBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fire {@link BlockPhysicsEvent} when neighbor updates would replace/destroy a
 * block (saplings, torches, carpets, etc.). Vanilla never called Bukkit here.
 *
 * <p>Decorative saplings are hard-blocked from popping to air even before Bukkit
 * plugins are enabled (chunk load / early ticks), which was dropping item
 * entities all over warz.
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
        // Always keep saplings / fungi / leaves as blocks — never pop into item drops.
        if (newState.isAir() && cardboard$isDecorativePlant(oldState)) {
            ci.cancel();
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

    @Unique
    private static boolean cardboard$isDecorativePlant(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof SaplingBlock
                || block instanceof BambooSaplingBlock
                || block instanceof NetherFungusBlock
                || block instanceof LeavesBlock) {
            return true;
        }
        try {
            return state.is(BlockTags.LEAVES);
        } catch (IllegalStateException ignored) {
            return false;
        }
    }
}
