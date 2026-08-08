package me.isaiah.common.event.block;

import me.isaiah.common.event.Cancelable;
import me.isaiah.common.event.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public class LeavesDecayEvent extends Event implements Cancelable {

    public BlockState state;
    public ServerLevel world;
    public BlockPos pos;
    
    public boolean no;

    public LeavesDecayEvent(BlockState state, ServerLevel world, BlockPos pos) {
        this.state = state;
        this.world = world;
        this.pos = pos;
        this.no = false;
    }

	@Override
	public boolean isCanceled() {
		return no;
	}

	@Override
	public void setCanceled(boolean cancel) {
		this.no = cancel;
	}


}