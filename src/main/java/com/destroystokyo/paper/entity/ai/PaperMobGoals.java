package com.destroystokyo.paper.entity.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import com.google.common.base.Preconditions;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Mob;

/**
 * Read and write access to a mob's AI goals.
 *
 * Vanilla splits goals across two selectors - one for behaviour, one for target
 * picking - and which one a goal belongs to is decided by whether it carries the
 * TARGET flag, exactly as vanilla itself decides where to register a goal.
 */
public class PaperMobGoals implements MobGoals {

    @Override
    public <T extends Mob> void addGoal(T mob, int priority, Goal<T> goal) {
        Preconditions.checkArgument(goal != null, "Goal cannot be null");
        PaperCustomGoal<T> wrapped = new PaperCustomGoal<>(goal);
        selectorFor(mob, goal.getTypes()).addGoal(priority, wrapped);
    }

    @Override
    public <T extends Mob> void removeGoal(T mob, Goal<T> goal) {
        Preconditions.checkArgument(goal != null, "Goal cannot be null");
        for (GoalSelector selector : selectors(mob)) {
            for (WrappedGoal wrapped : List.copyOf(selector.getAvailableGoals())) {
                if (wrapped.getGoal() instanceof PaperCustomGoal<?> custom && custom.getHandle() == goal) {
                    selector.removeGoal(wrapped.getGoal());
                } else if (wrapped.getGoal() instanceof net.minecraft.world.entity.ai.goal.Goal nms
                        && goal instanceof PaperVanillaGoal<?> vanilla && vanilla.getHandle() == nms) {
                    selector.removeGoal(nms);
                }
            }
        }
    }

    @Override
    public <T extends Mob> void removeAllGoals(T mob) {
        for (GoalSelector selector : selectors(mob)) {
            selector.removeAllGoals(goal -> true);
        }
    }

    @Override
    public <T extends Mob> void removeAllGoals(T mob, GoalType type) {
        net.minecraft.world.entity.ai.goal.Goal.Flag flag = paperToVanilla(type);
        for (GoalSelector selector : selectors(mob)) {
            selector.removeAllGoals(goal -> flag != null && goal.getFlags().contains(flag));
        }
    }

    @Override
    public <T extends Mob> void removeGoal(T mob, GoalKey<T> key) {
        for (Goal<T> goal : this.getGoals(mob, key)) {
            this.removeGoal(mob, goal);
        }
    }

    @Override
    public <T extends Mob> boolean hasGoal(T mob, GoalKey<T> key) {
        return !this.getGoals(mob, key).isEmpty();
    }

    @Override
    public <T extends Mob> Goal<T> getGoal(T mob, GoalKey<T> key) {
        Collection<Goal<T>> goals = this.getGoals(mob, key);
        return goals.isEmpty() ? null : goals.iterator().next();
    }

    @Override
    public <T extends Mob> Collection<Goal<T>> getGoals(T mob, GoalKey<T> key) {
        Preconditions.checkArgument(key != null, "GoalKey cannot be null");
        List<Goal<T>> matches = new ArrayList<>();
        for (Goal<T> goal : this.getAllGoals(mob)) {
            if (key.equals(goal.getKey())) matches.add(goal);
        }
        return matches;
    }

    @Override
    public <T extends Mob> Collection<Goal<T>> getAllGoals(T mob) {
        List<Goal<T>> goals = new ArrayList<>();
        for (GoalSelector selector : selectors(mob)) {
            for (WrappedGoal wrapped : selector.getAvailableGoals()) {
                goals.add(toApi(mob, wrapped.getGoal()));
            }
        }
        return goals;
    }

    @Override
    public <T extends Mob> Collection<Goal<T>> getAllGoals(T mob, GoalType type) {
        return filter(this.getAllGoals(mob), type, true);
    }

    @Override
    public <T extends Mob> Collection<Goal<T>> getAllGoalsWithout(T mob, GoalType type) {
        return filter(this.getAllGoals(mob), type, false);
    }

    @Override
    public <T extends Mob> Collection<Goal<T>> getRunningGoals(T mob) {
        List<Goal<T>> goals = new ArrayList<>();
        for (GoalSelector selector : selectors(mob)) {
            for (WrappedGoal wrapped : selector.getAvailableGoals()) {
                if (wrapped.isRunning()) goals.add(toApi(mob, wrapped.getGoal()));
            }
        }
        return goals;
    }

