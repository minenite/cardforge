package org.cardboardpowered.impl;

import java.util.Collection;
import java.util.List;
import net.minecraft.world.level.dimension.end.DragonRespawnStage;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.bukkit.Location;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import io.papermc.paper.math.Position;

public class CardboardDragonBattle implements DragonBattle {

    private final EnderDragonFight handle;

    public CardboardDragonBattle(EnderDragonFight handle) {
        this.handle = handle;
    }

    private org.minenite.cardforge.mixin.invoker.EnderDragonFightAccessor accessor() {
        return (org.minenite.cardforge.mixin.invoker.EnderDragonFightAccessor) this.handle;
    }

    @Override
    public EnderDragon getEnderDragon() {
        java.util.UUID uuid = this.handle.dragonUUID();
        if (uuid == null) return null;
        net.minecraft.world.entity.Entity dragon = this.accessor().cardforge$getLevel().getEntity(uuid);
        return dragon == null ? null
                : (EnderDragon) ((org.cardboardpowered.bridge.world.entity.EntityBridge) (Object) dragon).getBukkitEntity();
    }

    @Override
    public BossBar getBossBar() {
        // Returned null, so nothing could recolour or retitle the dragon bar.
        return new org.bukkit.craftbukkit.boss.CraftBossBar(this.accessor().cardforge$getDragonEvent());
    }

    @Override
    public Location getEndPortalLocation() {
        net.minecraft.core.BlockPos pos = this.accessor().cardforge$getExitPortalLocation();
        if (pos == null) return null;
        return org.bukkit.craftbukkit.util.CraftLocation.toBukkit(pos,
                ((org.cardboardpowered.bridge.world.level.LevelBridge) (Object) this.accessor().cardforge$getLevel()).cardboard$getWorld());
    }

    @Override
    public boolean generateEndPortal(boolean withPortals) {
        // Claimed success while generating nothing. There is only ever one exit
        // portal, so a second request is a no-op and says so.
        if (this.accessor().cardforge$getExitPortalLocation() != null) return false;
        this.accessor().cardforge$spawnExitPortal(withPortals);
        return true;
    }

    @Override
    public boolean hasBeenPreviouslyKilled() {
        return handle.hasPreviouslyKilledDragon();
    }

    @Override
    public void initiateRespawn() {
        this.handle.tryRespawn();
    }

    @Override
    public RespawnPhase getRespawnPhase() {
        // Always reported NONE, so a plugin watching the respawn sequence never
        // saw it happen.
        return this.toBukkitRespawnPhase(this.accessor().cardforge$getRespawnStage());
    }

    @Override
    public boolean setRespawnPhase(RespawnPhase phase) {
        com.google.common.base.Preconditions.checkArgument(phase != null, "RespawnPhase cannot be null");
        // Claimed success without changing anything. A phase can only be set
        // while a respawn is actually under way.
        if (phase == RespawnPhase.NONE || this.accessor().cardforge$getRespawnStage() == null) {
            return false;
        }
        this.accessor().cardforge$setRespawnStage(this.toNMSRespawnPhase(phase));
        return true;
    }

    @Override
    public void resetCrystals() {
        this.handle.resetSpikeCrystals();
    }

    @Override
    public int hashCode() {
        return handle.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CardboardDragonBattle && ((CardboardDragonBattle) obj).handle == this.handle;
    }

    private RespawnPhase toBukkitRespawnPhase(DragonRespawnStage phase) {
        return (phase != null) ? RespawnPhase.values()[phase.ordinal()] : RespawnPhase.NONE;
    }

    private DragonRespawnStage toNMSRespawnPhase(RespawnPhase phase) {
        return (phase != RespawnPhase.NONE) ? DragonRespawnStage.values()[phase.ordinal()] : null;
    }
    
