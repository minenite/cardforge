package me.isaiah.common.cmixin;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Old 1.16/1.17
 * @deprecated Removed in Cardboard 1.21.10
 */
@Deprecated
public interface IMixinHeightmap {

    /**
     * Method for {@link Heightmap#setRawData}
     * 
     * @implNote 1.16: setTo(long[]);
     * @implNote 1.17: setTo(Chunk, Type, long[])
     */
    public void I_setTo(ChunkAccess chunk, Heightmap.Types type, long[] ls);

}