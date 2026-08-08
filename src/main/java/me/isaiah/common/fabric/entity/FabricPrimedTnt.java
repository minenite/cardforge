package me.isaiah.common.fabric.entity;

import me.isaiah.common.cmixin.IMixinEntity;
import me.isaiah.common.entity.IEntity;
import me.isaiah.common.entity.IPrimedTnt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;

public class FabricPrimedTnt extends FabricEntity implements IPrimedTnt {

    public FabricPrimedTnt(Entity mc) {
        super((PrimedTnt)mc);
    }

    @Override
    public PrimedTnt getMC() {
        return (PrimedTnt) mc;
    }

    @Override
    public IEntity getSource() {
        // LivingEntity source = getMC().getCausingEntity();
        //return (source != null) ? ((IMixinEntity)source).getAsICommon() : null;
		// TODO: Update to also support 1.19.4
		return null;
    }

}
