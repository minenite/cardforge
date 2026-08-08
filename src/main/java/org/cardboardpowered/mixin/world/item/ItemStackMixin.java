package org.cardboardpowered.mixin.world.item;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.cardboardpowered.bridge.server.level.ServerPlayerBridge;
import org.cardboardpowered.bridge.world.entity.EntityBridge;
import org.cardboardpowered.bridge.world.level.LevelBridge;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.cardboardpowered.bridge.world.item.ItemStackBridge;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemStack.class, priority = 999)
public abstract class ItemStackMixin implements ItemStackBridge {
    
	
	@Shadow
	public Holder<Item> item;
    
    @Shadow
    public int count;
    
    @Mutable
    @Final
    @Shadow
    PatchedDataComponentMap components;

    @Shadow
    public abstract ItemStack copy();

    @Shadow
    public abstract int getDamageValue();

    @Shadow
    public abstract boolean isDamageableItem();

    @Shadow
    public abstract void setDamageValue(int i);

    @Shadow
    public abstract boolean isBroken();

    @Shadow
    public abstract Item getItem();

    @Shadow
    public abstract void shrink(int i);

    @Shadow
    public abstract int getMaxDamage();

    @Shadow
    protected abstract int processDurabilityChange(int i, ServerLevel serverLevel, @Nullable ServerPlayer serverPlayer);

    @Override
    public void cardboard$restore_patch(DataComponentPatch changes) {
        this.components.restorePatch(changes);
    }

    // Paper start - (this is just a good no conflict location)
    @Override
    public org.bukkit.inventory.ItemStack cardboard$asBukkitMirror() {
        return org.bukkit.craftbukkit.inventory.CraftItemStack.asCraftMirror(((ItemStack)(Object)this));
    }

    @Override
    public org.bukkit.inventory.ItemStack cardboard$asBukkitCopy() {
        return org.bukkit.craftbukkit.inventory.CraftItemStack.asCraftMirror(this.copy());
    }

    @Unique
    private org.bukkit.craftbukkit.inventory.@Nullable CraftItemStack bukkitStack;

    @Override
    public org.bukkit.inventory.ItemStack cardboard$getBukkitStack() {
        if (this.bukkitStack == null || this.bukkitStack.handle != ((ItemStack)(Object)this)) {
            this.bukkitStack = org.bukkit.craftbukkit.inventory.CraftItemStack.asCraftMirror(((ItemStack)(Object)this));
        }
        return this.bukkitStack;
    }
    // Paper end

    // CraftBukkit start
    @Deprecated
    @Override
    public void cardboard$setItem(Item item) {
        this.bukkitStack = null; // Paper
        this.item = item.builtInRegistryHolder();
        // Paper start - change base component prototype
        DataComponentPatch patch = ((ItemStack)(Object)this).getComponentsPatch();
        this.components = new PatchedDataComponentMap(this.item.components());
        ((ItemStack)(Object)this).applyComponents(patch);
        // Paper end - change base component prototype
    }
    // CraftBukkit end

    // BlockPlaceEvent used to live here as an @Overwrite of useOn. NeoForge
    // rewrote that method - on a dedicated server it fires UseItemOnBlockEvent and
    // then delegates to CommonHooks.onPlaceItemIntoWorld, never running the vanilla
    // body - so overwriting it silently removed both hooks for every NeoForge mod.
    // The Bukkit event now fires inside that replacement instead; see
    // org.cardboardpowered.mixin.neoforge.CommonHooksMixin.

    @Inject(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V", at = @At("HEAD"), cancellable = true)
    public void hurtAndBreakPaper(int damage, ServerLevel level, @Nullable ServerPlayer player, Consumer<Item> onBreak, CallbackInfo ci) { // Paper - Add EntityDamageItemEvent
        // Paper start - add force boolean overload
        this.hurtAndBreak(damage, level, player, onBreak, false);
        ci.cancel();
    }

    @Unique
    public void hurtAndBreak(int damage, ServerLevel level, @Nullable LivingEntity player, Consumer<Item> onBreak, boolean force) { // Paper - Add EntityDamageItemEvent
        // Paper end
        final int originalDamage = damage; // Paper - Expand PlayerItemDamageEvent
        int i = this.processDurabilityChange(damage, level, player, force); // Paper
        // CraftBukkit start
        if (i > 0 && player instanceof final ServerPlayer serverPlayer) { // Paper - Add EntityDamageItemEvent - limit to positive damage and run for player
            org.bukkit.event.player.PlayerItemDamageEvent event = new org.bukkit.event.player.PlayerItemDamageEvent((Player) ((EntityBridge) (Object) serverPlayer).getBukkitEntity(), org.bukkit.craftbukkit.inventory.CraftItemStack.asCraftMirror((ItemStack)(Object)this), i, originalDamage); // Paper - Add EntityDamageItemEvent
            event.getPlayer().getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }

            i = event.getDamage();
            // Paper start - Add EntityDamageItemEvent
        } else if (i > 0 && player != null) {
            io.papermc.paper.event.entity.EntityDamageItemEvent event = new io.papermc.paper.event.entity.EntityDamageItemEvent(((EntityBridge) (Object) player).getBukkitEntity(), org.bukkit.craftbukkit.inventory.CraftItemStack.asCraftMirror((ItemStack)(Object)this), i);
            if (!event.callEvent()) {
                return;
            }
            i = event.getDamage();
            // Paper end - Add EntityDamageItemEvent
        }
        // CraftBukkit end
        if (i != 0) { // Paper - Add EntityDamageItemEvent - diff on change for above event ifs.
            this.applyDamage(this.getDamageValue() + i, player, onBreak);
        }
    }

