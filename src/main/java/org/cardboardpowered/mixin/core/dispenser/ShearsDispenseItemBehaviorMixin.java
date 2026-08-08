package org.cardboardpowered.mixin.core.dispenser;

import com.google.common.collect.Lists;
import org.cardboardpowered.bridge.world.entity.EntityBridge;
import org.cardboardpowered.bridge.world.level.LevelBridge;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.dispenser.ShearsDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockShearEntityEvent;
import org.cardboardpowered.impl.block.DispenserBlockHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShearsDispenseItemBehavior.class)
public class ShearsDispenseItemBehaviorMixin {

    // todo: nonstatic
    private static Block cardboard_block;
    private static CraftItemStack cardboard_saved;

    @Inject(at = @At("HEAD"), method = "execute")
    protected void cardboard_dispenseSilently(BlockSource pointer, ItemStack stack, CallbackInfoReturnable<ItemStack> ci) {
        cardboard_block = ((LevelBridge)pointer.level()).cardboard$getWorld().getBlockAt(pointer.pos().getX(), pointer.pos().getY(), pointer.pos().getZ());
        cardboard_saved = CraftItemStack.asCraftMirror(stack);

        BlockDispenseEvent event = new BlockDispenseEvent(cardboard_block, cardboard_saved.clone(), new org.bukkit.util.Vector(0, 0, 0));
        if (!DispenserBlockHelper.eventFired) Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            ci.setReturnValue(stack);
            return;
        }

        if (!event.getItem().equals(cardboard_saved)) {
            ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
            DispenseItemBehavior idispensebehavior = (DispenseItemBehavior) DispenserBlock.DISPENSER_REGISTRY.get(eventStack.getItem());
            if (idispensebehavior != DispenseItemBehavior.NOOP && idispensebehavior != this) {
                idispensebehavior.dispense(pointer, eventStack);
                ci.setReturnValue(stack);
                return;
            }
        }
    }

    /*
     * NeoForge replaced the vanilla dispenser shear path outright:
     *
     *   -  if (entity.isAlive() && entity instanceof Shearable s && s.readyForShearing()) {
     *   -      s.shear(level, SoundSource.BLOCKS, tool);
     *   +  if (entity instanceof IShearable s && s.isShearable(null, tool, level, pos)) {
     *   +      s.onSheared(null, tool, level, pos).forEach(d -> s.spawnShearedDrop(level, pos, d));
     *
     * so the old Redirect on Shearable#shear has no call site to bind to. The hook
     * has to move onto the two IShearable calls that took its place, and it has to
     * be split across both of them, because the two halves of BlockShearEntityEvent
     * are now available at different moments:
     *
     *   - cancellation must be decided at isShearable, before anything mutates, since
     *     returning false there also suppresses the durability hit and the game event
     *     exactly as a cancelled shear should;
     *   - the drop list only exists after onSheared runs, because NeoForge derives it
     *     by capturing the entity's drops during the shear.
     *
     * So we fire the event at isShearable with a mutable, empty drop list (matching
     * what Cardboard passed on Fabric) and then, at onSheared, honour any drops a
     * plugin added to it. Redirecting IShearable rather than the concrete mobs also
     * means this now covers modded shearable entities.
     */

    private static BlockShearEntityEvent cardboard_shearEvent;

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/common/IShearable;isShearable(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z"),
            method = "tryShearEntity")
    private static boolean doEvent(net.neoforged.neoforge.common.IShearable shearable, net.minecraft.world.entity.player.Player player,
                                   ItemStack tool, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        cardboard_shearEvent = null;
        if (!shearable.isShearable(player, tool, level, pos)) {
            return false;
        }
        if (!(shearable instanceof LivingEntity living)) {
            return true;
        }

        BlockShearEntityEvent event = callBlockShearEntityEvent(living, cardboard_block, cardboard_saved,
                new java.util.ArrayList<>());
        if (event.isCancelled()) {
            return false;
        }
        cardboard_shearEvent = event;
        return true;
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/common/IShearable;onSheared(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Ljava/util/List;"),
            method = "tryShearEntity")
    private static List<ItemStack> cardboard_applyEventDrops(net.neoforged.neoforge.common.IShearable shearable,
                                                             net.minecraft.world.entity.player.Player player, ItemStack tool,
                                                             net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        List<ItemStack> drops = shearable.onSheared(player, tool, level, pos);

        BlockShearEntityEvent event = cardboard_shearEvent;
        cardboard_shearEvent = null;
        if (event == null || event.getDrops().isEmpty()) {
            return drops;
        }

        // A plugin added drops to the (initially empty) event list, so it is
        // asking for those instead of the natural ones.
        List<ItemStack> replaced = Lists.newArrayList();
        for (org.bukkit.inventory.ItemStack bukkit : event.getDrops()) {
            replaced.add(CraftItemStack.asNMSCopy(bukkit));
        }
        return replaced;
    }

    private static BlockShearEntityEvent callBlockShearEntityEvent(Entity animal, org.bukkit.block.Block dispenser, CraftItemStack is, List<net.minecraft.world.item.ItemStack> drops) {

        // Must be a real mutable list, not a Lists.transform view: the event contract
        // lets a plugin add to getDrops(), and a transformed view rejects add().
        List<org.bukkit.inventory.ItemStack> bukkitDrops = Lists.newArrayList();
        for (ItemStack drop : drops) {
            bukkitDrops.add(CraftItemStack.asCraftMirror(drop));
        }

    	BlockShearEntityEvent bse = new BlockShearEntityEvent(
    			dispenser,
    			((EntityBridge) (Object) animal).getBukkitEntity(),
    			is,
    			bukkitDrops
    	);
        Bukkit.getPluginManager().callEvent(bse);
        return bse;
    }

}
