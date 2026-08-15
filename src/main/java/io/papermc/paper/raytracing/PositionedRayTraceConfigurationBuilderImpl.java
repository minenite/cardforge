package io.papermc.paper.raytracing;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

/**
 * Collects the arguments for World#rayTrace(Consumer). Nothing here traces
 * anything; it just records what the caller asked for.
 */
public class PositionedRayTraceConfigurationBuilderImpl implements PositionedRayTraceConfigurationBuilder {

    private Location start;
    private Vector direction;
    private double maxDistance;
    private FluidCollisionMode fluidCollisionMode = FluidCollisionMode.NEVER;
    private BlockCollisionMode blockCollisionMode = BlockCollisionMode.COLLIDER;
    private boolean ignorePassableBlocks;
    private double raySize = 0.0D;
    private Predicate<? super Entity> entityFilter;
    private Predicate<? super Block> blockFilter;
    private final Set<RayTraceTarget> targets = EnumSet.noneOf(RayTraceTarget.class);

    @Override
    public PositionedRayTraceConfigurationBuilder start(Location start) {
        this.start = start;
        return this;
    }

    @Override
    public PositionedRayTraceConfigurationBuilder direction(Vector direction) {
        this.direction = direction;
        return this;
    }

    @Override
    public PositionedRayTraceConfigurationBuilder maxDistance(double maxDistance) {
        this.maxDistance = maxDistance;
        return this;
    }

    @Override
    public PositionedRayTraceConfigurationBuilder fluidCollisionMode(FluidCollisionMode fluidCollisionMode) {
        this.fluidCollisionMode = fluidCollisionMode;
        return this;
    }

    @Override
    public PositionedRayTraceConfigurationBuilder blockCollisionMode(BlockCollisionMode blockCollisionMode) {
        this.blockCollisionMode = blockCollisionMode;
        return this;
    }

    @Override
    public PositionedRayTraceConfigurationBuilder ignorePassableBlocks(boolean ignorePassableBlocks) {
        this.ignorePassableBlocks = ignorePassableBlocks;
        return this;
    }

    @Override
    public PositionedRayTraceConfigurationBuilder raySize(double raySize) {
        this.raySize = raySize;
        return this;
    }

    @Override
    public PositionedRayTraceConfigurationBuilder entityFilter(Predicate<? super Entity> entityFilter) {
        this.entityFilter = entityFilter;
        return this;
    }

    @Override
    public PositionedRayTraceConfigurationBuilder blockFilter(Predicate<? super Block> blockFilter) {
        this.blockFilter = blockFilter;
        return this;
    }

    @Override
    public PositionedRayTraceConfigurationBuilder targets(RayTraceTarget target, RayTraceTarget... targets) {
        this.targets.clear();
        this.targets.add(target);
        java.util.Collections.addAll(this.targets, targets);
        return this;
    }

    public Location start() {
        return this.start;
    }

    public Vector direction() {
        return this.direction;
    }

    public double maxDistance() {
        return this.maxDistance;
    }

    public FluidCollisionMode fluidCollisionMode() {
        return this.fluidCollisionMode;
    }

    public boolean ignorePassableBlocks() {
        return this.ignorePassableBlocks;
    }

    public double raySize() {
        return this.raySize;
    }

    public Predicate<? super Entity> entityFilter() {
        return this.entityFilter;
    }

    public Predicate<? super Block> blockFilter() {
        return this.blockFilter;
    }

    public Set<RayTraceTarget> targets() {
        // No explicit call means both, which is what the single-shot rayTrace
        // overloads do.
        return this.targets.isEmpty() ? EnumSet.allOf(RayTraceTarget.class) : this.targets;
    }
}