    @Inject(method = "processDurabilityChange", at = @At("HEAD"), cancellable = true)
    private void processDurabilityChangePaper(int damage, ServerLevel level, @Nullable ServerPlayer player, CallbackInfoReturnable<Integer> cir) { // Paper - Add EntityDamageItemEvent
        // Paper start - itemstack damage api
        cir.setReturnValue(processDurabilityChange(damage, level, player, false));
    }

    @Unique
    private int processDurabilityChange(int damage, ServerLevel level, @Nullable LivingEntity player, boolean force) {
        // Paper end - itemstack damage api
        if (!this.isDamageableItem()) {
            return 0;
        } else if (player instanceof ServerPlayer && player.hasInfiniteMaterials() && !force) { // Paper - Add EntityDamageItemEvent
            return 0;
        } else {
            return damage > 0 ? EnchantmentHelper.processDurabilityChange(level, (ItemStack)(Object)this, damage) : damage;
        }
    }

    // NeoForge widens applyDamage to LivingEntity and reduces the vanilla
    // ServerPlayer overload to a delegate, so the body - including the shrink
    // call this injects before - now lives in the LivingEntity variant.
    @Inject(method = "applyDamage(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V", shift = At.Shift.BEFORE))
    private void applyDamagePaper(int damage, net.minecraft.world.entity.@Nullable LivingEntity player, Consumer<Item> onBreak, CallbackInfo ci) { // Paper - Add EntityDamageItemEvent
        // CraftBukkit start - Check for item breaking
        if (this.count == 1 && player instanceof final ServerPlayer serverPlayer) { // Paper - Add EntityDamageItemEvent
            org.bukkit.craftbukkit.event.CraftEventFactory.callPlayerItemBreakEvent(serverPlayer, (ItemStack)(Object)this); // Paper - Add EntityDamageItemEvent
        }
        // CraftBukkit end
    }

    @Unique
    private void applyDamage(int damage, @Nullable LivingEntity player, Consumer<Item> onBreak) { // Paper - Add EntityDamageItemEvent
        if (player instanceof final ServerPlayer serverPlayer) { // Paper - Add EntityDamageItemEvent
            CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(serverPlayer, (ItemStack)(Object)this, damage); // Paper - Add EntityDamageItemEvent
        }

        this.setDamageValue(damage);
        if (this.isBroken()) {
            Item item = this.getItem();
            // CraftBukkit start - Check for item breaking
            if (this.count == 1 && player instanceof final ServerPlayer serverPlayer) { // Paper - Add EntityDamageItemEvent
                org.bukkit.craftbukkit.event.CraftEventFactory.callPlayerItemBreakEvent(serverPlayer, (ItemStack)(Object)this); // Paper - Add EntityDamageItemEvent
            }
            // CraftBukkit end
            this.shrink(1);
            onBreak.accept(item);
        }
    }

    @Inject(method = "hurtWithoutBreaking", at = @At("HEAD"), cancellable = true)
    public void hurtWithoutBreaking(int damage, net.minecraft.world.entity.player.Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            int i = this.processDurabilityChange(damage, serverPlayer.level(), serverPlayer);
            if (i == 0) {
                return;
            }

            int min = Math.min(this.getDamageValue() + i, this.getMaxDamage() - 1); // Paper - Expand PlayerItemDamageEvent - diff on change as min computation is copied post event.

            // Paper start - Expand PlayerItemDamageEvent
            if (min - this.getDamageValue() > 0) {
                org.bukkit.event.player.PlayerItemDamageEvent event = new org.bukkit.event.player.PlayerItemDamageEvent(
                        (Player) ((EntityBridge) (Object) serverPlayer).getBukkitEntity(),
                        org.bukkit.craftbukkit.inventory.CraftItemStack.asCraftMirror((ItemStack)(Object)this),
                        min - this.getDamageValue(),
                        damage
                );
                if (!event.callEvent() || event.getDamage() == 0) {
                    return;
                }

                // Prevent breaking the item in this code path as callers may expect the item to survive
                // (given the method name)
                min = Math.min(this.getDamageValue() + event.getDamage(), this.getMaxDamage() - 1);
            }
            // Paper end - Expand PlayerItemDamageEvent

            this.applyDamage(min, serverPlayer, item -> {});
        }
        ci.cancel();
    }

    @Inject(method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V", at = @At("HEAD"), cancellable = true)
    public void hurtAndBreak(int amount, LivingEntity entity, EquipmentSlot slot, CallbackInfo ci) {
        // Paper start - add param to skip infinite mats check
        this.hurtAndBreak(amount, entity, slot, false);
        ci.cancel();
    }

    @Unique
    public void hurtAndBreak(int amount, LivingEntity entity, EquipmentSlot slot, boolean force) {
        // Paper end - add param to skip infinite mats check
        if (entity.level() instanceof ServerLevel serverLevel) {
            this.hurtAndBreak(
                    amount, serverLevel, entity, item -> {if (slot != null) entity.onEquippedItemBroken(item, slot); }, force // Paper - Add EntityDamageItemEvent & itemstack damage API - do not process entity related callbacks when damaging from API
            );
        }
    }
}
