package org.cardboardpowered.mixin.world.level;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lets a world's configured spawn limits decide the mob cap.
 *
 * <p>Bukkit lets a plugin set how many of each category may exist, and those
 * settings had nowhere to be read: the API stored a number and the spawner went
 * on using the category's own per-chunk figure. A stored value nothing reads is
 * worse than an unimplemented one, because it looks like it worked.
 *
 * <p>The cap is the only thing substituted. Everything else about how the server
 * decides to spawn - where, what, how often - is untouched.
 */
@Mixin(targets = "net.minecraft.world.level.NaturalSpawner$SpawnState")
public class NaturalSpawnerMixin {

    @Redirect(
            method = "canSpawnForCategoryGlobal",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/MobCategory;getMaxInstancesPerChunk()I"))
    private int cardboard$spawnLimit(MobCategory category) {
        Integer configured = org.cardboardpowered.impl.world.CraftWorld.currentSpawnLimit(category);
        return configured != null ? configured : category.getMaxInstancesPerChunk();
    }
}
