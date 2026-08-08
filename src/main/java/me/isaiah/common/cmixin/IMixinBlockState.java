package me.isaiah.common.cmixin;

import me.isaiah.common.fabric.FabricWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import me.isaiah.common.block.IBlockState;

public interface IMixinBlockState {

    public default BlockState IgetMC() {
        return (BlockState)(Object)this;
    }

    public IBlockState getAsICommon(FabricWorld w, BlockPos pos);

}