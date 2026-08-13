package org.cardboardpowered.mixin.server.commands;

import java.util.function.Predicate;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.commands.ClearInventoryCommands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Minecraft 26.2 player HUD uses {@code ClientboundSetPlayerInventoryPacket}.
 * Vanilla {@code /clear} only {@code broadcastChanges}/{@code slotsChanged}, which
 * leaves ghost items on NeoForge clients — and a follow-up {@code /clear} reports
 * "no items were found" while the client still shows the map/hotbar.
 *
 * <p>Wraps the clear call: wipe leftovers on unlimited clears, always resync the
 * client. Map {@code sendMap} / render paths are untouched.
 */
@Mixin(ClearInventoryCommands.class)
public class ClearInventoryCommandsMixin {

    @WrapOperation(
            method = "clearInventory",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Inventory;clearOrCountMatchingItems(Ljava/util/function/Predicate;ILnet/minecraft/world/Container;)I"))
    private static int cardforge$clearAndResync(
            Inventory inventory,
            Predicate<ItemStack> predicate,
            int maxCount,
            Container craftSlots,
            Operation<Integer> original) {
        int removed = original.call(inventory, predicate, maxCount, craftSlots);

        // Count-only test (/clear … 0): leave state alone.
        if (maxCount == 0) {
            return removed;
        }

        // Unlimited /clear: if anything is still present, wipe and count it so the
        // command reports success instead of "no items were found".
        if (maxCount < 0) {
            removed += drainContainer(craftSlots);
            ItemStack carried = inventory.player.containerMenu.getCarried();
            if (!carried.isEmpty()) {
                removed += carried.getCount();
                inventory.player.containerMenu.setCarried(ItemStack.EMPTY);
            }
            if (!inventory.isEmpty()) {
                removed += Math.max(1, countNonEmpty(inventory));
                inventory.clearContent();
            }
        }

        if (inventory.player instanceof ServerPlayer serverPlayer
                && serverPlayer.connection != null) {
            resyncPlayerInventory(serverPlayer);
        }

        // Ghost-only client (server already empty): resync cleared the HUD, but
        // vanilla would throw "no items were found". Treat as a successful clear.
        if (maxCount < 0 && removed == 0) {
            return 1;
        }

        return removed;
    }

    private static int drainContainer(Container container) {
        int removed = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                removed += stack.getCount();
                container.setItem(i, ItemStack.EMPTY);
            }
        }
        return removed;
    }

    private static int countNonEmpty(Inventory inventory) {
        int n = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                n += stack.getCount();
            }
        }
        return n;
    }

    private static void resyncPlayerInventory(ServerPlayer player) {
        Inventory inv = player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            player.connection.send(inv.createInventoryUpdatePacket(slot));
        }
        player.inventoryMenu.sendAllDataToRemote();
        if (player.containerMenu != null) {
            player.containerMenu.sendAllDataToRemote();
        }
    }
}
