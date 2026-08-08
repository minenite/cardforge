package me.isaiah.common.cmixin;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface IMixinBlockEntity {

    public CompoundTag I_createNbtWithIdentifyingData();

    /**
     */
    public void IC$add_bee_to_beehive(ServerLevel world, int rand);
    
    /**
     */
    public void IC$read_nbt(CompoundTag nbt);
    
    
    /**
     */
    public BlockEntity IC$create_from_nbt(BlockPos pos, BlockState state, CompoundTag nbt);
    
}