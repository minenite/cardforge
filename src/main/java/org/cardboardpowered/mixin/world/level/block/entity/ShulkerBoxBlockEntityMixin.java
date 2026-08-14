package org.cardboardpowered.mixin.world.level.block.entity;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import org.cardboardpowered.bridge.world.ContainerBridge;

@Mixin(ShulkerBoxBlockEntity.class)
public abstract class ShulkerBoxBlockEntityMixin implements Container, ContainerBridge {

    @Shadow
    public NonNullList<ItemStack> itemStacks;

    public List<HumanEntity> transaction = new java.util.ArrayList<HumanEntity>();
    private int maxStack = MAX_STACK;

    @Override
    public List<ItemStack> getContents() {
        return itemStacks;
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
        this.maxStack = size;
    }

    @Override
    public int getMaxStackSize() {
        return maxStack;
    }

    @Override
    public Location getLocation() {
        net.minecraft.world.level.block.entity.BlockEntity blockEntity =
                (net.minecraft.world.level.block.entity.BlockEntity) (Object) this;
        if (blockEntity.getLevel() == null) return null;
        net.minecraft.core.BlockPos pos = blockEntity.getBlockPos();
        return new Location(((org.cardboardpowered.bridge.world.level.LevelBridge) blockEntity.getLevel())
                .cardboard$getWorld(), pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public InventoryHolder getOwner() {
        // Both of these returned null. For the brewing stand that also meant
        // BrewEvent never fired, because the event handler bails when the owner
        // is null.
        Location location = this.getLocation();
        if (location == null) return null;
        org.bukkit.block.BlockState state = location.getBlock().getState();
        return state instanceof InventoryHolder holder ? holder : null;
    }

}
