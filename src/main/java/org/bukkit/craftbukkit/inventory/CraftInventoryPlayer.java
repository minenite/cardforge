package org.bukkit.craftbukkit.inventory;

import com.google.common.base.Preconditions;
import java.util.List;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import org.bukkit.craftbukkit.CraftEquipmentSlot;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.cardboardpowered.bridge.world.ContainerBridge;
import org.cardboardpowered.bridge.world.entity.player.InventoryBridge;

public class CraftInventoryPlayer extends CraftInventory implements org.bukkit.inventory.PlayerInventory, EntityEquipment {
    public CraftInventoryPlayer(net.minecraft.world.entity.player.Inventory inventory) {
        super(inventory);
    }

    @Override
    public Inventory getInventory() {
        return (Inventory) this.inventory;
    }

    @Override
    public ItemStack[] getStorageContents() {
        return this.asCraftMirror(this.getInventory().getNonEquipmentItems());
    }

    @Override
    public ItemStack getItemInMainHand() {
        return CraftItemStack.asCraftMirror(this.getInventory().getSelectedItem());
    }

    @Override
    public void setItemInMainHand(ItemStack item) {
        this.setItem(this.getHeldItemSlot(), item);
    }

    @Override
    public void setItemInMainHand(ItemStack item, boolean silent) {
        this.setItemInMainHand(item); // Silence doesn't apply to players
    }

    @Override
    public ItemStack getItemInOffHand() {
        return CraftItemStack.asCraftMirror(this.getInventory().equipment.get(net.minecraft.world.entity.EquipmentSlot.OFFHAND));
    }

    @Override
    public void setItemInOffHand(ItemStack item) {
        this.getInventory().equipment.set(net.minecraft.world.entity.EquipmentSlot.OFFHAND, CraftItemStack.asNMSCopy(item));
    }

    @Override
    public void setItemInOffHand(ItemStack item, boolean silent) {
        this.setItemInOffHand(item); // Silence doesn't apply to players
    }

    @Override
    public ItemStack getItemInHand() {
        return this.getItemInMainHand();
    }

    @Override
    public void setItemInHand(ItemStack stack) {
        this.setItemInMainHand(stack);
    }

    @Override
    public void setItem(int index, ItemStack item) {
        // Paper start - Validate setItem index
        if (index < 0 || index > 42) {
            throw new ArrayIndexOutOfBoundsException("Index must be between 0 and 42");
        }
        // Paper end - Validate setItem index
        super.setItem(index, item);
        if (this.getHolder() == null) return;

        ServerPlayer player = ((CraftPlayer) this.getHolder()).getHandle();
        if (player.connection == null) return;

        // Minecraft 26.2 tracks the player's own inventory with
        // ClientboundSetPlayerInventoryPacket. The old inventoryMenu SetSlot
        // remapping leaves ghost items after /clear and desyncs container GUIs.
        player.connection.send(this.getInventory().createInventoryUpdatePacket(index));

        // Keep inventoryMenu remote-slot cache aligned. Skipping this left remotes
        // stale after Bukkit setItem; the next /give or pickup broadcastChanges
        // then pushed wrong ContainerSetSlot data and looked like hotbar overrides.
        if (player.inventoryMenu != null) {
            player.inventoryMenu.broadcastChanges();
        }
        if (player.containerMenu != null && player.containerMenu != player.inventoryMenu) {
            player.containerMenu.broadcastChanges();
        }
    }

    @Override
    public void clear() {
        super.clear();
        resyncEntireInventory();
    }

    @Override
    public void clear(int index) {
        this.setItem(index, null);
    }

