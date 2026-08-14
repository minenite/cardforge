package org.cardboardpowered.mixin.server.level;

import net.minecraft.server.level.ServerChunkCache;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Says which world is spawning, so its own limits can be applied.
 *
 * <p>The mob cap is decided inside {@code NaturalSpawner.SpawnState}, which knows
 * nothing about the level it belongs to, and Bukkit's spawn limits are per world.
 * The level is recorded around the spawn pass and read back there.
 *
 * <p>Safe because this all happens on the server thread, one level at a time: the
 * value is set and cleared within a single call, and never read outside it.
 */
@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheMixin_SpawnLimits {

    @Shadow
    @org.spongepowered.asm.mixin.Final
    public net.minecraft.server.level.ServerLevel level;

    @Inject(method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;J)V", at = @At("HEAD"))
    private void cardboard$spawningLevelStart(net.minecraft.util.profiling.ProfilerFiller profiler,
                                              long time, CallbackInfo ci) {
        org.cardboardpowered.impl.world.CraftWorld.setSpawningLevel(this.level);
    }

    @Inject(method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;J)V", at = @At("RETURN"))
    private void cardboard$spawningLevelEnd(net.minecraft.util.profiling.ProfilerFiller profiler,
                                            long time, CallbackInfo ci) {
        org.cardboardpowered.impl.world.CraftWorld.setSpawningLevel(null);
    }
}
