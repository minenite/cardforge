package org.cardboardpowered.mixin.network.protocol.game;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lets the API build player-info packets that vanilla has no constructor for.
 * Vanilla only ever assembles these from live ServerPlayers, so per-viewer
 * variations - hiding one player from one person's tab list - are impossible
 * without writing the entry list directly.
 */
@Mixin(ClientboundPlayerInfoUpdatePacket.class)
public interface ClientboundPlayerInfoUpdatePacketAccessor {

    @Mutable
    @Accessor("actions")
    void cardboard$setActions(EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions);

    @Mutable
    @Accessor("entries")
    void cardboard$setEntries(List<ClientboundPlayerInfoUpdatePacket.Entry> entries);
}
