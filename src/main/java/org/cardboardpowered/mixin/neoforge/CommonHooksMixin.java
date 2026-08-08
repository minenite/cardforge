package org.cardboardpowered.mixin.neoforge;

import java.util.List;

import org.bukkit.craftbukkit.block.CraftBlockStates;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.block.BlockPlaceEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;

/**
 * Fires Bukkit's block-place events on NeoForge's placement path.
 *
 * <p>Cardboard implemented BlockPlaceEvent by {@code @Overwrite}-ing
 * {@code ItemStack#useOn} with the vanilla body plus its own block capture. On
 * NeoForge that is actively harmful, because NeoForge rewrote {@code useOn}: on
 * a dedicated server it no longer runs the vanilla body at all, it fires
 * {@code UseItemOnBlockEvent} and then returns
 * {@code CommonHooks.onPlaceItemIntoWorld(context)}. Overwriting the method
 * therefore deleted both - every NeoForge mod listening for item-use-on-block or
 * for block placement silently stopped being called, while the Bukkit event kept
 * working, so nothing looked broken from the Bukkit side.
 *
 * <p>The fix is to put the Bukkit semantics inside the replacement rather than
 * restoring the vanilla path. {@code onPlaceItemIntoWorld} already does exactly
 * what Cardboard's overwrite did, and does it better: it captures block
 * snapshots, fires its place events, and on cancellation restores every snapshot
 * in reverse and resends the player's inventory slot. Cardboard's version forced
 * the position to AIR rather than restoring the previous state, handled only the
 * first block of a multi-block placement, and blind-grew the held stack by one.
 *
 * <p>So both events now fire at the same decision point, either side can cancel,
 * and NeoForge's own restore logic performs the revert. Multi-block placements
 * reach Bukkit as BlockMultiPlaceEvent, which the overwrite never supported.
 */
@Mixin(value = CommonHooks.class, remap = false)
public class CommonHooksMixin {

    @Redirect(
            method = "onPlaceItemIntoWorld",
            at = @At(value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/event/EventHooks;onBlockPlace("
                            + "Lnet/minecraft/world/entity/Entity;"
                            + "Lnet/neoforged/neoforge/common/util/BlockSnapshot;"
                            + "Lnet/minecraft/core/Direction;)Z"))
    private static boolean cardforge$onBlockPlace(Entity entity, BlockSnapshot snapshot, Direction direction) {
        // NeoForge first: a mod cancelling placement should not need a Bukkit
        // plugin's opinion, and skipping the Bukkit event here keeps plugins from
        // observing a placement that is not going to happen.
        if (EventHooks.onBlockPlace(entity, snapshot, direction)) {
            return true;
        }
        return cardforge$bukkitCancels(entity, List.of(snapshot), direction);
    }

    @Redirect(
            method = "onPlaceItemIntoWorld",
            at = @At(value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/event/EventHooks;onMultiBlockPlace("
                            + "Lnet/minecraft/world/entity/Entity;"
                            + "Ljava/util/List;"
                            + "Lnet/minecraft/core/Direction;)Z"))
    private static boolean cardforge$onMultiBlockPlace(Entity entity, List<BlockSnapshot> snapshots, Direction direction) {
        if (EventHooks.onMultiBlockPlace(entity, snapshots, direction)) {
            return true;
        }
        return cardforge$bukkitCancels(entity, snapshots, direction);
    }

    /**
     * @return true if a Bukkit plugin cancelled, in NeoForge's "cancelled" sense
     */
    private static boolean cardforge$bukkitCancels(Entity entity, List<BlockSnapshot> snapshots, Direction direction) {
        // Only player placements carry a Bukkit block-place event; a dispenser or
        // a mod placing blocks directly has its own events elsewhere.
        if (!(entity instanceof Player player) || snapshots.isEmpty()) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        try {
            BlockSnapshot first = snapshots.get(0);
            // The event wants the state that was replaced, which is what the
            // snapshot recorded before the placement ran.
            org.bukkit.block.BlockState replaced =
                    CraftBlockStates.getBlockState(level, first.getPos(), first.getState(), null);
            // The block placed against, which is what the player actually clicked.
            net.minecraft.core.BlockPos clicked = first.getPos().relative(direction.getOpposite());

            BlockPlaceEvent event = CraftEventFactory.callBlockPlaceEvent(
                    level, player, InteractionHand.MAIN_HAND, replaced, clicked);
            return event.isCancelled() || !event.canBuild();
        } catch (Throwable t) {
            // A failure here must not swallow the placement: NeoForge has already
            // decided to allow it, so let it through rather than cancelling on an
            // internal error.
            org.cardboardpowered.CardboardMod.LOGGER.warning(
                    "Could not fire BlockPlaceEvent for a NeoForge placement: " + t);
            return false;
        }
    }
}
