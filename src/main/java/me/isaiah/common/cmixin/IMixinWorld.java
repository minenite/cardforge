package me.isaiah.common.cmixin;

import me.isaiah.common.world.IWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.IdMap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public interface IMixinWorld {

    public IWorld icommon();

    /**
     * Creates a new instance of {@link BiomeArray}
     * 
     * @implNote 1.16  - new BiomeArray(IndexedIterable, ChunkPos, BiomeSource)
     * @implNote 1.17+ - new BiomeArray(IndexedIterable, World, ChunkPos, BiomeSource)
     */
    // @Deprecated
    public Object I_newBiomeArray(IdMap<Biome> biomes, Level world, ChunkPos chunkPos, BiomeSource biomeSource);
    
    /**
     * 1.17/1.18
     */
    public PalettedContainer<net.minecraft.world.level.block.state.BlockState> I_emptyBlockIDs();

    /**
     * <= 1.18.1: World.getBiomeForNoiseGen(int, int, int)
     * >= 1.18.2: World.getBiomeForNoiseGen(int, int, int).value()
     */
    public Biome I_get_biome_for_noise_gen(int biomeX, int biomeY, int biomeZ);
    
    /**
     */
    public MapItemSavedData IC$get_map_state(int id);

    /**
     */
	public boolean icommon$is_the_end();

	/**
	 */
	public BlockPos icommon$get_spawn_point();
    
}