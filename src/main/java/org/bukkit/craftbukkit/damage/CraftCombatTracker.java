package org.bukkit.craftbukkit.damage;

import java.util.List;
import java.util.stream.Collectors;

import io.papermc.paper.world.damagesource.CombatEntry;
import io.papermc.paper.world.damagesource.CombatTracker;
import io.papermc.paper.world.damagesource.FallLocationType;
import org.bukkit.entity.LivingEntity;
import org.cardboardpowered.bridge.world.entity.EntityBridge;
import org.minenite.cardforge.mixin.invoker.CombatTrackerAccessor;

/**
 * The running record of what has hit an entity recently, which drives death
 * messages and the "was killed while fighting" wording.
 */
public class CraftCombatTracker implements CombatTracker {

    private final net.minecraft.world.damagesource.CombatTracker handle;

    public CraftCombatTracker(net.minecraft.world.damagesource.CombatTracker handle) {
        this.handle = handle;
    }

    private CombatTrackerAccessor accessor() {
        return (CombatTrackerAccessor) this.handle;
    }

    @Override
    public LivingEntity getEntity() {
        return (LivingEntity) ((EntityBridge) (Object) this.accessor().cardforge$getMob()).getBukkitEntity();
    }

    @Override
    public List<CombatEntry> getCombatEntries() {
        return this.accessor().cardforge$getEntries().stream()
                .map(entry -> (CombatEntry) new CraftCombatEntry(entry))
                .collect(Collectors.toList());
    }

    @Override
    public void setCombatEntries(List<CombatEntry> entries) {
        List<net.minecraft.world.damagesource.CombatEntry> nms = this.accessor().cardforge$getEntries();
        nms.clear();
        if (entries != null) {
            for (CombatEntry entry : entries) {
                nms.add(toNms(entry));
            }
        }
    }

    @Override
    public CombatEntry computeMostSignificantFall() {
        net.minecraft.world.damagesource.CombatEntry entry = this.handle.getMostSignificantFall();
        return entry == null ? null : new CraftCombatEntry(entry);
    }

    @Override
    public boolean isInCombat() {
        return this.accessor().cardforge$isInCombat();
    }

    @Override
    public boolean isTakingDamage() {
        return this.accessor().cardforge$isTakingDamage();
    }

    @Override
    public int getCombatDuration() {
        return this.handle.getCombatDuration();
    }

    @Override
    public void addCombatEntry(CombatEntry entry) {
        this.accessor().cardforge$getEntries().add(toNms(entry));
    }

    @Override
    public net.kyori.adventure.text.Component getDeathMessage() {
        return io.papermc.paper.adventure.PaperAdventure.asAdventure(this.handle.getDeathMessage());
    }

    @Override
    public void resetCombatState() {
        this.handle.recheckStatus();
    }

    @Override
    public FallLocationType calculateFallLocationType() {
        return CraftCombatEntry.fromNms(
                net.minecraft.world.damagesource.FallLocation.getCurrentFallLocation(this.accessor().cardforge$getMob()));
    }

    @Override
    public int getLastDamageTime() {
        return this.accessor().cardforge$getLastDamageTime();
    }

    private static net.minecraft.world.damagesource.CombatEntry toNms(CombatEntry entry) {
        if (entry instanceof CraftCombatEntry craft) {
            return craft.getHandle();
        }
        return new net.minecraft.world.damagesource.CombatEntry(
                ((CraftDamageSource) entry.getDamageSource()).getHandle(),
                entry.getDamage(),
                CraftCombatEntry.toNms(entry.getFallLocationType()),
                entry.getFallDistance());
    }
}
