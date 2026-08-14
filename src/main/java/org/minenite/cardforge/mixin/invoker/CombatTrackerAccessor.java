package org.minenite.cardforge.mixin.invoker;

import java.util.List;

import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The combat tracker keeps everything the API wants to read - the hit list, who
 * it belongs to, and the combat timers - entirely private.
 */
@Mixin(CombatTracker.class)
public interface CombatTrackerAccessor {

    @Accessor("entries")
    List<CombatEntry> cardforge$getEntries();

    @Accessor("mob")
    LivingEntity cardforge$getMob();

    @Accessor("inCombat")
    boolean cardforge$isInCombat();

    @Accessor("takingDamage")
    boolean cardforge$isTakingDamage();

    @Accessor("lastDamageTime")
    int cardforge$getLastDamageTime();
}
