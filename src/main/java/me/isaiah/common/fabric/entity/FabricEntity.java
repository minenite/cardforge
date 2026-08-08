package me.isaiah.common.fabric.entity;

import java.util.UUID;

import me.isaiah.common.cmixin.IMixinEntity;
import me.isaiah.common.cmixin.IMixinTameableEntity;
import me.isaiah.common.entity.EntityType;
import me.isaiah.common.entity.IEntity;
import me.isaiah.common.entity.IRemoveReason;
import me.isaiah.common.fabric.FabricServer;
import me.isaiah.common.world.IWorld;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class FabricEntity implements IEntity {

    public Entity mc;
    public FabricEntity(Entity mc) {
        this.mc = mc;
    }
    
    private IMixinEntity imixin() {
    	return (IMixinEntity) mc;
    }

    @Override
    public String getName() {
    	// TODO: 1.20.5
        return getMC().getName().getString();
    }

    @Override
    public Entity getMC() {
        return mc;
    }
    
    @Override
    public IWorld getIWorld() {
    	Level world = this.imixin().ic$getWorld();
    	return FabricServer.getInstance().getIWorldForMinecraftWorld(world);
    }
 

    @Override
    public String getDisplayedName() {
        return getMC().getCustomName().getString();
    }

    @Override
    public void setDisplayedName(String str) {
        getMC().setCustomNameVisible(true);
        getMC().setCustomName(Component.nullToEmpty(str));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.UNKNOWN;
    }

    @Override
    public void remove(IRemoveReason r) {
        ((IMixinEntity) (Object) mc).Iremove(r);
    }

    @Override
    public void message(String msg) {
        ((IMixinEntity) (Object) mc).IsendText(Component.nullToEmpty(msg), UUID.randomUUID());
    }

    @Override
    public boolean isRemoved() {
        return ((IMixinEntity) (Object) mc).ic_isRemoved();
    }

    @Override
    public void collidesWith(IEntity e) {
        getMC().canCollideWith((Entity)e.getMC());
    }

    @Override
    public void teleport(double x, double y, double z) {
    	// TODO: ServerWorld argument is currently null;
    	// used to future proof
    	((IMixinEntity) (Object) mc).IC$teleport(null, x, y, z);
    }

    @Override
    public void teleport(double x, double y, double z, float yaw, float pitch) {
        this.teleport(x, y, z);
        getMC().setYBodyRot(yaw);
        // TODO 1.17 getMC().pitch = pitch;
    }
    
    @Override
    public void set_tamed(boolean tame, boolean updateAttrib) {
    	if (mc instanceof IMixinTameableEntity) {
    		((IMixinTameableEntity) (Object) mc).IC$set_tamed(tame, updateAttrib);
    	} else {
    		// Not TameableEntity
    	}
    }

}