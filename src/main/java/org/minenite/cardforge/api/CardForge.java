package org.minenite.cardforge.api;

import java.util.Collection;
import java.util.Optional;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import net.neoforged.neoforge.capabilities.BlockCapability;

/**
 * Opt-in access to the NeoForge server underneath the Bukkit layer.
 *
 * <p>CardForge runs Bukkit/Spigot/Paper plugins on a real NeoForge server. Those
 * plugins keep working untouched and know nothing about NeoForge - they are
 * best-effort compatibility plugins, and nothing here changes how they behave. A
 * plugin becomes NeoForge-aware only by asking for it, which is what this
 * interface is: calling {@link #get()} is the opt-in, and there is no other way
 * in.
 *
 * <p>The bridge is deliberately thin. It does not wrap or reimplement NeoForge;
 * where NeoForge already has a good type it is handed back directly, so a
 * CardForge-native plugin talks to NeoForge rather than to a parallel API that
 * would inevitably lag behind it.
 *
 * <p>Typical use, guarded so the plugin still loads on a plain Paper server:
 *
 * <pre>{@code
 * CardForge.getIfPresent().ifPresent(cardforge -> {
 *     if (cardforge.isModLoaded("waystones")) {
 *         // ... integrate
 *     }
 * });
 * }</pre>
 */
public interface CardForge {

    /**
     * The active bridge.
     *
     * @throws IllegalStateException if this is not a CardForge server
     */
    static CardForge get() {
        return org.minenite.cardforge.api.impl.CardForgeImpl.get();
    }

    /** The active bridge, or empty when not running on CardForge. */
    static Optional<CardForge> getIfPresent() {
        return org.minenite.cardforge.api.impl.CardForgeImpl.getIfPresent();
    }

    /** The NeoForge version this server is running. */
    String neoForgeVersion();

    /** The Minecraft version this server is running. */
    String minecraftVersion();

    /** Every loaded mod, CardForge and NeoForge itself included. */
    Collection<ModInfo> mods();

    /** A loaded mod by id. */
    Optional<ModInfo> mod(String modId);

    /** Whether a mod is loaded. Cheaper and clearer than scanning {@link #mods()}. */
    boolean isModLoaded(String modId);

    /** Modded content by its real namespaced id. */
    ModdedRegistry registry();

    /**
     * Looks up a NeoForge block capability at a Bukkit block.
     *
     * <p>This is the thing a plain Bukkit plugin genuinely cannot do. Capabilities
     * are how NeoForge mods expose machine inventories, fluid tanks and energy
     * buffers, and no Bukkit interface describes them - {@code Block#getState()}
     * on a modded block entity gives back a generic TileState with no typed
     * access to any of it. Going through the capability means reading a mod's
     * inventory the same way another mod would, rather than guessing at NBT.
     *
     * <p>The capability object is NeoForge's own, so callers use NeoForge's API
     * directly:
     *
     * <pre>{@code
     * cardforge.blockCapability(block, Capabilities.Item.BLOCK, BlockFace.UP)
     *          .ifPresent(handler -> ...);
     * }</pre>
     *
     * @param block the Bukkit block to query
     * @param capability the NeoForge capability to look up
     * @param side the face to query from, or null for a sideless lookup
     * @return the capability instance, or empty if the block does not provide it
     */
    <T> Optional<T> blockCapability(Block block,
                                    BlockCapability<T, net.minecraft.core.Direction> capability,
                                    BlockFace side);

    /**
     * Looks up a sided NeoForge block capability, passing the context through
     * unchanged.
     *
     * <p>The general form, for capabilities whose context is not a Direction.
     */
    <T, C> Optional<T> blockCapability(Block block, BlockCapability<T, C> capability, C context);
}
