package com.destroystokyo.paper.entity.ai;

import java.util.EnumSet;

import org.bukkit.entity.Mob;

/**
 * Wraps a plugin's Goal so vanilla's goal selector can run it.
 */
public class PaperCustomGoal<T extends Mob> extends net.minecraft.world.entity.ai.goal.Goal {

    private final Goal<T> handle;

    public PaperCustomGoal(Goal<T> handle) {
        this.handle = handle;
        this.setFlags(PaperMobGoals.paperToVanilla(handle.getTypes()));
    }

    public Goal<T> getHandle() {
        return this.handle;
    }

    @Override
    public boolean canUse() {
        return this.handle.shouldActivate();
    }

    @Override
    public boolean canContinueToUse() {
        return this.handle.shouldStayActive();
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
    public boolean requiresUpdateEveryTick() {
        // Plugin goals expect tick() every tick; vanilla otherwise runs them on a
        // slower schedule and the goal appears to stutter.
        return true;
    }
}