    @Override
    public <T extends Mob> Collection<Goal<T>> getRunningGoals(T mob, GoalType type) {
        return filter(this.getRunningGoals(mob), type, true);
    }

    @Override
    public <T extends Mob> Collection<Goal<T>> getRunningGoalsWithout(T mob, GoalType type) {
        return filter(this.getRunningGoals(mob), type, false);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Mob> Goal<T> toApi(T mob, net.minecraft.world.entity.ai.goal.Goal goal) {
        if (goal instanceof PaperCustomGoal<?> custom) {
            return (Goal<T>) custom.getHandle();
        }
        return new PaperVanillaGoal<>(goal, (Class<T>) mob.getClass().getInterfaces()[0]);
    }

    private static <T extends Mob> Collection<Goal<T>> filter(Collection<Goal<T>> goals, GoalType type, boolean keep) {
        Preconditions.checkArgument(type != null, "GoalType cannot be null");
        List<Goal<T>> matches = new ArrayList<>();
        for (Goal<T> goal : goals) {
            if (goal.getTypes().contains(type) == keep) matches.add(goal);
        }
        return matches;
    }

    private static net.minecraft.world.entity.Mob handle(Mob mob) {
        Preconditions.checkArgument(mob != null, "Mob cannot be null");
        return (net.minecraft.world.entity.Mob) ((CraftEntity) mob).getHandle();
    }

    private static GoalSelector[] selectors(Mob mob) {
        net.minecraft.world.entity.Mob nms = handle(mob);
        return new GoalSelector[]{nms.goalSelector, nms.targetSelector};
    }

    private static GoalSelector selectorFor(Mob mob, EnumSet<GoalType> types) {
        net.minecraft.world.entity.Mob nms = handle(mob);
        return types.contains(GoalType.TARGET) ? nms.targetSelector : nms.goalSelector;
    }

    public static net.minecraft.world.entity.ai.goal.Goal.Flag paperToVanilla(GoalType type) {
        return switch (type) {
            case MOVE -> net.minecraft.world.entity.ai.goal.Goal.Flag.MOVE;
            case LOOK -> net.minecraft.world.entity.ai.goal.Goal.Flag.LOOK;
            case JUMP -> net.minecraft.world.entity.ai.goal.Goal.Flag.JUMP;
            case TARGET -> net.minecraft.world.entity.ai.goal.Goal.Flag.TARGET;
            // UNKNOWN_BEHAVIOR is Paper's name for a goal with no vanilla flags.
            default -> null;
        };
    }

    public static EnumSet<net.minecraft.world.entity.ai.goal.Goal.Flag> paperToVanilla(EnumSet<GoalType> types) {
        EnumSet<net.minecraft.world.entity.ai.goal.Goal.Flag> flags =
                EnumSet.noneOf(net.minecraft.world.entity.ai.goal.Goal.Flag.class);
        for (GoalType type : types) {
            net.minecraft.world.entity.ai.goal.Goal.Flag flag = paperToVanilla(type);
            if (flag != null) flags.add(flag);
        }
        return flags;
    }

    public static EnumSet<GoalType> vanillaToPaper(EnumSet<net.minecraft.world.entity.ai.goal.Goal.Flag> flags) {
        EnumSet<GoalType> types = EnumSet.noneOf(GoalType.class);
        for (net.minecraft.world.entity.ai.goal.Goal.Flag flag : flags) {
            if (flag == net.minecraft.world.entity.ai.goal.Goal.Flag.MOVE) types.add(GoalType.MOVE);
            else if (flag == net.minecraft.world.entity.ai.goal.Goal.Flag.LOOK) types.add(GoalType.LOOK);
            else if (flag == net.minecraft.world.entity.ai.goal.Goal.Flag.JUMP) types.add(GoalType.JUMP);
            else if (flag == net.minecraft.world.entity.ai.goal.Goal.Flag.TARGET) types.add(GoalType.TARGET);
        }
        if (types.isEmpty()) types.add(GoalType.UNKNOWN_BEHAVIOR);
        return types;
    }
}
