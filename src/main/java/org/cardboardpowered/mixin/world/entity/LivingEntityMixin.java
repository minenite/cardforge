package org.cardboardpowered.mixin.world.entity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.cardboardpowered.bridge.world.entity.EntityBridge;
import org.cardboardpowered.bridge.world.entity.LivingEntityBridge;
import org.cardboardpowered.bridge.server.level.ServerPlayerBridge;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.attribute.CraftAttributeMap;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends EntityMixin implements LivingEntityBridge {

    private transient EntityPotionEffectEvent.Cause bukkitCause;
    private LivingEntity get() {
        return (LivingEntity)(Object)this;
    }

    @Shadow
    private AttributeMap attributes;

    private boolean PICE_canceled = false;

    /**
     * Bukkit's collidable, collidable and invulnerability duration have no vanilla
     * storage - CraftBukkit adds fields for them. They live here instead, and are
     * consulted by the hooks below.
     */
    @Unique
    private boolean cardboard$collidable = true;
    @Unique
    private int cardboard$invulnerableDuration = 20;

    @Override
    public void cardboard$setInvulnerableDuration(int ticks) {
        this.cardboard$invulnerableDuration = ticks;
    }

    @Override
    public int cardboard$getInvulnerableDuration() {
        return this.cardboard$invulnerableDuration;
    }


    // private CardboardAttributable craftAttributes;
    private CraftAttributeMap craftAttributes;

    @Override
    public CraftAttributeMap cardboard_getAttr() {
        if (null == craftAttributes) {

            this.craftAttributes = new CraftAttributeMap( get().getAttributes() ); // new CardboardAttributable(this.attributes);
        }
        return craftAttributes;
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"), 
            method = "completeUsingItem")
    public ItemStack doBukkitEvent_PlayerItemConsumeEvent(ItemStack s, Level w, LivingEntity e) {
        PICE_canceled = false;
        if (get() instanceof ServerPlayer) {
            org.bukkit.inventory.ItemStack craftItem = CraftItemStack.asBukkitCopy(get().useItem);
            PlayerItemConsumeEvent event = new PlayerItemConsumeEvent((Player) ((ServerPlayerBridge)((ServerPlayer) get())).getBukkitEntity(), craftItem);
            Bukkit.getServer().getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                ((Player)((ServerPlayerBridge)((ServerPlayer) get())).getBukkitEntity()).updateInventory();
                ((CraftPlayer)((ServerPlayerBridge)((ServerPlayer) get())).getBukkitEntity()).updateScaledHealth();
                PICE_canceled = true;
                return null;
            }
            return (craftItem.equals(event.getItem())) ? get().useItem.finishUsingItem(get().level(), get()) : CraftItemStack.asNMSCopy(event.getItem()).finishUsingItem(get().level(), get());
        } else return get().useItem.finishUsingItem(get().level(), get());
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setItemInHand(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V"),
            method = "completeUsingItem", cancellable = true)
    public void doBukkitEvent_PlayerItemConsumeEvent_FixCancel(CallbackInfo ci) {
        if (PICE_canceled) {
            ci.cancel();
            return;
        }
    }
    
    @Shadow
    public void dropEquipment( ServerLevel world) {
    	// Shadowed
    }

    @Inject(at = @At("HEAD"), method = "dropAllDeathLoot", cancellable = true)
    public void cardboard_doDrop(ServerLevel world, DamageSource damagesource, CallbackInfo ci) {
        Entity entity = damagesource.getEntity();

        boolean flag = get().lastHurtByPlayerMemoryTime > 0;
        this.dropEquipment(world);
        if (!get().isBaby() && world.getGameRules().get(GameRules.MOB_DROPS)) {
            this.dropFromLootTable(world, damagesource, flag);
            this.dropCustomDeathLoot((ServerLevel) world, damagesource, flag);
        }

        // NeoForge fires LivingDropsEvent inside dropAllDeathLoot, and this hook
        // cancels that method to run Bukkit's own drop handling instead - so the mod
        // event was never firing at all. Any mod adding or removing mob drops did
        // nothing, silently, because the drops still appeared: Cardboard spawns them
        // itself further down.
        //
        // Both ecosystems get a say now, mods first. The drop list is handed to
        // NeoForge as ItemEntities, since that is what its event expects, and read
        // back afterwards so anything a mod added or removed survives into the Bukkit
        // event. Nothing is spawned here; callEntityDeathEvent still owns that.
        java.util.List<org.bukkit.inventory.ItemStack> drops = ((EntityBridge) (Object) this).cardboard_getDrops();
        try {
            java.util.Collection<net.minecraft.world.entity.item.ItemEntity> asEntities = new ArrayList<>();
            for (org.bukkit.inventory.ItemStack stack : drops) {
                if (stack == null || stack.getType() == org.bukkit.Material.AIR || stack.getAmount() == 0) {
                    continue;
                }
                asEntities.add(new net.minecraft.world.entity.item.ItemEntity(world,
                        get().getX(), get().getY(), get().getZ(),
                        org.bukkit.craftbukkit.inventory.CraftItemStack.asNMSCopy(stack)));
            }

            if (net.neoforged.neoforge.common.CommonHooks.onLivingDrops(get(), damagesource, asEntities, flag)) {
                // A mod cancelled the drops outright.
                drops = new ArrayList<>();
            } else {
                drops = new ArrayList<>();
                for (net.minecraft.world.entity.item.ItemEntity item : asEntities) {
                    drops.add(org.bukkit.craftbukkit.inventory.CraftItemStack.asBukkitCopy(item.getItem()));
                }
            }
        } catch (Throwable t) {
            // Never let the bridge cost the drops themselves; fall back to the list as
            // collected, which is the pre-existing behaviour.
            org.cardboardpowered.CardboardMod.LOGGER.warning("Could not fire LivingDropsEvent for a death: " + t);
        }

        CraftEventFactory.callEntityDeathEvent(get(), damagesource, drops);
        ((EntityBridge) (Object) this).cardboard_setDrops(new ArrayList<>());
        this.dropExperience(world, damagesource.getEntity());
        ci.cancel();
        return;
    }

    /*@Shadow
    public int getXpToDrop(PlayerEntity entityhuman) {
        return 0;
    }*/

    
    // TODO: getExperienceToDrop
    
    @Shadow
    public int getBaseExperienceReward(ServerLevel world) {
    	return 0; // Shadowed
    }
    
    @Shadow
    public boolean isAlwaysExperienceDropper() {
    	return true; // Shadowed
    }
    
    @Override
    public int getExpReward() {
    	Level w = this.mc_world();
    	
    	if (w instanceof ServerLevel) {
    		ServerLevel sw = (ServerLevel) w;
            if ((this.isAlwaysExperienceDropper() || get().lastDamageStamp > 0 && this.isAlwaysExperienceDropper() && sw.getGameRules().get(GameRules.MOB_DROPS))) {
                //int i = getXpToDrop(get().attackingPlayer);
            	int i = getBaseExperienceReward(sw);
            	return i;
            }
    	}
        
        return 0;
    }

    @Override
    public void pushEffectCause(EntityPotionEffectEvent.Cause cause) {
        bukkitCause = cause;
    }

    @Shadow
    public void dropFromLootTable(ServerLevel world, DamageSource damagesource, boolean flag) {
    }

    //@Shadow
    //public void dropEquipment(DamageSource damagesource, int i, boolean flag) {
    //}
    
    @Shadow
    public void dropCustomDeathLoot( ServerLevel world, DamageSource source, boolean causedByPlayer) {
    }
    
    @Shadow
    public void dropExperience( ServerLevel world, Entity attacker) {}// dropXp( Entity attacker) {}

	@Shadow public abstract HumanoidArm getMainArm();

    @Shadow
    @Nullable
    protected abstract ItemEntity createItemStackToDrop(ItemStack itemStack, boolean bl, boolean bl2);

    @Shadow
    public abstract void swing(InteractionHand interactionHand);

    @Shadow
    public ItemStack useItem;

    @Shadow
    public abstract void stopUsingItem();

    @Shadow
    public abstract float getYHeadRot();

    /**
     * @reason Bukkit RegainHealthEvent
     */
    @Inject(at = @At("HEAD"), method = "heal", cancellable = true)
    public void doRegainHealthEvent(float f, CallbackInfo ci) {
        heal(f, EntityRegainHealthEvent.RegainReason.CUSTOM);
        ci.cancel();
        return;
    }

    public void heal(float f, EntityRegainHealthEvent.RegainReason regainReason) {
        if (get().getHealth() > 0.0F) {
            // NeoForge fires onLivingHeal at the top of heal() and lets a mod scale or
            // veto the amount. The hook above cancels that method to run this instead,
            // so the mod event was never firing: a mod halving or blocking healing did
            // nothing, and nothing looked wrong because the entity still healed.
            //
            // Mods decide the amount first, then the plugin sees the result, which is
            // the same order used for block breaking and death drops.
            float amount = f;
            try {
                amount = net.neoforged.neoforge.event.EventHooks.onLivingHeal(get(), amount);
            } catch (Throwable t) {
                org.cardboardpowered.CardboardMod.LOGGER.warning("Could not fire onLivingHeal: " + t);
            }
            if (amount <= 0.0F) {
                return;
            }

            EntityRegainHealthEvent event = new EntityRegainHealthEvent(this.getBukkitEntity(), amount, regainReason);
            if (this.isValidBF()) {
                Bukkit.getPluginManager().callEvent(event);
                if (!event.isCancelled()) get().setHealth((float) (get().getHealth() + event.getAmount()));
            }
        }
    }

    // Paper start - Extend dropItem API
    @Override
    public final @Nullable ItemEntity cardboard$drop(ItemStack stack, boolean randomizeMotion, boolean includeThrower) {
        return this.cardboard$drop(stack, randomizeMotion, includeThrower, true, null);
    }

    @Override
    public @Nullable ItemEntity cardboard$drop(ItemStack stack, boolean randomizeMotion, boolean includeThrower, boolean callEvent, java.util.function.@Nullable Consumer<org.bukkit.entity.Item> entityOperation) {
        // Paper end - Extend dropItem API
        if (stack.isEmpty()) {
            return null;
        } else if (this.level().isClientSide()) {
            this.swing(InteractionHand.MAIN_HAND);
            return null;
        } else {
            ItemEntity itemEntity = this.createItemStackToDrop(stack, randomizeMotion, includeThrower);
            if (itemEntity != null) {
                // CraftBukkit start - fire PlayerDropItemEvent
                if (entityOperation != null) entityOperation.accept((org.bukkit.entity.Item)((EntityBridge) (Object) itemEntity).getBukkitEntity());
                if (callEvent && this.getBukkitEntity() instanceof org.bukkit.entity.Player player) {
                    org.bukkit.entity.Item drop = (org.bukkit.entity.Item)((EntityBridge) (Object) itemEntity).getBukkitEntity();

                    org.bukkit.event.player.PlayerDropItemEvent event = new org.bukkit.event.player.PlayerDropItemEvent(player, drop);
                    CraftServer.INSTANCE.getPluginManager().callEvent(event);

                    if (event.isCancelled()) {
                        org.bukkit.inventory.ItemStack inHandItem = player.getInventory().getItemInMainHand();
                        if (includeThrower && inHandItem.getAmount() == 0) {
                            // The complete stack was dropped
                            player.getInventory().setItemInMainHand(drop.getItemStack());
                        } else if (includeThrower && inHandItem.isSimilar(drop.getItemStack()) && inHandItem.getAmount() < inHandItem.getMaxStackSize() && drop.getItemStack().getAmount() == 1) {
                            // Only one item is dropped
                            inHandItem.setAmount(inHandItem.getAmount() + 1);
                            player.getInventory().setItemInMainHand(inHandItem);
                        } else {
                            // Fallback
                            player.getInventory().addItem(drop.getItemStack());
                        }
                        return null;
                    }
                }
                // CraftBukkit end
                this.level().addFreshEntity(itemEntity);
            }

            return itemEntity;
        }
    }

    // CraftBukkit start
    @Override
    public float cardboard$getBukkitYaw() {
        return this.getYHeadRot();
    }
    // CraftBukkit end
}
