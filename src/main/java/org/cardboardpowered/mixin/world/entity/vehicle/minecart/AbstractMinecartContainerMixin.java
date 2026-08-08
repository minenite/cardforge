package org.cardboardpowered.mixin.world.entity.vehicle.minecart;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Mixin;

import org.cardboardpowered.bridge.world.ContainerBridge;
import org.cardboardpowered.bridge.world.entity.EntityBridge;
import org.cardboardpowered.bridge.world.level.LevelBridge;

@Mixin(AbstractMinecartContainer.class)
public abstract class AbstractMinecartContainerMixin implements Container, ContainerBridge {

    public List<HumanEntity> transaction = new ArrayList<>();
    private int maxStack = MAX_STACK;

    @Override
    public List<ItemStack> getContents() {
        return ((AbstractMinecartContainer)(Object)this).getItemStacks();
    }

    @Override
    public InventoryHolder getOwner() {
        org.bukkit.entity.Entity entity = ((EntityBridge) (Object) this).getBukkitEntity();
        return (entity instanceof InventoryHolder) ? (InventoryHolder) entity : null;
    }

    @Override
    public void onOpen(CraftHumanEntity who) {
        transaction.add(who);
    }

    @Override
    public void onClose(CraftHumanEntity who) {
        transaction.remove(who);
    }

    @Override
    public List<HumanEntity> getViewers() {
        return transaction;
    }

    @Override
    public void cardboard$setMaxStackSize(int size) {
        maxStack = size;
    }

    @Override
    public int getMaxStackSize() {
        return maxStack;
    }

    @Override
    public Location getLocation() {
        Entity entity = (Entity)(Object)this;
        return new Location(((LevelBridge)entity.level()).cardboard$getWorld(), entity.getX(), entity.getY(), entity.getZ());
    }

}