    /** Push every player-inventory slot to the client (fixes /clear ghosts). */
    public void resyncEntireInventory() {
        if (this.getHolder() == null) {
            return;
        }
        ServerPlayer player = ((CraftPlayer) this.getHolder()).getHandle();
        if (player.connection == null) {
            return;
        }
        Inventory inv = this.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            player.connection.send(inv.createInventoryUpdatePacket(i));
        }
        player.inventoryMenu.sendAllDataToRemote();
        if (player.containerMenu != null) {
            player.containerMenu.sendAllDataToRemote();
        }
    }

    @Override
    public void setItem(EquipmentSlot slot, ItemStack item) {
        Preconditions.checkArgument(slot != null, "slot must not be null");

        switch (slot) {
            case HAND -> this.setItemInMainHand(item);
            case OFF_HAND, FEET, LEGS, CHEST, HEAD, BODY, SADDLE ->
                    this.getInventory().equipment.set(CraftEquipmentSlot.getNMS(slot), CraftItemStack.asNMSCopy(item));
        }
    }

    @Override
    public void setItem(EquipmentSlot slot, ItemStack item, boolean silent) {
        this.setItem(slot, item); // Silence doesn't apply to players
    }

    @Override
    public ItemStack getItem(EquipmentSlot slot) {
        Preconditions.checkArgument(slot != null, "slot must not be null");

        return switch (slot) {
            case HAND -> this.getItemInMainHand();
            case OFF_HAND, FEET, LEGS, CHEST, HEAD, BODY, SADDLE -> CraftItemStack.asCraftMirror(this.getInventory().equipment.get(CraftEquipmentSlot.getNMS(slot)));
        };
    }

    @Override
    public int getHeldItemSlot() {
        return this.getInventory().getSelectedSlot();
    }

    @Override
    public void setHeldItemSlot(int slot) {
        Preconditions.checkArgument(slot >= 0 && slot < Inventory.getSelectionSize(), "Slot (%s) is not between 0 and %s inclusive", slot, Inventory.getSelectionSize() - 1);
        this.getInventory().setSelectedSlot(slot);
        ((CraftPlayer) this.getHolder()).getHandle().connection.send(new ClientboundSetHeldSlotPacket(slot));
    }

    @Override
    public ItemStack getHelmet() {
        return this.getItem(EquipmentSlot.HEAD);
    }

    @Override
    public ItemStack getChestplate() {
        return this.getItem(EquipmentSlot.CHEST);
    }

    @Override
    public ItemStack getLeggings() {
        return this.getItem(EquipmentSlot.LEGS);
    }

    @Override
    public ItemStack getBoots() {
        return this.getItem(EquipmentSlot.FEET);
    }

    @Override
    public void setHelmet(ItemStack helmet) {
        this.setItem(EquipmentSlot.HEAD, helmet);
    }

    @Override
    public void setHelmet(ItemStack helmet, boolean silent) {
        this.setHelmet(helmet); // Silence doesn't apply to players
    }

    @Override
    public void setChestplate(ItemStack chestplate) {
        this.setItem(EquipmentSlot.CHEST, chestplate);
    }

    @Override
    public void setChestplate(ItemStack chestplate, boolean silent) {
        this.setChestplate(chestplate); // Silence doesn't apply to players
    }

    @Override
    public void setLeggings(ItemStack leggings) {
        this.setItem(EquipmentSlot.LEGS, leggings);
    }

    @Override
    public void setLeggings(ItemStack leggings, boolean silent) {
        this.setLeggings(leggings); // Silence doesn't apply to players
    }

    @Override
    public void setBoots(ItemStack boots) {
        this.setItem(EquipmentSlot.FEET, boots);
    }

    @Override
    public void setBoots(ItemStack boots, boolean silent) {
        this.setBoots(boots); // Silence doesn't apply to players
    }

    @Override
    public ItemStack[] getArmorContents() {
        return this.asCraftMirror(((InventoryBridge)this.getInventory()).getArmorContents());
    }

    private void setSlots(ItemStack[] items, int baseSlot, int length) {
        if (items == null) {
            items = new ItemStack[length];
        }
        Preconditions.checkArgument(items.length <= length, "items.length must be <= %s", length);

        for (int i = 0; i < length; i++) {
            if (i >= items.length) {
                this.setItem(baseSlot + i, null);
            } else {
                this.setItem(baseSlot + i, items[i]);
            }
        }
    }

    @Override
    public void setStorageContents(ItemStack[] items) throws IllegalArgumentException {
        this.setSlots(items, 0, this.getInventory().getNonEquipmentItems().size());
    }

    @Override
    public void setArmorContents(ItemStack[] items) {
        this.setSlots(items, this.getInventory().getNonEquipmentItems().size(), ((InventoryBridge)this.getInventory()).getArmorContents().size());
    }

    @Override
    public ItemStack[] getExtraContents() {
        return this.asCraftMirror(((InventoryBridge)this.getInventory()).getExtraContent());
    }

    @Override
    public void setExtraContents(ItemStack[] items) {
        this.setSlots(items, this.getInventory().getNonEquipmentItems().size() + ((InventoryBridge)this.getInventory()).getArmorContents().size(), 3);
    }

    @Override
    public HumanEntity getHolder() {
        return (HumanEntity) ((ContainerBridge) (Object) this.inventory).getOwner();
    }

    @Override
    public float getItemInHandDropChance() {
        return this.getItemInMainHandDropChance();
    }

    @Override
    public void setItemInHandDropChance(float chance) {
        this.setItemInMainHandDropChance(chance);
    }

    @Override
    public float getItemInMainHandDropChance() {
        return 1;
    }

    @Override
    public void setItemInMainHandDropChance(float chance) {
        throw new UnsupportedOperationException("Cannot set drop chance for PlayerInventory");
    }

    @Override
    public float getItemInOffHandDropChance() {
        return 1;
    }

    @Override
    public void setItemInOffHandDropChance(float chance) {
        throw new UnsupportedOperationException("Cannot set drop chance for PlayerInventory");
    }

    @Override
    public float getHelmetDropChance() {
        return 1;
    }

    @Override
    public void setHelmetDropChance(float chance) {
        throw new UnsupportedOperationException("Cannot set drop chance for PlayerInventory");
    }

    @Override
    public float getChestplateDropChance() {
        return 1;
    }

    @Override
    public void setChestplateDropChance(float chance) {
        throw new UnsupportedOperationException("Cannot set drop chance for PlayerInventory");
    }

    @Override
    public float getLeggingsDropChance() {
        return 1;
    }

    @Override
    public void setLeggingsDropChance(float chance) {
        throw new UnsupportedOperationException("Cannot set drop chance for PlayerInventory");
    }

    @Override
    public float getBootsDropChance() {
        return 1;
    }

    @Override
    public void setBootsDropChance(float chance) {
        throw new UnsupportedOperationException("Cannot set drop chance for PlayerInventory");
    }
    // Paper start
    @Override
    public float getDropChance(EquipmentSlot slot) {
        return 1;
    }

    @Override
    public void setDropChance(EquipmentSlot slot, float chance) {
        throw new UnsupportedOperationException("Cannot set drop chance for PlayerInventory");
    }
    // Paper end
}
