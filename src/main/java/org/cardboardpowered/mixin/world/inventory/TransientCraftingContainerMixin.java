package org.cardboardpowered.mixin.world.inventory;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import org.cardboardpowered.bridge.world.ContainerBridge;

@Mixin(TransientCraftingContainer.class)
public abstract class TransientCraftingContainerMixin implements Container, ContainerBridge {

    @Shadow public AbstractContainerMenu menu;
    @Shadow public NonNullList<ItemStack> items;

    public List<HumanEntity> transaction = new java.util.ArrayList<HumanEntity>();
    public int maxStack = MAX_STACK;

    @Override
    public List<ItemStack> getContents() {
        return items;
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
    public InventoryHolder getOwner() {
        // A crafting grid belongs to whoever has the menu open; there is no block
        // behind it to own it.
        return this.transaction.isEmpty() ? null : this.transaction.get(0);
    }

    @Override
    public void cardboard$setMaxStackSize(int size) {
        maxStack = size;
    }

    @Override
    public Location getLocation() {
        // A crafting grid is not placed anywhere: it lives inside a menu. Null is
        // the honest answer, and the API documents it.
        return null;
    }

    @Override
    public int getMaxStackSize() {
        return maxStack;
    }

}