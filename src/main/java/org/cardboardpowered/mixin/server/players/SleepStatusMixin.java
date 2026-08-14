package org.cardboardpowered.mixin.server.players;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lets a player be left out of the sleep count.
 *
 * <p>Bukkit's "sleeping ignored" says a player should not hold the night up -
 * used for staff who are watching rather than playing. There was nowhere for that
 * answer to be asked, so the setter stored a value nothing read and a single
 * hidden admin could keep a server awake all night.
 *
 * <p>Counted the same way a spectator already is, since that is exactly the
 * question being asked here.
 */
@Mixin(SleepStatus.class)
public class SleepStatusMixin {

    @Redirect(
            method = "update",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;isSpectator()Z"))
    private boolean cardboard$ignoreSleepingIgnored(ServerPlayer player) {
        if (player.isSpectator()) {
            return true;
        }
        org.bukkit.entity.Player bukkit = (org.bukkit.entity.Player)
                ((org.cardboardpowered.bridge.server.level.ServerPlayerBridge) (Object) player).getBukkitEntity();
        return bukkit.isSleepingIgnored();
    }
}
