package org.minenite.cardforge.api;

import java.util.Optional;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;

/**
 * Looks modded content up by its real namespaced id.
 *
 * <p>Plain Bukkit plugins can reach modded content, but only through names
 * CardForge invented for it: a block registered as {@code waystones:andesite_waystone}
 * becomes {@code Material.WAYSTONES_ANDESITE_WAYSTONE}. That mangling is lossy and
 * not something a plugin should be hard-coding. This lets a CardForge-native
 * plugin work in the mod's own terms instead.
 */
public interface ModdedRegistry {

    /** The namespaced id a Material really has, modded or vanilla. */
    NamespacedKey keyOf(Material material);

    /** The Material for a namespaced id, or empty if nothing registered it. */
    Optional<Material> material(NamespacedKey key);

    /** The EntityType for a namespaced id, or empty if nothing registered it. */
    Optional<EntityType> entityType(NamespacedKey key);

    /** True if this Material came from a mod rather than vanilla Minecraft. */
    boolean isModded(Material material);

    /** Every block id registered under a namespace, e.g. all of {@code waystones}. */
    Set<NamespacedKey> blocks(String namespace);

    /** Every item id registered under a namespace. */
    Set<NamespacedKey> items(String namespace);

    /** Every entity id registered under a namespace. */
    Set<NamespacedKey> entities(String namespace);
}
