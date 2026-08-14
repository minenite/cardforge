package com.destroystokyo.paper.entity.ai;

import java.util.EnumSet;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;

/**
 * The other direction: a vanilla goal presented through the plugin API, so
 * getAllGoals can report the AI a mob actually has.
 */
public class PaperVanillaGoal<T extends Mob> implements Goal<T> {

    private final net.minecraft.world.entity.ai.goal.Goal handle;
    private final GoalKey<T> key;

    @SuppressWarnings("unchecked")
    public PaperVanillaGoal(net.minecraft.world.entity.ai.goal.Goal handle, Class<T> entityClass) {
        this.handle = handle;
        // Vanilla goals have no registry key, so the class name is used - stable
        // for a given server build and unique per goal type.
        String name = handle.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
        if (name.isEmpty()) name = "anonymous";
        this.key = GoalKey.of(entityClass, NamespacedKey.minecraft(name));
    }

    public net.minecraft.world.entity.ai.goal.Goal getHandle() {
        return this.handle;
    }

    @Override
    public boolean shouldActivate() {
        return this.handle.canUse();
    }

    @Override
    public boolean shouldStayActive() {
        return this.handle.canContinueToUse();
    }

    @Override
    public void start() {
        this.handle.start();
    }

    @Override
    public void stop() {
        this.handle.stop();
    }

    @Override
    public void tick() {
        this.handle.tick();
    }

    @Override
    public GoalKey<T> getKey() {
        return this.key;
    }

    @Override
    public EnumSet<GoalType> getTypes() {
        return PaperMobGoals.vanillaToPaper(this.handle.getFlags());
    }
}
