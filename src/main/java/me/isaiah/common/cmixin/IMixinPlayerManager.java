package me.isaiah.common.cmixin;

import com.mojang.authlib.GameProfile;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public interface IMixinPlayerManager {

    public ServerPlayer InewPlayer(MinecraftServer server, ServerLevel world, GameProfile profile);

}