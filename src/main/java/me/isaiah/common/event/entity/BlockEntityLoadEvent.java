package me.isaiah.common.event.entity;

import me.isaiah.common.event.Event;
import net.minecraft.nbt.CompoundTag;

public class BlockEntityLoadEvent extends Event {

    private CompoundTag element;
    private Object mc;

    public BlockEntityLoadEvent(CompoundTag element, Object mc) {
        this.element = element;
        this.mc = mc;
    }

    public CompoundTag getElement() {
        return element;
    }

    public Object getMC() {
        return mc;
    }

}