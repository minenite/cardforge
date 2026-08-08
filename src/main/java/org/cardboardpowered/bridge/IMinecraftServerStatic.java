package org.cardboardpowered.bridge;

import org.bukkit.craftbukkit.CraftServer;

import net.minecraft.server.MinecraftServer;

public interface IMinecraftServerStatic {


	public static MinecraftServer getServer() {
		return CraftServer.server;
	}


}
