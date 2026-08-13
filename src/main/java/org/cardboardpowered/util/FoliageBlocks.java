package org.cardboardpowered.util;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GlowLichenBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.HangingMossBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MossyCarpetBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Matches MineniteClient foliage hitbox stripping so walk-through leaves
 * do not rubberband against the server.
 *
 * <p>Tag lookups are deferred until tags bind — shape-cache init during
 * {@code Blocks} clinit is too early for {@code state.is(tag)}.
 */
public final class FoliageBlocks {
    private FoliageBlocks() {}

    public static boolean isFoliage(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof LeavesBlock
                || block instanceof VegetationBlock
                || block instanceof VineBlock
                || block instanceof GrowingPlantBlock
                || block instanceof HangingMossBlock
                || block instanceof GlowLichenBlock
                || block instanceof MossyCarpetBlock) {
            return true;
        }
        if (block == Blocks.MOSS_CARPET || block == Blocks.PALE_MOSS_CARPET) {
            return true;
        }
        try {
            return state.is(BlockTags.LEAVES)
                    || state.is(BlockTags.FLOWERS)
                    || state.is(BlockTags.REPLACEABLE_BY_TREES)
                    || isReplaceablePlant(state);
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    /** Leaf blocks only — used for climb-through trees. */
    public static boolean isLeaves(BlockState state) {
        if (state.getBlock() instanceof LeavesBlock) {
            return true;
        }
        try {
            return state.is(BlockTags.LEAVES);
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    private static boolean isReplaceablePlant(BlockState state) {
        if (!state.is(BlockTags.REPLACEABLE)) {
            return false;
        }
        Block block = state.getBlock();
        return block != Blocks.AIR
                && block != Blocks.CAVE_AIR
                && block != Blocks.VOID_AIR
                && block != Blocks.WATER
                && block != Blocks.LAVA
                && block != Blocks.FIRE
                && block != Blocks.SOUL_FIRE
                && block != Blocks.SNOW
                && block != Blocks.STRUCTURE_VOID
                && block != Blocks.LIGHT
                && block != Blocks.BUBBLE_COLUMN;
    }
}
