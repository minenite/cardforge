package org.cardboardpowered.mixin.world.level.block.entity;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import org.cardboardpowered.bridge.world.ContainerBridge;
import org.cardboardpowered.bridge.world.level.LevelBridge;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin implements Container, ContainerBridge {

    // TODO Add FurnaceBurnEvent, FurnanceSmeltEvent, FurnaceExtractEvent
    // TODO

    @Shadow
    public NonNullList<ItemStack> items;

    private int maxStack = MAX_STACK;
    public List<HumanEntity> transaction = new java.util.ArrayList<HumanEntity>();

    @Override
    public List<ItemStack> getContents() {
        return this.items;
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
        BlockEntity blockEntity = (BlockEntity) (Object) this;
        if (blockEntity.getLevel() == null) return null;
        BlockPos pos = blockEntity.getBlockPos();
        return new Location(((LevelBridge) blockEntity.getLevel()).cardboard$getWorld(), pos.getX(), pos.getY(), pos.getZ());
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

}