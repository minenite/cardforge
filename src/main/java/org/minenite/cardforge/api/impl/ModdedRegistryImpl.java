package org.minenite.cardforge.api.impl;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.minenite.cardforge.api.ModdedRegistry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * Looks modded content up through NeoForge's registries rather than through the
 * mangled enum names CardForge has to invent for the Bukkit API.
 */
public final class ModdedRegistryImpl implements ModdedRegistry {

    @Override
    public NamespacedKey keyOf(Material material) {
        return material.getKey();
    }

    @Override
    public Optional<Material> material(NamespacedKey key) {
        if (key == null) {
            return Optional.empty();
        }
        // Match on the real key rather than reconstructing the enum name, which
        // is derived by mangling and is not reliably reversible.
        for (Material material : Material.values()) {
            try {
                if (key.equals(material.getKey())) {
                    return Optional.of(material);
                }
            } catch (IllegalArgumentException legacy) {
                // Legacy materials refuse getKey(); they are never modded.
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<org.bukkit.entity.EntityType> entityType(NamespacedKey key) {
        if (key == null) {
            return Optional.empty();
        }
        for (org.bukkit.entity.EntityType type : org.bukkit.entity.EntityType.values()) {
            try {
                if (key.equals(type.getKey())) {
                    return Optional.of(type);
                }
            } catch (IllegalArgumentException | UnsupportedOperationException ignored) {
                // Not every EntityType carries a key.
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isModded(Material material) {
        try {
            return !NamespacedKey.MINECRAFT.equals(material.getKey().getNamespace());
        } catch (IllegalArgumentException legacy) {
            return false;
        }
    }

    @Override
    public Set<NamespacedKey> blocks(String namespace) {
        return keysIn(BuiltInRegistries.BLOCK.keySet(), namespace);
    }

    @Override
    public Set<NamespacedKey> items(String namespace) {
        return keysIn(BuiltInRegistries.ITEM.keySet(), namespace);
    }

    @Override
    public Set<NamespacedKey> entities(String namespace) {
        return keysIn(BuiltInRegistries.ENTITY_TYPE.keySet(), namespace);
    }

    private static Set<NamespacedKey> keysIn(Set<Identifier> ids, String namespace) {
        Set<NamespacedKey> out = new LinkedHashSet<>();
        for (Identifier id : ids) {
            if (namespace == null || namespace.equals(id.getNamespace())) {
                out.add(CraftNamespacedKey.fromMinecraft(id));
            }
        }
        return out;
    }
}
