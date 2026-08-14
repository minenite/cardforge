package org.minenite.cardforge.mixin.invoker;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.dimension.end.DragonRespawnStage;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Almost everything the DragonBattle API exposes is private on the fight: the
 * boss bar, the portal position, the gateway list, the respawn stage, and the
 * gateway and portal spawners.
 */
@Mixin(EnderDragonFight.class)
public interface EnderDragonFightAccessor {

    @Accessor("dragonEvent")
    ServerBossEvent cardforge$getDragonEvent();

    @Accessor("level")
    ServerLevel cardforge$getLevel();

    @Accessor("origin")
    BlockPos cardforge$getOrigin();

    @Accessor("exitPortalLocation")
    BlockPos cardforge$getExitPortalLocation();

    @Accessor("gateways")
    List<Integer> cardforge$getGateways();

    @Accessor("respawnStage")
    DragonRespawnStage cardforge$getRespawnStage();

    @Accessor("respawnCrystals")
    List<EntityReference<EndCrystal>> cardforge$getRespawnCrystals();

    @Accessor("respawnCrystals")
    void cardforge$setRespawnCrystals(List<EntityReference<EndCrystal>> crystals);

    @Invoker("setRespawnStage")
    void cardforge$setRespawnStage(DragonRespawnStage stage);

    @Invoker("spawnNewGateway")
    void cardforge$spawnNewGateway();

    @Invoker("spawnNewGateway")
    void cardforge$spawnNewGateway(BlockPos pos);

    @Invoker("spawnExitPortal")
    void cardforge$spawnExitPortal(boolean active);
}
