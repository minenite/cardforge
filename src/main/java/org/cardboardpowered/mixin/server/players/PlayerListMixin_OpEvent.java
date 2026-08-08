package org.cardboardpowered.mixin.server.players;

import org.cardboardpowered.bridge.world.entity.EntityBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;

/**
 * Recalculates a connected player's permissions when they are opped or deopped.
 *
 * <p>{@code CraftPlayer#setOp} does this already, but the vanilla {@code /op}
 * and {@code /deop} commands never go through it - they call
 * {@code PlayerList#op} and {@code #deop} directly. Bukkit's Permissible caches
 * its computed permissions and only refreshes when told to, so without this a
 * deopped player kept every operator permission for the rest of their session
 * and only lost them on reconnect.
 *
 * <p>That is a privilege-escalation-shaped bug on a public server: it silently
 * defeated WorldGuard, which asked whether the player had
 * {@code worldguard.region.bypass.world}, was told yes by a stale operator, and
 * correctly stood aside. Upstream CraftBukkit patches PlayerList for exactly
 * this reason; the port had not carried that across.
 */
@Mixin(PlayerList.class)
public class PlayerListMixin_OpEvent {

    @Inject(method = "op(Lnet/minecraft/server/players/NameAndId;)V", at = @At("TAIL"))
    private void cardforge$recalculateOnOp(NameAndId profile, CallbackInfo ci) {
        cardforge$recalculate(profile);
    }

    @Inject(method = "deop(Lnet/minecraft/server/players/NameAndId;)V", at = @At("TAIL"))
    private void cardforge$recalculateOnDeop(NameAndId profile, CallbackInfo ci) {
        cardforge$recalculate(profile);
    }

    private void cardforge$recalculate(NameAndId profile) {
        if (profile == null) {
            return;
        }
        try {
            PlayerList self = (PlayerList) (Object) this;
            ServerPlayer player = self.getPlayer(profile.id());
            if (player == null) {
                return;
            }
            org.bukkit.craftbukkit.entity.CraftEntity bukkit =
                    ((EntityBridge) (Object) player).getBukkitEntity();
            if (bukkit instanceof org.bukkit.entity.Player bukkitPlayer) {
                bukkitPlayer.recalculatePermissions();
            }
        } catch (Throwable t) {
            // Never let this break the op command itself; the operator list has
            // already been updated by the time this runs.
            org.cardboardpowered.CardboardMod.LOGGER.warning(
                    "Could not recalculate permissions after an op change: " + t);
        }
    }
}
