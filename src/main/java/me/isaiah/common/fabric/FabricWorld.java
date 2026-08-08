package me.isaiah.common.fabric;

import me.isaiah.common.block.IBlockState;
import me.isaiah.common.cmixin.IMixinBlockState;
import me.isaiah.common.cmixin.IMixinWorld;
import me.isaiah.common.world.IWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class FabricWorld implements IWorld {

	public Level mc; // Backwards Compact
    public ServerLevel mc1;
    private String name;

    @Deprecated
    public FabricWorld(String name, Level world) {
        this.mc = (ServerLevel) world;
        this.mc1 = (ServerLevel) world;
        this.name = name;
    }
    
    public FabricWorld(String name, ServerLevel world) {
        this.mc = world;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

	/*
    private GameRules gr() {
        return mc1.getGameRules();
    }
	*/

    @Override
    @Deprecated
	public boolean doDaylightCycle() {
        return true; // TODO
		// return gr().get(GameRules.DO_DAYLIGHT_CYCLE).get();
    }

    @Override
    public boolean isDay() {
		// #if mc211
		// return mc.isDay();
    	// #elif mc201 
    	// return mc.isDay();
		// #else
        return mc.isBrightOutside();
		// #endif
    }

    @Override
    public int getLoadedChunkCount() {
        return ((ServerLevel)mc).getChunkSource().getLoadedChunksCount();
    }

    @Override
    public IBlockState getBlockState(int x, int y, int z) {
        BlockPos pos = new BlockPos(x,y,z);
        return ((IMixinBlockState)mc.getBlockState(pos)).getAsICommon(this, pos);
    }

	@Override
	public boolean isTheEnd(ServerLevel world) {
		return ((IMixinWorld) mc1).icommon$is_the_end();
	}

	@Override
	public BlockPos getSpawnPoint() {
		return ((IMixinWorld) mc1).icommon$get_spawn_point();
	}

}