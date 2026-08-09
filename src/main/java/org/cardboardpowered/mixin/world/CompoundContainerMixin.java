package org.cardboardpowered.mixin.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import org.cardboardpowered.bridge.world.ContainerBridge;

@Mixin(CompoundContainer.class)
public abstract class CompoundContainerMixin implements Container, ContainerBridge {

    @Shadow public Container container1;
    @Shadow public Container container2;

    public List<HumanEntity> transaction = new java.util.ArrayList<HumanEntity>();

    @Override
    public List<ItemStack> getContents() {
        List<ItemStack> result = new ArrayList<ItemStack>(this.container1.getContainerSize() + this.container2.getContainerSize());
        for (int i = 0; i < (this.container1.getContainerSize() + this.container2.getContainerSize()); i++)
            result.add(this.getItem(i));
        return result;
    }

    @Shadow
    public ItemStack getItem(int i) {
        return null;
    }

    @Override
    public void onOpen(CraftHumanEntity who) {
        this.container1.startOpen(who.getHandle());
        this.container2.startOpen(who.getHandle());
        transaction.add(who);
    }

    @Override
    public void onClose(CraftHumanEntity who) {
        this.container1.stopOpen(who.getHandle());
        this.container2.stopOpen(who.getHandle());
        transaction.remove(who);
    }

    @Override
    public List<HumanEntity> getViewers() {
        return transaction;
    }

    @Override
    public InventoryHolder getOwner() {
        return null; // Bukkit DoubleChest does not refer to this method.
    }

    @Override
    public void cardboard$setMaxStackSize(int size) {
        ((ContainerBridge) (Object) this.container1).cardboard$setMaxStackSize(size);
        ((ContainerBridge) (Object) this.container2).cardboard$setMaxStackSize(size);
    }

    @Override
    public Location getLocation() {
        return ((ContainerBridge) (Object) this.container1).getLocation();
    }

    /**
     * Vanilla returns {@code container1.getMaxStackSize()}. This returned a
     * constant 64, and because the method exists on the target and this one
     * carries no {@code @Shadow}, Mixin merged it over the top - confirmed by
     * exporting the transformed class, where the body is {@code bipush 64;
     * ireturn}.
     *
     * <p>Two things were wrong with that. A modded container whose halves limit
     * stacks to less than 64 was reported as allowing 64, and the mixin
     * contradicted its own setter: {@code cardboard$setMaxStackSize} writes
     * through to both halves, so a plugin could set 16 and read back 64.
     *
     * <p>Invisible on a vanilla-only server, where every container answers 64.
     */
    @Override
    public int getMaxStackSize() {
        return this.container1.getMaxStackSize();
    }

}