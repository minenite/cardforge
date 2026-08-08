package me.isaiah.common.event.block;

import me.isaiah.common.event.Event;
import net.minecraft.nbt.CompoundTag;

public class BlockEntityWriteNbtEvent extends Event {
    
    private CompoundTag element;
    private Object mc;

    public BlockEntityWriteNbtEvent(CompoundTag element, Object mc) {
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