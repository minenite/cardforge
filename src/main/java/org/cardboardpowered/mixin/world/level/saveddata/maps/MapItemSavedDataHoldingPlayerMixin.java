package org.cardboardpowered.mixin.world.level.saveddata.maps;

import java.util.ArrayList;
import java.util.Collection;

import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.map.CraftMapCursor;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.map.MapCursor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import org.cardboardpowered.bridge.world.entity.EntityBridge;
import org.cardboardpowered.bridge.world.level.saveddata.maps.MapItemSavedDataBridge;
import org.cardboardpowered.impl.map.MapViewImpl;
import org.cardboardpowered.impl.map.RenderData;

/**
 * Vanilla {@code nextUpdatePacket} only ships {@link MapItemSavedData#colors}.
 * Plugin {@link org.bukkit.map.MapRenderer}s paint into a Craft canvas — same as
 * Paper, we must render that buffer into the map packet or custom maps stay blank.
 */
@Mixin(MapItemSavedData.HoldingPlayer.class)
public abstract class MapItemSavedDataHoldingPlayerMixin {

    @Shadow
    @Final
    public Player player;

    @Shadow
    @Final
    MapItemSavedData this$0;

    @Shadow
    private boolean dirtyData;

    @Shadow
    private boolean dirtyDecorations;

    @Shadow
    private int tick;

    @Inject(method = "nextUpdatePacket", at = @At("HEAD"), cancellable = true)
    private void cardboard$renderPluginMaps(MapId mapId, CallbackInfoReturnable<Packet<?>> cir) {
        MapViewImpl view = ((MapItemSavedDataBridge) (Object) this.this$0).getMapViewBF();
        if (view == null || !view.hasCustomRenderers()) {
            return;
        }

        // Custom maps: refresh on a short interval even when vanilla colors are clean.
        if (!this.dirtyData && this.tick % 5 != 0) {
            this.tick++;
            cir.setReturnValue(null);
            return;
        }
        this.tick++;
        this.dirtyData = false;
        this.dirtyDecorations = false;

        CraftPlayer craftPlayer = (CraftPlayer) ((EntityBridge) this.player).getBukkitEntity();
        RenderData render = view.render(craftPlayer);

        Collection<MapDecoration> icons = new ArrayList<>();
        for (MapCursor cursor : render.cursors) {
            if (cursor.isVisible()) {
                icons.add(new MapDecoration(
                        CraftMapCursor.CraftType.bukkitToMinecraftHolder(cursor.getType()),
                        cursor.getX(),
                        cursor.getY(),
                        cursor.getDirection(),
                        CraftChatMessage.fromStringOrOptional(cursor.getCaption())));
            }
        }

        cir.setReturnValue(new ClientboundMapItemDataPacket(
                mapId,
                this.this$0.scale,
                this.this$0.locked,
                icons,
                new MapItemSavedData.MapPatch(0, 0, 128, 128, render.buffer)));
    }
}
