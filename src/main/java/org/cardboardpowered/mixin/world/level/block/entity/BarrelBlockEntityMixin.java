package org.cardboardpowered.mixin.world.level.block.entity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.minenite.cardforge.mixin.invoker.BarrelBlockEntityAccessor;
import org.spongepowered.asm.mixin.Mixin;

import org.cardboardpowered.bridge.world.ContainerBridge;
import org.cardboardpowered.bridge.world.level.LevelBridge;

@Mixin(BarrelBlockEntity.class)
public abstract class BarrelBlockEntityMixin implements Container, ContainerBridge {

    public List<HumanEntity> transaction = new ArrayList<>();
    private int maxStack = MAX_STACK;

    @Override
    public List<ItemStack> getContents() {
        // Returned null, so anything reading a barrel through this bridge - the
        // inventory snapshot among them - failed on the first element.
        return ((BarrelBlockEntityAccessor) this).cardforge$getItems();
    }

    @Override
    public InventoryHolder getOwner() {
        // Returned null for every container, so InventoryHolder-based lookups -
        // "which chest is this inventory in" - answered nothing.
        Location location = this.getLocation();
        if (location == null) return null;
        org.bukkit.block.BlockState state = location.getBlock().getState();
        return state instanceof InventoryHolder holder ? holder : null;
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
    public Location getLocation() {
        BlockPos pos = ((BlockEntity)(Object)this).getBlockPos();
        return new Location(((LevelBridge)((BlockEntity)(Object)this).getLevel()).cardboard$getWorld(), pos.x, pos.y, pos.z);
    }

    @Override
    public int getMaxStackSize() {
        return maxStack;
    }

}
