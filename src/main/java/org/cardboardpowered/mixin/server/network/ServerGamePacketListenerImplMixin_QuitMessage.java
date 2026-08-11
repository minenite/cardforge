package org.cardboardpowered.mixin.server.network;

import net.minecraft.network.chat.Component;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Stops vanilla announcing a departure before plugins have had their say.
 *
 * <p>{@code removePlayerFromWorld} broadcasts "left the game" and only afterwards
 * is {@code PlayerList.remove} reached, which is where {@code PlayerQuitEvent}
 * fires. A plugin changing or clearing the message therefore changed nothing:
 * the line players saw had already been sent, and the event's message was read
 * by nobody.
 *
 * <p>Suppressed here so the announcement can be made once the event has run, in
 * {@code PlayerListMixin}. The only cost is that it arrives a moment later in the
 * disconnect sequence, which nothing observes.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin_QuitMessage {

    @Redirect(
            method = "removePlayerFromWorld",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;"
                            + "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void cardboard$deferQuitAnnouncement(PlayerList playerList, Component message, boolean overlay) {
        // Deliberately nothing: PlayerListMixin announces it after the event.
    }
}
