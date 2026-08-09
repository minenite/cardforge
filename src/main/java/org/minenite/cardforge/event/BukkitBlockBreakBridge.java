package org.minenite.cardforge.event;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import org.cardboardpowered.bridge.world.entity.EntityBridge;
import org.bukkit.craftbukkit.block.CraftBlock;

/**
 * Drives Bukkit's {@link BlockBreakEvent} from NeoForge's {@code BreakBlockEvent}.
 *
 * <p>Cardboard fired the Bukkit event from a HEAD inject on
 * {@code ServerPlayerGameMode#destroyBlock}. On NeoForge that produced two
 * unreconciled events for one break, because NeoForge had already replaced the
 * top of that method with its own:
 *
 * <pre>
 * // Neo: Fire the BlockBreakEvent, and ignore the original
 * // ItemStack#canDestroyBlock check since the break event manages the status of it.
 * var event = CommonHooks.fireBlockBreak(level, gameModeForPlayer, player, pos, state);
 * if (event.isCanceled()) return false;
 * </pre>
 *
 * <p>The HEAD hook ran first, so a plugin cancelling the Bukkit event returned
 * before NeoForge's event fired at all and no mod ever saw the attempt; a mod
 * cancelling NeoForge's left plugins that had already run believing the break
 * had happened. The hook also recomputed by hand the very
 * {@code canDestroyBlock} guard NeoForge had deliberately deleted, so the sword
 * case was decided twice by two authorities, and it re-sent the block-update
 * packets that {@code fireBlockBreak} already sends itself.
 *
 * <p>Listening instead means one event source. NeoForge's pre-cancellation -
 * {@code canDestroyBlock}, {@code blockActionRestricted}, game-master blocks -
 * arrives as the initial cancelled state, which is exactly what Bukkit's
 * {@code isCancelled} is supposed to express, and a plugin can still override it
 * the way it would on Paper. Cancellation maps back so mod and plugin decisions
 * compose in both directions.
 *
 * <p>Not bridged: {@code BlockBreakEvent#setExpToDrop}. NeoForge's
 * {@code BreakBlockEvent} carries no experience - it moved to the drops path -
 * so a plugin setting it changes a value this event does not own. That is a
 * separate bridge against the drops event, not something to fake here.
 */
public final class BukkitBlockBreakBridge {

    private BukkitBlockBreakBridge() {
    }

    public static void register(IEventBus gameBus) {
        // receiveCancelled = true. NeoForge pre-cancels this event for the cases it
        // absorbed from vanilla - the item cannot destroy the block, the player is
        // action-restricted (adventure, spectator), a game-master block - and posts
        // it already cancelled. A plain addListener does not run for an event that
        // arrives cancelled, so the Bukkit event would simply never fire in those
        // cases. On Paper it does fire, cancelled, and a plugin may overturn it;
        // Cardboard's original hook reproduced that by pre-setting cancelled from
        // the sword check. Dropping the notification would have been a regression
        // hidden behind "the block did not break anyway".
        gameBus.addListener(true, BreakBlockEvent.class, BukkitBlockBreakBridge::onBreak);
    }

    private static void onBreak(BreakBlockEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        CraftServer server = CraftServer.INSTANCE;
        if (server == null) {
            // Breaks are possible during world generation and early startup, before
            // the Bukkit server exists. Nothing to notify, and constructing the
            // event would NPE.
            return;
        }

        org.bukkit.block.Block block = CraftBlock.at(event.getLevel(), event.getPos());

        BlockBreakEvent bukkitEvent = new BlockBreakEvent(
                block, (Player) ((EntityBridge) (Object) serverPlayer).getBukkitEntity());

        // NeoForge decides first - the vanilla guards it folded into this event are
        // the server's own rules, not a plugin's - and a plugin may then overturn
        // it, which is how the same event behaves on Paper.
        bukkitEvent.setCancelled(event.isCanceled());

        server.getPluginManager().callEvent(bukkitEvent);

        event.setCanceled(bukkitEvent.isCancelled());
    }
}
