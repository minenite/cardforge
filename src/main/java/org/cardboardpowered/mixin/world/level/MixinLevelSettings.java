package org.cardboardpowered.mixin.world.level;

import org.cardboardpowered.bridge.level.ILevelSettings;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.level.LevelSettings;

@Mixin(LevelSettings.class)
public class MixinLevelSettings implements ILevelSettings{

	/**
	 * Cardboard - Helper method to get the "this" reference as the correct type.
	 */
	private LevelSettings cb$thiz() {
		return (LevelSettings) (Object) this;
	}
	
	// Paper start
	@Override
	public LevelSettings cardboard$withLevelName(String name) {
		return new LevelSettings(name, cb$thiz().gameType, cb$thiz().difficultySettings, cb$thiz().allowCommands, cb$thiz().dataConfiguration);
	}

	@Override
	public LevelSettings cardboard$withHardcore(boolean hardcore) {
		return new LevelSettings(
				cb$thiz().levelName,
				cb$thiz().gameType,
				new LevelSettings.DifficultySettings(
						cb$thiz().difficultySettings.difficulty(), hardcore, cb$thiz().difficultySettings.locked()
				), cb$thiz().allowCommands, cb$thiz().dataConfiguration);
	}
	// Paper end

}
