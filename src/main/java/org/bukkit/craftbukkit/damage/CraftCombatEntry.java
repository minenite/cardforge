package org.bukkit.craftbukkit.damage;

import io.papermc.paper.world.damagesource.CombatEntry;
import io.papermc.paper.world.damagesource.FallLocationType;
import org.bukkit.damage.DamageSource;

/**
 * One recorded hit from an entity's combat tracker.
 */
public class CraftCombatEntry implements CombatEntry {

    private final net.minecraft.world.damagesource.CombatEntry handle;

    public CraftCombatEntry(net.minecraft.world.damagesource.CombatEntry handle) {
        this.handle = handle;
    }

    public net.minecraft.world.damagesource.CombatEntry getHandle() {
        return this.handle;
    }

    @Override
    public DamageSource getDamageSource() {
        return new CraftDamageSource(this.handle.source());
    }

    @Override
    public float getDamage() {
        return this.handle.damage();
    }

    @Override
    public FallLocationType getFallLocationType() {
        return fromNms(this.handle.fallLocation());
    }

    @Override
    public float getFallDistance() {
        return this.handle.fallDistance();
    }

    /**
     * The two sets of fall locations are the same eight values under the same ids,
     * so they are matched by id rather than kept in a hand-written table that
     * would silently drop anything added later.
     */
    public static FallLocationType fromNms(net.minecraft.world.damagesource.FallLocation fallLocation) {
        if (fallLocation == null) return null;
        for (FallLocationType type : new FallLocationType[]{
                FallLocationType.GENERIC, FallLocationType.LADDER, FallLocationType.VINES,
                FallLocationType.WEEPING_VINES, FallLocationType.TWISTING_VINES,
                FallLocationType.SCAFFOLDING, FallLocationType.OTHER_CLIMBABLE, FallLocationType.WATER}) {
            if (type.id().equals(fallLocation.id())) return type;
        }
        return FallLocationType.GENERIC;
    }

    public static net.minecraft.world.damagesource.FallLocation toNms(FallLocationType type) {
        return type == null ? null : new net.minecraft.world.damagesource.FallLocation(type.id());
    }
}
