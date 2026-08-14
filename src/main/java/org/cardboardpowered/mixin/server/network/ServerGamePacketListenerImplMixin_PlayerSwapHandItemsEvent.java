package org.cardboardpowered.mixin.server.network;

import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.cardboardpowered.bridge.server.level.ServerPlayerBridge;
import org.cardboardpowered.util.MixinInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla swaps main/offhand on {@code SWAP_ITEM_WITH_OFFHAND} (F) with no Bukkit
 * event. WarZ zeroing, zip-tie restraint, and drone orbit all listen for
 * {@link PlayerSwapHandItemsEvent}.
 */
@MixinInfo(events = {"PlayerSwapHandItemsEvent"})
@Mixin(value = ServerGamePacketListenerImpl.class, priority = 800)
public class ServerGamePacketListenerImplMixin_PlayerSwapHandItemsEvent {

    @Shadow
    public ServerPlayer player;

    @Inject(
            method = "handlePlayerAction",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket;getAction()Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket$Action;",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void cardboard$playerSwapHandItems(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (packet.getAction() != ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
            return;
        }
        if (this.player.isSpectator()) {
            return;
        }
        Player bukkit = (Player) ((ServerPlayerBridge) (Object) this.player).getBukkitEntity();
        org.bukkit.inventory.ItemStack main = CraftItemStack.asBukkitCopy(
                this.player.getItemInHand(InteractionHand.MAIN_HAND));
        org.bukkit.inventory.ItemStack off = CraftItemStack.asBukkitCopy(
                this.player.getItemInHand(InteractionHand.OFF_HAND));
        PlayerSwapHandItemsEvent event = new PlayerSwapHandItemsEvent(bukkit, main, off);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            this.player.containerMenu.sendAllDataToRemote();
            ci.cancel();
        }
    }
}
