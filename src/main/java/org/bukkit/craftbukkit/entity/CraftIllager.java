package org.bukkit.craftbukkit.entity;

import net.minecraft.world.entity.monster.illager.AbstractIllager;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Illager;

public class CraftIllager extends CraftRaider implements Illager {

    public CraftIllager(CraftServer server, AbstractIllager entity) {
        super(server, entity);
    }

    @Override
    public AbstractIllager getHandle() {
        return (AbstractIllager) super.getHandle();
    }

    @Override
    public String toString() {
        return "Illager";
    }

	@Override
	public boolean isCelebrating() {
		// Raiders track this themselves; the API just never asked.
		return this.getHandle().isCelebrating();
	}

	@Override
	public void setCelebrating(boolean celebrating) {
		this.getHandle().setCelebrating(celebrating);
	}

}