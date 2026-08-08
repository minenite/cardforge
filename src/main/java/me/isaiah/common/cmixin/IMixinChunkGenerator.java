package me.isaiah.common.cmixin;

import net.minecraft.server.level.ServerLevel;

public interface IMixinChunkGenerator {

    public int IgetSpawnHeight(ServerLevel w);

}