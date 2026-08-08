package org.cardboardpowered.impl;

import org.bukkit.Material;

/**
 * Holds the live set of Materials, including ones added for modded content.
 *
 * Enum extension writes Material's private static final $VALUES array through
 * Unsafe, and that write lands - but values() compiles to a getstatic on a
 * static final field, which HotSpot constant-folds once the class is
 * initialised. values() is hot during registration, so it folds early and then
 * keeps returning the pre-extension array: every modded Material stays reachable
 * by name and by key while being invisible to any plugin that iterates values().
 *
 * Rather than trying to defeat the folding at the read site, the extended array
 * is published here explicitly and BukkitMaterialMixin's values() returns it.
 * Until it is published, callers get the vanilla array, which is correct - no
 * modded materials exist yet at that point.
 */
public final class MaterialValues {

    private static volatile Material[] values;

    private MaterialValues() {
    }

    public static void set(Material[] all) {
        values = all;
    }

    /** The live array, or null if modded materials have not been registered. */
    public static Material[] get() {
        return values;
    }
}
