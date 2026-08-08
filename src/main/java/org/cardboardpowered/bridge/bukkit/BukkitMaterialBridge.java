package org.cardboardpowered.bridge.bukkit;

import org.cardboardpowered.impl.CardboardModdedMaterial;

public interface BukkitMaterialBridge {

    boolean isModded();

    CardboardModdedMaterial getModdedData();

    void setModdedData(CardboardModdedMaterial data);

    /**
     * Points a dynamically added Material at the namespaced key it really has.
     *
     * Material's constructor always derives its key as minecraft:&lt;lowercased
     * enum name&gt;, which is right for vanilla and wrong for anything a mod
     * registered: WAYSTONES_BOUND_SCROLL would claim
     * minecraft:waystones_bound_scroll. That key is not cosmetic - the memoized
     * itemType and blockType suppliers resolve through it, so a wrong key makes
     * asItemType() and asBlockType() return null, which in turn makes isItem(),
     * isBlock(), new ItemStack(...) and createBlockData(...) all fail.
     *
     * Must be called before anything reads asItemType()/asBlockType(), since
     * those suppliers memoize their first result.
     */
    void cardboard$setKey(org.bukkit.NamespacedKey key);

}