package me.isaiah.common.event.world;

import me.isaiah.common.event.Event;
import net.minecraft.world.level.chunk.LevelChunk;

public class WorldChunkInitEvent extends Event {

    private LevelChunk chunk;

    public WorldChunkInitEvent(LevelChunk chunk) {
        this.chunk = chunk;
    }

    public LevelChunk getWorld() {
        return chunk;
    }

}