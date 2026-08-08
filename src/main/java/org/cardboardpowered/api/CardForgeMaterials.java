package org.cardboardpowered.api;

import org.bukkit.Material;
import org.cardboardpowered.impl.MaterialValues;

/**
 * The dynamic Material set, including entries added for NeoForge mod content.
 *
 * <p>Enum extension adds modded materials by writing Material's private static
 * final {@code $VALUES} array through Unsafe. The write lands - reading the field
 * reflectively shows every modded entry - but {@code Material.values()} compiles
 * to a {@code getstatic} on a static final field, which HotSpot constant-folds
 * once the class is initialised. {@code values()} is hot during registration, so
 * it folds early and then keeps handing back the pre-extension array. No amount
 * of patching the read site changes that, because the fold has already happened
 * by the time any plugin runs.
 *
 * <p>So the read site is not where this is solved. Plugin classes are rewritten
 * as they load, by {@link org.cardboardpowered.plugin.MaterialValuesRewriter},
 * to call this method instead. The descriptor is identical to
 * {@code Material.values()} - {@code ()[Lorg/bukkit/Material;} - so the rewrite
 * is a drop-in substitution at the call site and plugins need no source changes.
 *
 * <p>Plugins may also call this directly if they prefer to be explicit.
 */
public final class CardForgeMaterials {

    private CardForgeMaterials() {
    }

    /**
     * Every Material, vanilla and modded.
     *
     * <p>Returns a fresh array on each call, matching the contract of the
     * {@code values()} method it stands in for.
     */
    public static Material[] values() {
        Material[] live = MaterialValues.get();
        if (live == null) {
            // Modded materials have not been registered yet, so the folded
            // constant is still the whole truth.
            return Material.values();
        }
        return live.clone();
    }
}
