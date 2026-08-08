package me.isaiah.common.fabric.entity;

import me.isaiah.common.entity.IArmorStand;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;

public class FabricArmorStandEntity extends FabricAliveEntity implements IArmorStand {

    public FabricArmorStandEntity(Entity mc) {
        super((ArmorStand)mc);
    }

    @Override
    public ArmorStand getMC() {
        return (ArmorStand) mc;
    }

    @Override
    public boolean isMarker() {
        return getMC().isMarker();
    }

    @Override
    public void setMarker(boolean marker) {
        SynchedEntityData dataTracker = getMC().getEntityData();
        dataTracker.set(ArmorStand.DATA_CLIENT_FLAGS, this.setBitField1(dataTracker.get(ArmorStand.DATA_CLIENT_FLAGS), 16, marker));
    }

    private byte setBitField1(byte value, int bitField, boolean set) {
        value = set ? (byte)(value | bitField) : (byte)(value & ~bitField);
        return value;
    }

}