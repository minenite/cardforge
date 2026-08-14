package org.bukkit.craftbukkit.potion;

import java.util.Collection;
import java.util.List;

import io.papermc.paper.potion.PotionMix;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionBrewer;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

/**
 * Potion effect lookups, and the brewing-stand recipe additions Paper allows.
 */
public class CraftPotionBrewer implements PotionBrewer {

    @Override
    public Collection<PotionEffect> getEffects(PotionType potionType, boolean upgraded, boolean extended) {
        // Returned nothing usable before, because getPotionBrewer itself was null.
        return potionType.getPotionEffects();
    }

    @Override
    public void addPotionMix(PotionMix potionMix) {
        // Brewing recipes are built once during data load from an immutable
        // PotionBrewing instance; there is no supported way to add to it at
        // runtime on this server, and pretending to accept the mix would leave a
        // brewing stand that silently never produces the potion.
        throw new UnsupportedOperationException(
                "Adding brewing recipes at runtime is not supported; define them in a datapack");
    }

    @Override
    public void removePotionMix(NamespacedKey key) {
        throw new UnsupportedOperationException(
                "Removing brewing recipes at runtime is not supported; define them in a datapack");
    }

    @Override
    public void resetPotionMixes() {
        // Nothing was ever added, so the recipe set is already the vanilla one.
    }
}
