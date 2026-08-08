package org.cardboardpowered.mixin.server.level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import org.cardboardpowered.bridge.server.level.ChunkHolderBridge;

@Mixin(ChunkHolder.class)
public class ChunkHolderMixin implements ChunkHolderBridge {

	@Shadow
	public int oldTicketLevel;

	public LevelChunk getFullChunkNow() {
		// Note: We use the oldTicketLevel for isLoaded checks.
		if (!ChunkLevel.fullStatus(this.oldTicketLevel).isOrAfter(FullChunkStatus.FULL)) return null;
		return this.getFullChunkNowUnchecked();
	}

	public LevelChunk getFullChunkNowUnchecked() {
		return (LevelChunk) ((ChunkHolder)(Object)this).getChunkIfPresentUnchecked(ChunkStatus.FULL);
	}

}