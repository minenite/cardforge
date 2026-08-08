/**
 * Cardboard - CardboardPowered.org
 * Copyright (C) 2020 CardboardPowered.org and contributors
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.cardboardpowered.bridge.world.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.inventory.InventoryView;

public interface AbstractContainerMenuBridge {

    /**
     * The player this menu was opened for.
     *
     * <p>AbstractContainerMenu has no player of its own, so the fallback
     * getBukkitView() had to build its view with a null one - and anything that
     * then asked the view for the bottom (player) inventory threw. Recording the
     * viewer when the menu is opened gives that fallback a real player, which is
     * what every menu a mod defines relies on.
     */
    void cardboard$setViewer(org.bukkit.entity.HumanEntity viewer);

    org.bukkit.entity.HumanEntity cardboard$getViewer();


    default InventoryView getBukkitView() {
        return null;
    }

    default Component getTitle() {
        return null;
    }

    default void setTitle(Component title) {
    }

    default void transferTo(AbstractContainerMenu other, CraftHumanEntity player) {
    }

    /*
    public default void transferTo(ScreenHandler other, CraftHumanEntity player) {
    	CardboardInventoryView source = this.getBukkitView();
    	CardboardInventoryView destination = other.getBukkitView();

        ( (IMixinInventory) ((CraftInventory)source.getTopInventory()).getInventory() ).onClose(player);
        ( (IMixinInventory) ((CraftInventory)source.getBottomInventory()).getInventory() ).onClose(player);
        ( (IMixinInventory) ((CraftInventory)destination.getTopInventory()).getInventory() ).onOpen(player);
        ( (IMixinInventory) ((CraftInventory)destination.getBottomInventory()).getInventory() ).onOpen(player);
    }
    */


    default NonNullList<ItemStack> getTrackedStacksBF() {
        return null;
    }

    default void setTrackedStacksBF(NonNullList<ItemStack> trackedStacks) {
    }

    default void cardboard$setCheckReachable(boolean bl) {
    }

    default void cardboard_setSlots(NonNullList<Slot> slots) {
    }

    default NonNullList<ItemStack> cardboard_previousTrackedStacks() {
        return null;
    }

    default void cardboard_previousTrackedStacks(NonNullList<ItemStack> s) {
    }

    default void cardboard$broadcastCarriedItem() {
    }
}