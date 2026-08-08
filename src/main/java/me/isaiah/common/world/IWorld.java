package me.isaiah.common.world;

import me.isaiah.common.block.IBlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * iCommon API for Worlds
 * 
 * @see {@link me.isaiah.common.cmixin.IMixinWorld}
 * @see {@link me.isaiah.common.fabric.FabricWorld}
 */
public interface IWorld {

    /**
     * Returns the level name of this world
     * EX: "world", "world_nether", etc..
     */
    public String getName();

    /**
     */
    public boolean isDay();

    /**
     * Return the value of the GameRule "doDaylightCycle"
     */
    boolean doDaylightCycle();

    /**
     * Return the count of the loaded chunks.
     */
    int getLoadedChunkCount();

    /**
     * Gets the BlockState at the given pos
     *
     * @param x X-value of the block
     * @param y Y-value of the block
     * @param z Z-value of the block
     */
    public IBlockState getBlockState(int x, int y, int z);
    
    /**
     * Is this World "The End" Dimension
     */
    public boolean isTheEnd(ServerLevel world);
    
    /**
     * Get the World's Spawn Point
     * 
     * @see {@link net.minecraft.core.BlockPos}
     */
    public BlockPos getSpawnPoint();
    

}