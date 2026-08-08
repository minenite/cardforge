package me.isaiah.common.fabric.entity;

import me.isaiah.common.entity.IPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class FabricPlayer extends FabricAliveEntity implements IPlayer {

    public FabricPlayer(Entity mc) {
        super((Player)mc);
    }

    @Override
    public String[] getClientMods() {
        return new String[] {"Minecraft"};
    }

    @Override
    public Player getMC() {
        return (Player) mc;
    }

    @Override
    public boolean isCreativeMode() {
        return getMC().isCreative();
    }

}
