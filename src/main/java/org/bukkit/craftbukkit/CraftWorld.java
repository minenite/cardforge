package org.bukkit.craftbukkit;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;

public class CraftWorld extends org.cardboardpowered.impl.world.CraftWorld {

	public CraftWorld(String name, ServerLevel world) {
		super(name, world);
	}

	public CraftWorld(ServerLevel world) {
		this(((ServerLevelData) world.getLevelData()).getLevelName(), world);
	}

}