	@Override
	public boolean initiateRespawn(@Nullable Collection<EnderCrystal> enderCrystals) {
		if (this.handle.hasPreviouslyKilledDragon() && this.accessor().cardforge$getRespawnStage() == null) {
			if (enderCrystals != null) {
				// The four crystals that will be consumed by the respawn, chosen by
				// the caller instead of by the pillar scan.
				List<net.minecraft.world.entity.EntityReference<net.minecraft.world.entity.boss.enderdragon.EndCrystal>> crystals =
						new java.util.ArrayList<>();
				for (EnderCrystal crystal : enderCrystals) {
					crystals.add(net.minecraft.world.entity.EntityReference.of(
							(net.minecraft.world.entity.boss.enderdragon.EndCrystal)
									((org.bukkit.craftbukkit.entity.CraftEntity) crystal).getHandle()));
				}
				this.accessor().cardforge$setRespawnCrystals(crystals);
			}
			this.handle.tryRespawn();
			return this.accessor().cardforge$getRespawnStage() != null;
		}
		return false;
	}

	@Override
	public int getGatewayCount() {
		// The list holds the gateways still to be placed, so the number already
		// out there is what has been taken from the full twenty.
		return 20 - this.accessor().cardforge$getGateways().size();
	}

	@Override
	public boolean spawnNewGateway() {
		if (this.accessor().cardforge$getGateways().isEmpty()) return false;
		this.accessor().cardforge$spawnNewGateway();
		return true;
	}

	@Override
	public void spawnNewGateway(@NotNull Position position) {
		com.google.common.base.Preconditions.checkArgument(position != null, "Position cannot be null");
		this.accessor().cardforge$spawnNewGateway(
				new net.minecraft.core.BlockPos(position.blockX(), position.blockY(), position.blockZ()));
	}

	@Override
	public @NotNull @Unmodifiable List<EnderCrystal> getRespawnCrystals() {
		return this.resolveCrystals(this.accessor().cardforge$getRespawnCrystals());
	}

	@Override
	public @NotNull @Unmodifiable List<EnderCrystal> getHealingCrystals() {
		// The crystals on the pillars, which are what heal the dragon - as opposed
		// to the four on the portal that are consumed by a respawn.
		net.minecraft.core.BlockPos origin = this.accessor().cardforge$getOrigin();
		if (origin == null) return List.of();
		return this.accessor().cardforge$getLevel().getEntitiesOfClass(
						net.minecraft.world.entity.boss.enderdragon.EndCrystal.class,
						new net.minecraft.world.phys.AABB(origin).inflate(128.0D))
				.stream()
				.map(crystal -> (EnderCrystal) ((org.cardboardpowered.bridge.world.entity.EntityBridge) (Object) crystal).getBukkitEntity())
				.toList();
	}

	private List<EnderCrystal> resolveCrystals(
			List<net.minecraft.world.entity.EntityReference<net.minecraft.world.entity.boss.enderdragon.EndCrystal>> references) {
		if (references == null) return List.of();
		List<EnderCrystal> crystals = new java.util.ArrayList<>();
		for (net.minecraft.world.entity.EntityReference<net.minecraft.world.entity.boss.enderdragon.EndCrystal> reference : references) {
			net.minecraft.world.entity.Entity entity =
					this.accessor().cardforge$getLevel().getEntity(reference.getUUID());
			if (entity instanceof net.minecraft.world.entity.boss.enderdragon.EndCrystal) {
				crystals.add((EnderCrystal) ((org.cardboardpowered.bridge.world.entity.EntityBridge) (Object) entity).getBukkitEntity());
			}
		}
		return List.copyOf(crystals);
	}

	// 1.20.4 API
	
	@Override
	public void setPreviouslyKilled(boolean previouslyKilled) {
		// Whether the dragon has been beaten once, which is what decides if the
		// end gateways and the egg appear on the next kill.
		((org.minenite.cardforge.mixin.invoker.EnderDragonFightMutator) this.handle)
				.cardforge$setHasPreviouslyKilledDragon(previouslyKilled);
	}
}
