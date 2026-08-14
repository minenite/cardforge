package org.cardboardpowered.mixin.world.level;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EntityGetter;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets a player stop drawing mobs towards them.
 *
 * <p>Bukkit's "affects spawning" says a player should not count when the game
 * looks for somebody to spawn mobs near - used for staff moving through a world
 * without stirring it up. The setting had nowhere to be read, so it stored a
 * value and mobs kept spawning around them.
 *
 * <p>Hooked on the distance-based lookup the spawner uses. Only players who have
 * turned it off are skipped, so nothing changes for anybody else.
 */
@Mixin(EntityGetter.class)
public interface EntityGetterMixin_AffectsSpawning {

    @Inject(method = "getNearestPlayer(DDDDZ)Lnet/minecraft/world/entity/player/Player;",
            at = @At("RETURN"), cancellable = true)
    private void cardboard$skipNonSpawningPlayers(double x, double y, double z, double range,
                                                  boolean creativePlayers,
                                                  CallbackInfoReturnable<Player> cir) {
        Player found = cir.getReturnValue();
        if (!(found instanceof net.minecraft.server.level.ServerPlayer server)) {
            return;
        }
        org.bukkit.entity.Player bukkit = (org.bukkit.entity.Player)
                ((org.cardboardpowered.bridge.server.level.ServerPlayerBridge) (Object) server).getBukkitEntity();
        if (!bukkit.getAffectsSpawning()) {
            // As far as spawning is concerned there is nobody here.
            cir.setReturnValue(null);
        }
    }
}
