package org.cardboardpowered.bridge.level;

import net.minecraft.world.level.LevelSettings;

/**
 * Interface for the LevelSettings mixin.
 * 
 * @see {@link org.cardboardpowered.mixin.world.level.MixinLevelSettings}
 * @implSpec {@link https://github.com/PaperMC/Paper/blob/main/paper-server/patches/sources/net/minecraft/world/level/LevelSettings.java.patch}
 * @since Cardboard 26.1
 */
public interface ILevelSettings {

	/**
	 * Create a new LevelSettings with the given level name
	 */
	LevelSettings cardboard$withLevelName(String name);

	/**
	 * Create a new LevelSettings with the given hardcore setting
	 */
	LevelSettings cardboard$withHardcore(boolean hardcore);

}
