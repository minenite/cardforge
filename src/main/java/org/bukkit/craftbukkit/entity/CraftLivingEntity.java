package org.bukkit.craftbukkit.entity;

//<<<<<<< HEAD
//=======
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.CraftEquipmentSlot;
import com.google.common.base.Preconditions;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.damage.CraftDamageSource;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Consumer;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

//>>>>>>> upstream/ver/1.20
import com.destroystokyo.paper.block.TargetBlockInfo;
import com.destroystokyo.paper.block.TargetBlockInfo.FluidMode;
import com.destroystokyo.paper.entity.TargetEntityInfo;
import com.google.common.collect.Sets;
import com.javazilla.bukkitfabric.Utils;

import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.world.damagesource.CombatTracker;

import org.cardboardpowered.bridge.world.entity.EntityBridge;
import org.cardboardpowered.bridge.world.entity.LivingEntityBridge;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.util.TriState;
import net.minecraft.Optionull;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.waypoints.ServerWaypointManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.waypoints.WaypointStyleAsset;
import net.minecraft.world.waypoints.WaypointStyleAssets;

import org.bukkit.entity.*;
import org.bukkit.craftbukkit.potion.CraftPotionEffectType;
import org.bukkit.craftbukkit.potion.CraftPotionUtil;
import org.bukkit.craftbukkit.inventory.CraftEntityEquipment;
import org.cardboardpowered.impl.world.CraftWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"deprecation", "removal"})
public class CraftLivingEntity extends CraftEntity implements LivingEntity {

    private CraftEntityEquipment equipment;

    public CraftLivingEntity(net.minecraft.world.entity.Entity entity) {
        super(entity);
        this.entity = (net.minecraft.world.entity.LivingEntity) entity;
        if (entity instanceof Mob || entity instanceof ArmorStand) {
            equipment = new CraftEntityEquipment(this);
        }
    }

    public CraftLivingEntity(CraftServer server, net.minecraft.world.entity.Entity entity) {
        this(entity);
    }

    @Override
    public AttributeInstance getAttribute(Attribute att) {
        return ((LivingEntityBridge)this.getHandle()).cardboard_getAttr().getAttribute(att); //.getAttribute(att, nms.getAttributes());
    }

    @Override
    public void registerAttribute(Attribute attribute) {
        ((LivingEntityBridge)this.getHandle()).cardboard_getAttr().registerAttribute(attribute);
    }

    @Override
    public void damage(double arg0) {
        // nms.damage(DamageSource.MAGIC, (float)arg0);
    	damage(arg0, (Entity) null);
    }

    @Override
    public void damage(double arg0, Entity source) {
        // nms.damage(DamageSource.mob((net.minecraft.entity.LivingEntity) arg1), (float) arg0);
    	DamageSource reason = getHandle().damageSources().generic();

        if (source instanceof HumanEntity) {
            reason = getHandle().damageSources().playerAttack(((CraftHumanEntity) source).getHandle());
        } else if (source instanceof LivingEntity) {
            reason = getHandle().damageSources().mobAttack(((CraftLivingEntity) source).getHandle());
        }

        // nms.damage(reason, (float) arg0);
        damage(arg0, reason);
    }
    
    private void damage(double amount, DamageSource damageSource) {
        // Preconditions.checkArgument(damageSource != null, "damageSource cannot be null");
        // Preconditions.checkState(!this.getHandle().generation, "Cannot damage entity during world generation");

        this.getHandle().hurt(damageSource, (float) amount);
    }

    @Override
    public double getAbsorptionAmount() {
        return this.getHandle().getAbsorptionAmount();
    }

    @Override
    public double getHealth() {
        return this.getHandle().getHealth();
    }

    @Override
    public double getMaxHealth() {
        return this.getHandle().getMaxHealth();
    }

    @Override
    public void resetMaxHealth() {
        // Back to the attribute's own base value, undoing any setMaxHealth.
        net.minecraft.world.entity.ai.attributes.AttributeInstance attribute =
                this.getHandle().getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (attribute != null) {
            this.setMaxHealth(attribute.getAttribute().value().getDefaultValue());
        }
    }

    @Override
    public void setAbsorptionAmount(double arg0) {
        this.getHandle().setAbsorptionAmount((float)arg0);
    }

    @Override
    public void setHealth(double arg0) {
        float health = (float) arg0;
        // Setting health to zero has to run the death sequence, not just write the
        // attribute. Cardboard only wrote it, so the entity read as dead while
        // nothing that death normally triggers happened - no EntityDeathEvent, no
        // drops, no experience. Plugins that kill entities this way are common.
        if (health <= 0.0F) {
            this.getHandle().die(this.getHandle().damageSources().generic());
            this.getHandle().setHealth(0.0F);
            return;
        }
        this.getHandle().setHealth(health);
    }

    @Override
    public void setMaxHealth(double arg0) {
        // This used to call setHealth, which is a different attribute entirely:
        // asking for a bigger maximum silently healed the entity instead, and
        // asking for a smaller one damaged it.
        Preconditions.checkArgument(arg0 > 0, "Max health must be greater than 0");
        net.minecraft.world.entity.ai.attributes.AttributeInstance attribute =
                this.getHandle().getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        Preconditions.checkArgument(attribute != null, "Entity has no max health attribute");
        attribute.setBaseValue(arg0);
        if (this.getHandle().getHealth() > arg0) {
            this.getHandle().setHealth((float) arg0);
        }
    }

    @Override
    public <T extends Projectile> T launchProjectile(Class<? extends T> arg0) {
        return launchProjectile(arg0, null);
    }

    @Override
    public net.minecraft.world.entity.LivingEntity getHandle() {
        return (net.minecraft.world.entity.LivingEntity) this.entity;
    }

    @Override
    public <T extends Projectile> T launchProjectile(Class<? extends T> projectile, Vector velocity) {
    	return this.launchProjectile(projectile, velocity, null);
    }

    @Override
    public boolean addPotionEffect(PotionEffect effect) {
        return addPotionEffect(effect, false);
    }

    @Override
    public boolean addPotionEffect(PotionEffect effect, boolean force) {
        net.minecraft.world.entity.LivingEntity nms = this.getHandle();
        if (nms == null || effect == null || effect.getType() == null) {
            return false;
        }
        // Bukkit getId() is 1-indexed; MOB_EFFECT.byId is 0-indexed. Using byId(getId())
        // applied the wrong effect (or none), so gun ADS slowness never showed.
        return nms.addEffect(CraftPotionUtil.fromBukkit(effect));
    }

    @Override
    public boolean addPotionEffects(Collection<PotionEffect> effects) {
        boolean success = true;
        for (PotionEffect effect : effects)
            success &= addPotionEffect(effect);
        return success;
    }

    @Override
    public void attack(Entity arg0) {
        ((org.minenite.cardforge.mixin.invoker.LivingEntityInvoker) (Object) this.getHandle()).cardforge$doAutoAttackOnTouch(((CraftLivingEntity)arg0).getHandle());
    }

    @Override
    public Collection<PotionEffect> getActivePotionEffects() {
        List<PotionEffect> effects = new ArrayList<>();
        for (MobEffectInstance handle :  this.getHandle().activeEffects.values()) {
                // effects.add(new PotionEffect(PotionEffectType.getById(Registries.STATUS_EFFECT.getRawId(handle.getEffectType())), handle.getDuration(), handle.getAmplifier(), handle.isAmbient(), handle.shouldShowParticles()));
                effects.add(CraftPotionUtil.toBukkit(handle));
        
        }
        return effects;
    }

    @Override
    public boolean getCanPickupItems() {
        if (getHandle() instanceof Mob) {
            return ((Mob) getHandle()).canPickUpLoot();
        }
        return true; // todo
    }

    @Override
    public EntityEquipment getEquipment() {
        return equipment;
    }

    @Override
    public double getEyeHeight() {
        return entity.getEyeHeight();
    }

    @Override
    public double getEyeHeight(boolean arg0) {
        return getEyeHeight();
    }

    @Override
    public Location getEyeLocation() {
        Location loc = getLocation();
        loc.setY(loc.getY() + getEyeHeight());
        return loc;
    }

    @Override
    public Player getKiller() {
        return Optionull.map(this.getHandle().getLastHurtByPlayer(), player -> (Player) ((EntityBridge) (Object) player).getBukkitEntity());
    }


    @Override
    public double getLastDamage() {
        return  this.getHandle().lastHurt;
    }

    @Override
    public List<Block> getLastTwoTargetBlocks(Set<Material> arg0, int arg1) {
        return getLineOfSight(arg0, arg1, 2);
    }

    @Override
    public Entity getLeashHolder() throws IllegalStateException {
        return ((EntityBridge)((Mob) entity).getLeashHolder()).getBukkitEntity();
    }

    private List<Block> getLineOfSight(Set<Material> transparent, int maxDistance, int maxLength) {
        if (transparent == null)
            transparent = Sets.newHashSet(Material.AIR, Material.CAVE_AIR, Material.VOID_AIR);

        if (maxDistance > 120)
            maxDistance = 120;
        ArrayList<Block> blocks = new ArrayList<Block>();
        Iterator<Block> itr = new BlockIterator(this, maxDistance);
        while (itr.hasNext()) {
            Block block = itr.next();
            blocks.add(block);
            if (maxLength != 0 && blocks.size() > maxLength)
                blocks.remove(0);
            Material material = block.getType();
            if (!transparent.contains(material))
                break;
        }
        return blocks;
    }

    @Override
    public List<Block> getLineOfSight(Set<Material> transparent, int maxDistance) {
        return getLineOfSight(transparent, maxDistance, 0);
    }

    @Override
    public int getMaximumAir() {
        return entity.getMaxAirSupply();
    }

    @Override
    public int getMaximumNoDamageTicks() {
    	return ((org.cardboardpowered.bridge.world.entity.LivingEntityBridge) this.getHandle()).cardboard$getInvulnerableDuration();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getMemory(MemoryKey<T> arg0) {
        return (T)  this.getHandle().getBrain().getMemoryInternal(Utils.fromMemoryKey(arg0)).map(Utils::fromNmsGlobalPos).orElse(null);
    }

    @Override
    public int getNoDamageTicks() {
        return entity.invulnerableTime;
    }

    @Override
    public PotionEffect getPotionEffect(PotionEffectType arg0) {
        net.minecraft.world.entity.LivingEntity nms = this.getHandle();
        if (nms == null || arg0 == null) {
            return null;
        }
        Holder<MobEffect> holder = CraftPotionEffectType.bukkitToMinecraftHolder(arg0);
        MobEffectInstance handle = nms.getEffect(holder);
        return handle == null ? null : CraftPotionUtil.toBukkit(handle);
    }

    @Override
    public int getRemainingAir() {
        return entity.getAirSupply();
    }

    @Override
    public boolean getRemoveWhenFarAway() {
        // The inverse of persistenceRequired, and the read side of the setter.
        return this.getHandle() instanceof Mob mob && !mob.isPersistenceRequired();
    }

    @Override
    public Block getTargetBlock(Set<Material> arg0, int arg1) {
        List<Block> blocks = getLineOfSight(arg0, arg1, 1);
        return blocks.get(0);
    }

    @Override
    public Block getTargetBlockExact(int maxDistance) {
        return this.getTargetBlockExact(maxDistance, FluidCollisionMode.NEVER);
    }

    @Override
    public Block getTargetBlockExact(int maxDistance, FluidCollisionMode fluidCollisionMode) {
        RayTraceResult hitResult = this.rayTraceBlocks(maxDistance, fluidCollisionMode);
        return (hitResult != null ? hitResult.getHitBlock() : null);
    }

    @Override
    public boolean hasAI() {
        return (this.getHandle() instanceof Mob) ? !((Mob) this.getHandle()).isNoAi() : false;
    }

    @Override
    public boolean hasLineOfSight(Entity arg0) {
        return  this.getHandle().hasLineOfSight(((CraftEntity)arg0).entity);
    }

    @Override
    public boolean hasPotionEffect(PotionEffectType arg0) {
        net.minecraft.world.entity.LivingEntity nms = this.getHandle();
        if (nms == null || arg0 == null) {
            return false;
        }
        return nms.hasEffect(CraftPotionEffectType.bukkitToMinecraftHolder(arg0));
    }

    @Override
    public boolean isCollidable() {
        return this.getHandle().isPushable();
    }

    @Override
    public boolean isGliding() {
        // setGliding wrote shared flag 7 while this always answered false, so a
        // plugin could never read back what it had just set.
        return this.getHandle().isFallFlying();
    }

    @Override
    public boolean isLeashed() {
        if (!(getHandle() instanceof Mob))
            return false;
        return ((Mob) getHandle()).getLeashHolder() != null;
    }

    @Override
    public boolean isRiptiding() {
        return  this.getHandle().isAutoSpinAttack();
    }

    @Override
    public boolean isSleeping() {
        return  this.getHandle().isSleeping();
    }

    @Override
    public boolean isSwimming() {
        return entity.isSwimming();
    }

    @Override
    public RayTraceResult rayTraceBlocks(double maxDistance) {
        return this.rayTraceBlocks(maxDistance, FluidCollisionMode.NEVER);
    }

    @Override
    public RayTraceResult rayTraceBlocks(double maxDistance, FluidCollisionMode fluidCollisionMode) {
        Location eyeLocation = this.getEyeLocation();
        Vector direction = eyeLocation.getDirection();
        return this.getWorld().rayTraceBlocks(eyeLocation, direction, maxDistance, fluidCollisionMode, false);
    }

    @Override
    public void removePotionEffect(PotionEffectType type) {
        net.minecraft.world.entity.LivingEntity nms = this.getHandle();
        if (nms == null || type == null) {
            return;
        }
        nms.removeEffect(CraftPotionEffectType.bukkitToMinecraftHolder(type));
    }

    @Override
    public void setAI(boolean arg0) {
        if (this.getHandle() instanceof Mob)
            ((Mob) this.getHandle()).setNoAi(!arg0);
    }

    @Override
    public void setCanPickupItems(boolean pickup) {
        if (this.getHandle() instanceof Mob mob) {
            mob.setCanPickUpLoot(pickup);
        }
    }

    @Override
    public void setCollidable(boolean collidable) {
        ((org.cardboardpowered.bridge.world.entity.EntityBridge) (Object) this.getHandle()).cardboard$setCollidable(collidable);
    }

    @Override
    public void setGliding(boolean arg0) {
        entity.setSharedFlag(7, arg0);
    }

    @Override
    public void setLastDamage(double arg0) {
        this.getHandle().lastHurt = (float) arg0;
    }

    @Override
    public boolean setLeashHolder(Entity holder) {
        if ((entity instanceof WitherBoss) || !(entity instanceof Mob))
            return false;

        if (holder == null)
            return unleash();

        if (holder.isDead())
            return false;

        unleash();
        ((Mob) entity).setLeashedTo(((CraftEntity) holder).getHandle(), true);
        return true;
    }

    private boolean unleash() {
        if (!isLeashed())
            return false;
        // ((MobEntity) getHandle()).detachLeash(true, false);
        
        ((Mob) getHandle()).dropLeash();
        
        return true;
    }

    @Override
    public void setMaximumAir(int ticks) {
        ((org.cardboardpowered.bridge.world.entity.EntityBridge) (Object) this.getHandle()).cardboard$setMaxAirSupply(ticks);
    }

    @Override
    public void setMaximumNoDamageTicks(int ticks) {
        ((org.cardboardpowered.bridge.world.entity.LivingEntityBridge) this.getHandle()).cardboard$setInvulnerableDuration(ticks);
    }

    @Override
    public <T> void setMemory(MemoryKey<T> memoryKey, T memoryValue) {
        // The read side already worked; writing was missing entirely, so brain
        // memories set by plugins never reached the mob.
        this.getHandle().getBrain().setMemory(Utils.fromMemoryKey(memoryKey),
                memoryValue == null ? null : org.bukkit.craftbukkit.entity.memory.CraftMemoryMapper.toNms(memoryValue));
    }

    @Override
    public void setNoDamageTicks(int ticks) {
        // The read side already returned invulnerableTime; writing it was missing,
        // so plugins could not clear or extend damage immunity at all.
        this.getHandle().invulnerableTime = ticks;
    }

    @Override
    public void setRemainingAir(int ticks) {
        this.getHandle().setAirSupply(ticks);
    }

    @Override
    public void setRemoveWhenFarAway(boolean remove) {
        // Was a no-op, so a plugin could not stop a mob despawning - quest NPCs,
        // shop mobs and arena mobs would simply vanish once no player was near.
        // Bukkit's "remove when far away" is the inverse of NMS persistenceRequired.
        if (this.getHandle() instanceof net.minecraft.world.entity.Mob mob) {
            ((org.minenite.cardforge.mixin.invoker.MobInvoker) mob)
                    .cardforge$setPersistenceRequired(!remove);
        }
    }

    @Override
    public void setSwimming(boolean arg0) {
        entity.setSwimming(arg0);
    }

    @Override
    public void swingMainHand() {
        this.getHandle().swing(InteractionHand.MAIN_HAND);
    }

    @Override
    public void swingOffHand() {
        this.getHandle().swing(InteractionHand.OFF_HAND);
    }

    @Override
    public Set<UUID> getCollidableExemptions() {
        // Returned null, so the documented "add a UUID to this set" usage threw a
        // NullPointerException instead of exempting anything.
        return ((org.cardboardpowered.bridge.world.entity.EntityBridge) (Object) this.getHandle())
                .cardboard$getCollidableExemptions();
    }

    @Override
    public EntityCategory getCategory() {
        // Bukkit's categories are the enchantment-damage groups, which modern
        // Minecraft expresses as entity type tags rather than a mob-type field.
        net.minecraft.core.Holder<net.minecraft.world.entity.EntityType<?>> type =
                this.getHandle().getType().builtInRegistryHolder();
        if (type.is(net.minecraft.tags.EntityTypeTags.UNDEAD)) return EntityCategory.UNDEAD;
        if (type.is(net.minecraft.tags.EntityTypeTags.ARTHROPOD)) return EntityCategory.ARTHROPOD;
        if (type.is(net.minecraft.tags.EntityTypeTags.ILLAGER)) return EntityCategory.ILLAGER;
        if (type.is(net.minecraft.tags.EntityTypeTags.AQUATIC)) return EntityCategory.WATER;
        return EntityCategory.NONE;
    }

    public void setArrowsInBody(int count) {
        Preconditions.checkArgument(count >= 0, "New arrow amount must be >= 0");
        this.getHandle().setArrowCount(count);
    }

    public int getArrowsInBody() {
        return this.getHandle().getArrowCount();
    }

    public void setArrowCooldown(int i) {}
    public int getArrowCooldown() { return -1; }

    // Spigot-743
    public boolean isInvisible() {
        return getHandle().isInvisible();
    }

    // Spigot-743
    public void setInvisible(boolean invisible) {
        // TODO getHandle().persistentInvisibility = invisible;
        getHandle().setSharedFlag(5, invisible);
    }

    // PaperAPI - start
    public boolean isJumping() {
        return getHandle().jumping;
    }

    public void setJumping(boolean jumping) {
        getHandle().setJumping(jumping);
        if (jumping && getHandle() instanceof Mob)
            ((Mob) getHandle()).getJumpControl().tick();
    }

    @Override
    public boolean fromMobSpawner() {
        return this.getEntitySpawnReason() == SpawnReason.SPAWNER;
    }

    @Override
    public Chunk getChunk() {
        return super.getChunk();
    }

    @Override
    public SpawnReason getEntitySpawnReason() {
        // Recorded when the entity is added to the world; DEFAULT covers anything
        // that predates the tracking, which is better than the null this returned.
        SpawnReason reason = ((org.cardboardpowered.bridge.world.entity.EntityBridge) (Object) this.getHandle())
                .cardboard$getSpawnReason();
        return reason == null ? SpawnReason.DEFAULT : reason;
    }

    @Override
    public boolean isInBubbleColumn() {
        return this.getHandle().getInBlockState().is(net.minecraft.world.level.block.Blocks.BUBBLE_COLUMN);
    }

    @Override
    public boolean isInWaterOrBubbleColumn() {
        return this.getHandle().isInWater() || this.isInBubbleColumn();
    }

    @Override
    public boolean isInWaterOrRain() {
        return this.getHandle().isInWaterOrRain();
    }

    @Override
    public boolean isInWaterOrRainOrBubbleColumn() {
        return this.getHandle().isInWaterOrRain() || this.isInBubbleColumn();
    }

    @Override
    public void clearActiveItem() {
        this.getHandle().stopUsingItem();
    }

    @Override
    public ItemStack getActiveItem() {
        return org.bukkit.craftbukkit.inventory.CraftItemStack.asCraftMirror(this.getHandle().getUseItem());
    }

    @Override
    public int getArrowsStuck() {
        return this.getHandle().getArrowCount();
    }

    @Override
    public int getHandRaisedTime() {
        return this.getHandle().getTicksUsingItem();
    }

    @Override
    public float getHurtDirection() {
        return this.getHandle().getHurtDir();
    }

    @Override
    public int getItemUseRemainingTime() {
        return this.getHandle().getUseItemRemainingTicks();
    }

    @Override
    public int getShieldBlockingDelay() {
        // Modern Minecraft keeps this on the item's blocks_attacks component
        // rather than on the entity, so it is read from whatever is being raised.
        net.minecraft.world.item.component.BlocksAttacks blocks =
                this.getHandle().getUseItem().get(net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS);
        if (blocks == null) {
            blocks = this.getHandle().getItemInHand(InteractionHand.OFF_HAND)
                    .get(net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS);
        }
        return blocks == null ? 0 : Math.round(blocks.blockDelaySeconds() * 20.0F);
    }

    @Override
    public Block getTargetBlock(int maxDistance, FluidMode fluidMode) {
        return this.getTargetBlockExact(maxDistance, fluidMode.bukkit);
    }

    @Override
    public BlockFace getTargetBlockFace(int arg0, FluidMode arg1) {
    	return this.getTargetBlockFace(arg0, arg1.bukkit);
    }

    @Override
    public TargetBlockInfo getTargetBlockInfo(int maxDistance, FluidMode fluidMode) {
        RayTraceResult hit = this.rayTraceBlocks(maxDistance, fluidMode.bukkit);
        return (hit == null || hit.getHitBlock() == null) ? null
                : new TargetBlockInfo(hit.getHitBlock(), hit.getHitBlockFace());
    }

    @Override
    public Entity getTargetEntity(int maxDistance, boolean ignoreBlocks) {
        TargetEntityInfo info = this.getTargetEntityInfo(maxDistance, ignoreBlocks);
        return info == null ? null : info.getEntity();
    }

    @Override
    public TargetEntityInfo getTargetEntityInfo(int maxDistance, boolean ignoreBlocks) {
        // Trace along the look vector, stopping at the first block unless the
        // caller asked to see through them.
        Location eye = this.getEyeLocation();
        double distance = maxDistance;
        if (!ignoreBlocks) {
            RayTraceResult blocks = this.rayTraceBlocks(maxDistance);
            if (blocks != null) {
                distance = eye.toVector().distance(blocks.getHitPosition());
            }
        }

        RayTraceResult hit = this.getWorld().rayTraceEntities(eye, eye.getDirection(), distance,
                entity -> entity != this);
        return (hit == null || hit.getHitEntity() == null) ? null
                : new TargetEntityInfo(hit.getHitEntity(), hit.getHitPosition());
    }

    @Override
    public boolean isHandRaised() {
    	return this.getHandle().isUsingItem();
    }

    @Override
    public void playPickupItemAnimation(Item item, int quantity) {
        // The animation of the item flying into this entity, which every viewer
        // in range sees. Nothing was sent before, so scripted pickups looked like
        // items simply vanishing.
        this.getHandle().take(((CraftEntity) item).getHandle(), quantity);
    }

    @Override
    public void setArrowsStuck(int arg0) {
    	this.getHandle().setArrowCount(arg0);
    }

    @Override
    public void setHurtDirection(float hurtDirection) {
        // Only players carry a hurt direction; on anything else getHurtDir is a
        // hardcoded zero, so there is nothing to write.
        if (this.getHandle() instanceof net.minecraft.world.entity.player.Player) {
            ((org.cardboardpowered.bridge.world.entity.player.PlayerBridge) this.getHandle()).cardboard$setHurtDir(hurtDirection);
        }
    }

    @Override
    public void setKiller(Player killer) {
        // Ignored its argument, so a plugin could not attribute a kill - death
        // messages and "killed by" statistics stayed with whoever vanilla thought
        // it was. The memory time is vanilla's own hundred-tick window.
        this.getHandle().setLastHurtByPlayer(
                killer == null ? null : ((CraftPlayer) killer).getHandle(), 100);
    }

    @Override
    public void setShieldBlockingDelay(int ticks) {
    	// Not implemented: the delay comes from the blocks_attacks component on the
    	// item being raised, not from the entity, so it cannot be set per mob.
    	throw new UnsupportedOperationException(
    			"Shield blocking delay is a property of the item's blocks_attacks component, not the entity");
    }
    // PaperAPI - end

    @Override
    public @NotNull EquipmentSlot getHandRaised() {
        InteractionHand hand = this.getHandle().getUsedItemHand();
        return hand == InteractionHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND;
    }

    @Override
    public boolean hasLineOfSight(@NotNull Location location) {
        Preconditions.checkArgument(location != null, "Location cannot be null");
        // Said "no" for every location, including one directly in front of the
        // entity. A clip from the eyes answers it the way vanilla sight checks do.
        if (!this.getWorld().equals(location.getWorld())) return false;

        net.minecraft.world.phys.Vec3 eyes = this.getHandle().getEyePosition();
        net.minecraft.world.phys.Vec3 target =
                new net.minecraft.world.phys.Vec3(location.getX(), location.getY(), location.getZ());
        return this.getHandle().level().clip(new net.minecraft.world.level.ClipContext(
                eyes, target,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                this.getHandle())).getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }

    // 1.17 API START
    @Override
    public boolean isClimbing() {
        return this.getHandle().onClimbable();
    }

    @Override
    public int getBeeStingerCooldown() {
        return this.getHandle().removeStingerTime;
    }

    @Override
    public int getBeeStingersInBody() {
        return this.getHandle().getStingerCount();
    }

    @Override
    public void setBeeStingerCooldown(int ticks) {
        this.getHandle().removeStingerTime = ticks;
    }

    @Override
    public void setBeeStingersInBody(int count) {
        Preconditions.checkArgument(count >= 0, "New bee stinger amount must be >= 0");
        this.getHandle().setStingerCount(count);
    }
    
    // 1.19.2

	// @Override
	public <T extends Projectile> @NotNull T launchProjectile_old(@NotNull Class<? extends T> projectile, @Nullable Vector velocity,
			@Nullable Consumer<T> function) {
		return this.launchProjectile(projectile, velocity, function);
	}
	
	// @Override
	public <T extends Projectile> T launchProjectile(Class<? extends T> projectile, Vector velocity, java.util.function.Consumer<? super T> function) {
		ServerLevel world = ((CraftWorld)this.getWorld()).getHandle();
        net.minecraft.world.entity.projectile.Projectile launch = null;
        if (Snowball.class.isAssignableFrom(projectile)) {
            launch = new net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball(world, this.getHandle(), new net.minecraft.world.item.ItemStack(Items.SNOWBALL));
            ((ThrowableProjectile)launch).shootFromRotation(this.getHandle(), this.getHandle().getXRot(), this.getHandle().getYRot(), 0.0f, 1.5f, 1.0f);
        } else if (Egg.class.isAssignableFrom(projectile)) {
            launch = new ThrownEgg(world, this.getHandle(), new net.minecraft.world.item.ItemStack(Items.EGG));
            ((ThrowableProjectile)launch).shootFromRotation(this.getHandle(), this.getHandle().getXRot(), this.getHandle().getYRot(), 0.0f, 1.5f, 1.0f);
        } else if (EnderPearl.class.isAssignableFrom(projectile)) {
            launch = new ThrownEnderpearl(world, this.getHandle(), new net.minecraft.world.item.ItemStack(Items.ENDER_PEARL));
            ((ThrowableProjectile)launch).shootFromRotation(this.getHandle(), this.getHandle().getXRot(), this.getHandle().getYRot(), 0.0f, 1.5f, 1.0f);
        } else if (AbstractArrow.class.isAssignableFrom(projectile)) {
            if (TippedArrow.class.isAssignableFrom(projectile)) {
                launch = new Arrow(world, this.getHandle(), new net.minecraft.world.item.ItemStack(Items.ARROW), null);
                ((org.bukkit.entity.Arrow) ((org.cardboardpowered.bridge.world.entity.EntityBridge) (Object) launch).getBukkitEntity()).setBasePotionType(PotionType.WATER);
            } else {
                launch = SpectralArrow.class.isAssignableFrom(projectile) ? new net.minecraft.world.entity.projectile.arrow.SpectralArrow(world, this.getHandle(), new net.minecraft.world.item.ItemStack(Items.SPECTRAL_ARROW), null) : (Trident.class.isAssignableFrom(projectile) ? new ThrownTrident(world, this.getHandle(), new net.minecraft.world.item.ItemStack(Items.TRIDENT)) : new Arrow(world, this.getHandle(), new net.minecraft.world.item.ItemStack(Items.ARROW), null));
            }
            ((net.minecraft.world.entity.projectile.arrow.AbstractArrow)launch).shootFromRotation(this.getHandle(), this.getHandle().getXRot(), this.getHandle().getYRot(), 0.0f, Trident.class.isAssignableFrom(projectile) ? 2.5f : 3.0f, 1.0f);
        } else if (ThrownPotion.class.isAssignableFrom(projectile)) {
        	launch = LingeringPotion.class.isAssignableFrom(projectile) ? new ThrownLingeringPotion(world, this.getHandle(), new net.minecraft.world.item.ItemStack(Items.LINGERING_POTION)) : new ThrownSplashPotion(world, this.getHandle(), new net.minecraft.world.item.ItemStack(Items.SPLASH_POTION));
            ((ThrowableProjectile)launch).shootFromRotation(this.getHandle(), this.getHandle().getXRot(), this.getHandle().getYRot(), -20.0f, 0.5f, 1.0f);
        } else if (ThrownExpBottle.class.isAssignableFrom(projectile)) {
            launch = new ThrownExperienceBottle(world, this.getHandle(), new net.minecraft.world.item.ItemStack(Items.EXPERIENCE_BOTTLE));
            ((ThrowableProjectile)launch).shootFromRotation(this.getHandle(), this.getHandle().getXRot(), this.getHandle().getYRot(), -20.0f, 0.7f, 1.0f);
        } else if (FishHook.class.isAssignableFrom(projectile) && this.getHandle() instanceof net.minecraft.world.entity.player.Player) {
            // launch = new FishingBobberEntity((PlayerEntity)this.getHandle(), world, 0, 0, new net.minecraft.item.ItemStack(Items.FISHING_ROD));
            
        	launch = net.minecraft.world.entity.EntityTypes.FISHING_BOBBER.create(world, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        	// launch.refreshPositionAndAngles(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
            
        } else if (Fireball.class.isAssignableFrom(projectile)) {
            Location location = this.getEyeLocation();
            Vector direction = location.getDirection().multiply(10);
            Vec3 vec = new Vec3(direction.getX(), direction.getY(), direction.getZ());
            if (SmallFireball.class.isAssignableFrom(projectile)) {
                launch = new net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball(world, this.getHandle(), vec);
            } else if (WitherSkull.class.isAssignableFrom(projectile)) {
                launch = new net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull(world, this.getHandle(), vec);
            } else if (DragonFireball.class.isAssignableFrom(projectile)) {
                launch = new net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball(world, this.getHandle(), vec);
            } else if (AbstractWindCharge.class.isAssignableFrom(projectile)) {
                launch = BreezeWindCharge.class.isAssignableFrom(projectile)
                		? net.minecraft.world.entity.EntityTypes.BREEZE_WIND_CHARGE.create(world, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED)
                		: net.minecraft.world.entity.EntityTypes.WIND_CHARGE.create(world, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
                ((net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge)launch).setOwner(this.getHandle());
                ((net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge)launch).shootFromRotation(this.getHandle(), this.getHandle().getXRot(), this.getHandle().getYRot(), 0.0f, 1.5f, 1.0f);
            } else {
                launch = new LargeFireball(world, this.getHandle(), vec, 1);
            }
            ((org.cardboardpowered.bridge.world.entity.EntityBridge) (Object) ((AbstractHurtingProjectile)launch)).setProjectileSourceBukkit(this);
            // TODO: launch.preserveMotion = true;
            launch.snapTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        } else if (LlamaSpit.class.isAssignableFrom(projectile)) {
            Location location = this.getEyeLocation();
            Vector direction = location.getDirection();
            launch = net.minecraft.world.entity.EntityTypes.LLAMA_SPIT.create(world, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
            ((net.minecraft.world.entity.projectile.LlamaSpit)launch).setOwner(this.getHandle());
            ((net.minecraft.world.entity.projectile.LlamaSpit)launch).shoot(direction.getX(), direction.getY(), direction.getZ(), 1.5f, 10.0f);
            launch.snapTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        } else if (ShulkerBullet.class.isAssignableFrom(projectile)) {
            Location location = this.getEyeLocation();
            launch = new net.minecraft.world.entity.projectile.ShulkerBullet(world, this.getHandle(), null, null);
            launch.snapTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        } else if (Firework.class.isAssignableFrom(projectile)) {
            Location location = this.getEyeLocation();
            
            // TODO
            
            /*
            launch = new FireworkRocketEntity(world, FireworkRocketEntity.getDefaultStack(), this.getHandle(), location.getX(), location.getY() - (double)0.15f, location.getZ(), true);
            float f2 = 0.0f;
            int projectileSize = 1;
            int i2 = 0;
            float f3 = projectileSize == 1 ? 0.0f : 2.0f * f2 / (float)(projectileSize - 1);
            float f4 = (float)((projectileSize - 1) % 2) * f3 / 2.0f;
            float f5 = 1.0f;
            float yaw = f4 + f5 * (float)((i2 + 1) / 2) * f3;
            Vec3d vec3 = this.getHandle().getOppositeRotationVector(1.0f);
            Quaternionf quaternionf = new Quaternionf().setAngleAxis((double)(yaw * ((float)Math.PI / 180)), vec3.x, vec3.y, vec3.z);
            Vec3d vec32 = this.getHandle().getRotationVec(1.0f);
            Vector3f vector3f = vec32.toVector3f().rotate((Quaternionfc)quaternionf);
            ((FireworkRocketEntity)launch).setVelocity(vector3f.x(), vector3f.y(), vector3f.z(), 1.6f, 1.0f);
            */
            
            launch = new FireworkRocketEntity(world, net.minecraft.world.item.ItemStack.EMPTY, getHandle());
            launch.snapTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
            
        }
        // Preconditions.checkArgument((launch != null ? 1 : 0) != 0, (String)"Projectile (%s) not supported", (Object)projectile.getName());
        if (velocity != null) {
            ((Projectile) ((org.cardboardpowered.bridge.world.entity.EntityBridge) (Object) launch).getBukkitEntity()).setVelocity(velocity);
        }
        if (function != null) {
            function.accept((T) (Projectile) ((org.cardboardpowered.bridge.world.entity.EntityBridge) (Object) launch).getBukkitEntity());
        }
        world.addFreshEntity(launch);
        return (T)((Projectile) ((org.cardboardpowered.bridge.world.entity.EntityBridge) (Object) launch).getBukkitEntity());
	}

	@Override
	public @NotNull TriState getFrictionState() {
		// Returned null where the API promises a TriState, so callers that
		// switched on it hit a NullPointerException.
		return ((org.cardboardpowered.bridge.world.entity.EntityBridge) (Object) this.getHandle())
				.cardboard$getFrictionState();
	}

	@Override
	public void setFrictionState(@NotNull TriState state) {
		Preconditions.checkArgument(state != null, "Friction state cannot be null");
		((org.cardboardpowered.bridge.world.entity.EntityBridge) (Object) this.getHandle())
				.cardboard$setFrictionState(state);
	}

	public void broadcastSlotBreak(EquipmentSlot slot) {
        this.getHandle().level().broadcastEntityEvent(this.getHandle(), net.minecraft.world.entity.LivingEntity.entityEventForEquipmentBreak(CraftEquipmentSlot.getNMS(slot)));
	}

	public void broadcastSlotBreak(EquipmentSlot slot, Collection<Player> players) {
		if (players.isEmpty()) {
			return;
		}
		ClientboundEntityEventPacket packet = new ClientboundEntityEventPacket(this.getHandle(), net.minecraft.world.entity.LivingEntity.entityEventForEquipmentBreak( CraftEquipmentSlot.getNMS(slot)));
		players.forEach(player -> ((CraftPlayer)player).getHandle().connection.send(packet));
	}

	@Override
    public boolean canBreatheUnderwater() {
        return this.getHandle().canBreatheUnderwater();
    }

	@Override
    public ItemStack damageItemStack(ItemStack stack, int amount) {
        net.minecraft.world.item.ItemStack nmsStack;
        if (stack instanceof CraftItemStack) {
            CraftItemStack craftItemStack = (CraftItemStack)stack;
            if (craftItemStack.handle == null || craftItemStack.handle.isEmpty()) {
                return stack;
            }
            nmsStack = craftItemStack.handle;
        } else {
            nmsStack = CraftItemStack.asNMSCopy(stack);
            stack = CraftItemStack.asCraftMirror(nmsStack);
        }
        this.damageItemStack0(nmsStack, amount, null);
        return stack;
    }

	@Override
    public void damageItemStack(EquipmentSlot slot, int amount) {
        net.minecraft.world.entity.EquipmentSlot nmsSlot = CraftEquipmentSlot.getNMS(slot);
        this.damageItemStack0(this.getHandle().getItemBySlot(nmsSlot), amount, nmsSlot);
    }
	
    private void damageItemStack0(net.minecraft.world.item.ItemStack nmsStack, int amount, net.minecraft.world.entity.EquipmentSlot slot) {
        /*nmsStack.damage(amount, this.getHandle(), livingEntity -> {
            if (slot != null) {
                livingEntity.sendEquipmentBreakStatus(slot);
            }
        });*/
        
        nmsStack.hurtAndBreak(amount, this.getHandle(), slot);
    }

	
	@Override
	public @Nullable Sound getDeathSound() {
		// Every one of these answered with the generic sound no matter what the
		// entity was, so a plugin asking a creeper for its death sound got the
		// player one.
		net.minecraft.sounds.SoundEvent sound =
				((org.minenite.cardforge.mixin.invoker.LivingEntityInvoker) (Object) this.getHandle()).cardforge$getDeathSound();
		return sound == null ? null : org.bukkit.craftbukkit.CraftSound.minecraftToBukkit(sound);
	}

	@Override
	public @NotNull Sound getDrinkingSound(@NotNull ItemStack item) {
		return consumeSound(item, Sound.ENTITY_GENERIC_DRINK);
	}

	@Override
	public @NotNull Sound getEatingSound(@NotNull ItemStack item) {
		return consumeSound(item, Sound.ENTITY_GENERIC_EAT);
	}

	@Override
	public @NotNull Sound getFallDamageSound(int fallHeight) {
		// Vanilla splits fall sounds at four blocks.
		return fallHeight > 4 ? this.getFallDamageSoundBig() : this.getFallDamageSoundSmall();
	}

	@Override
	public @NotNull Sound getFallDamageSoundBig() {
		return org.bukkit.craftbukkit.CraftSound.minecraftToBukkit(this.getHandle().getFallSounds().big());
	}

	@Override
	public @NotNull Sound getFallDamageSoundSmall() {
		return org.bukkit.craftbukkit.CraftSound.minecraftToBukkit(this.getHandle().getFallSounds().small());
	}

	@Override
	public @Nullable Sound getHurtSound() {
		return this.getHurtSound(new CraftDamageSource(this.getHandle().damageSources().generic()));
	}

	/**
	 * The sound an item makes when consumed, which lives on the item's consumable
	 * component rather than on the entity.
	 */
	private Sound consumeSound(ItemStack item, Sound fallback) {
		Preconditions.checkArgument(item != null, "ItemStack cannot be null");
		net.minecraft.world.item.component.Consumable consumable =
				org.bukkit.craftbukkit.inventory.CraftItemStack.asNMSCopy(item)
						.get(net.minecraft.core.component.DataComponents.CONSUMABLE);
		return consumable == null ? fallback : org.bukkit.craftbukkit.CraftSound.minecraftHolderToBukkit(consumable.sound());
	}

	@Override
	public @Nullable Sound getHurtSound(org.bukkit.damage.DamageSource damageSource) {
		final DamageSource nms = damageSource instanceof CraftDamageSource craft
				? craft.getHandle() : this.getHandle().damageSources().generic();
		final net.minecraft.sounds.SoundEvent sound = ((org.minenite.cardforge.mixin.invoker.LivingEntityInvoker) (Object) this.getHandle()).cardforge$getHurtSound(nms);
		return sound == null ? null : org.bukkit.craftbukkit.CraftSound.minecraftToBukkit(sound);
	}

	@Override
	public float getSoundVolume() {
		return ((org.minenite.cardforge.mixin.invoker.LivingEntityInvoker) (Object) this.getHandle()).cardforge$getSoundVolume();
	}

	@Override
	public float getSoundPitch() {
		return this.getHandle().getVoicePitch();
	}

	@Override
	public void knockback(double arg0, double arg1, double arg2) {
		 // 26.2: LivingEntity#knockback(double,double,double) was removed
		 this.getHandle().push(arg0, arg1, arg2);
	}
	
	// 1.19.4:

	@Override
    public float getBodyYaw() {
        return this.getHandle().getVisualRotationYInDegrees();
    }

	@Override
    public BlockFace getTargetBlockFace(int maxDistance, FluidCollisionMode fluidMode) {
        RayTraceResult result = this.rayTraceBlocks(maxDistance, fluidMode);
        return result != null ? result.getHitBlockFace() : null;
    }

	@Override
    public RayTraceResult rayTraceEntities(int maxDistance, boolean ignoreBlocks) {
        EntityHitResult rayTrace = this.rayTraceEntity(maxDistance, ignoreBlocks);
        return null;
        //return rayTrace == null ? null : new RayTraceResult(CraftVector.toBukkit(rayTrace.getPos()), ((IMixinEntity)rayTrace.getEntity()).getBukkitEntity());
    }
	
    public EntityHitResult rayTraceEntity(int maxDistance, boolean ignoreBlocks) {
        return null;
    }

	@Override
    public void setArrowsInBody(int count, boolean fireEvent) {
        // Preconditions.checkArgument((count >= 0 ? 1 : 0) != 0, (Object)"New arrow amount must be >= 0");
        if (!fireEvent) {
            this.getHandle().getEntityData().set(net.minecraft.world.entity.LivingEntity.DATA_ARROW_COUNT_ID, count);
        } else {
            this.getHandle().setArrowCount(count);
        }
    }

	@Override
	public void setBodyYaw(float arg0) {
        this.getHandle().setYBodyRot(arg0);
	}

	@Override
	public int getNoActionTicks() {
		return this.getHandle().getNoActionTime();
	}

	@Override
	public void setNoActionTicks(int ticks) {
		Preconditions.checkArgument(ticks >= 0, "ticks must be >= 0");
		this.getHandle().setNoActionTime(ticks);
	}

	@Override
	public boolean clearActivePotionEffects() {
		return this.getHandle().removeAllEffects();
	}

	@Override
	public void playHurtAnimation(float yaw) {
		// The red flash and knockback tilt, sent to everyone tracking this entity.
		net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket packet =
				new net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket(this.getHandle().getId(), yaw);
		for (Player player : this.getWorld().getPlayers()) {
			if (player.canSee(this) || player == this) {
				((CraftPlayer) player).getHandle().connection.send(packet);
			}
		}
	}
	
	// 1.20.2 API:
	@Override
    public float getSidewaysMovement() {
        return this.getHandle().xxa;
    }

	@Override
    public float getForwardsMovement() {
        return this.getHandle().zza;
    }

	@Override
    public float getUpwardsMovement() {
        return this.getHandle().yya;
    }
   
	// 1.20.4 API:
	
	@Override
    public boolean hasActiveItem() {
        return this.getHandle().isUsingItem();
    }
	
	@Override
    public EquipmentSlot getActiveItemHand() {
        return CraftEquipmentSlot.getHand(this.getHandle().getUsedItemHand());
    }
	
    @Override
    public void setItemInUseTicks(int ticks) {
        // TODO
    	// this.getHandle().itemUseTimeLeft = ticks;
    }
    
    @Override
    public int getItemInUseTicks() {
        return this.getHandle().getUseItemRemainingTicks();
    }
    
    @Override
    public void startUsingItem(EquipmentSlot hand) {
        switch (hand) {
            case HAND: {
                this.getHandle().startUsingItem(InteractionHand.MAIN_HAND);
                break;
            }
            case OFF_HAND: {
                this.getHandle().startUsingItem(InteractionHand.OFF_HAND);
                break;
            }
            default: {
                throw new IllegalArgumentException("hand may only be HAND or OFF_HAND");
            }
        }
    }

    @Override
    public ItemStack getItemInUse() {
        net.minecraft.world.item.ItemStack item = this.getHandle().getUseItem();
        return item.isEmpty() ? null : CraftItemStack.asCraftMirror(item);
    }
    
    @Override
    public void completeUsingActiveItem() {
        // TODO
    	// this.getHandle().consumeItem();
    }
    
    @Override
    public int getActiveItemRemainingTime() {
        return this.getHandle().getUseItemRemainingTicks();
    }
    
    @Override
    public void setActiveItemRemainingTime(int ticks) {
    	// TODO
    	// this.getHandle().itemUseTimeLeft = ticks;
    }
    
    @Override
    public int getNextArrowRemoval() {
        return this.getHandle().removeArrowTime;
    }
    
    @Override
    public void setNextArrowRemoval(int ticks) {
        this.getHandle().removeArrowTime = ticks;
    }
    
    @Override
    public int getNextBeeStingerRemoval() {
        return this.getHandle().removeStingerTime;
    }
    
    @Override
    public void setNextBeeStingerRemoval(int ticks) {
        this.getHandle().removeStingerTime = ticks;
    }

	@Override
	public void damage(double amount, org.bukkit.damage.@NotNull DamageSource damageSource) {
		Preconditions.checkArgument(damageSource != null, "DamageSource cannot be null");
		// Damage with an explicit source did nothing at all, so plugins using the
		// modern API could not hurt anything.
		DamageSource nms = damageSource instanceof CraftDamageSource craft
				? craft.getHandle() : this.getHandle().damageSources().generic();
		this.getHandle().hurtServer(
				(net.minecraft.server.level.ServerLevel) this.getHandle().level(), nms, (float) amount);
	}

	@Override
	public int getActiveItemUsedTime() {
		return this.getHandle().getTicksUsingItem();
	}
	
	// 1.20.6 API:

	@Override
	public void heal(double amount, @NotNull RegainReason reason) {
		Preconditions.checkArgument(reason != null, "RegainReason cannot be null");
		// The reason was dropped, so every plugin heal reached EntityRegainHealthEvent
		// as CUSTOM and listeners could not tell them apart.
		org.bukkit.event.entity.EntityRegainHealthEvent event =
				new org.bukkit.event.entity.EntityRegainHealthEvent(this, amount, reason);
		this.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled()) return;
		this.setHealth(Math.min(this.getHealth() + event.getAmount(), this.getMaxHealth()));
	}

	@Override
	public boolean canUseEquipmentSlot(@NotNull EquipmentSlot slot) {
		net.minecraft.world.entity.EquipmentSlot es = CraftEquipmentSlot.getNMS(slot);
		return this.getHandle().canUseSlot( es );
	}
	
	// 1.21:

	@Override
	public void broadcastHurtAnimation(@NotNull Collection<Player> players) {
		 for (Player player : players) {
			 ((CraftPlayer)player).sendHurtAnimation(0.0f, this);
		 }
	}

	@Override
	public void setRiptiding(boolean riptiding) {
		// this.getHandle().setLivingFlag(4, riptiding);
	}

	@Override
	public @NotNull CombatTracker getCombatTracker() {
		return new org.bukkit.craftbukkit.damage.CraftCombatTracker(this.getHandle().getCombatTracker());
	}

	@Override
	public void setWaypointStyle(@Nullable Key key) {
		final ResourceKey<WaypointStyleAsset> newKey = key == null
				? WaypointStyleAssets.DEFAULT
						: PaperAdventure.asVanilla(WaypointStyleAssets.ROOT_ID, key);
		if (Objects.equals(getHandle().waypointIcon().style, newKey)) return;

		getHandle().waypointIcon().style = newKey;
		retrack_waypoint();
	}

	@Override
	public void setWaypointColor(@Nullable Color color) {
		final Optional<Integer> newColor = Optional.ofNullable(color).map(Color::asARGB);
        if (Objects.equals(getHandle().waypointIcon().color, newColor)) {
        	return;
        }

        getHandle().waypointIcon().color = newColor;
        retrack_waypoint();
	}
	
	private void retrack_waypoint() {
        ServerWaypointManager manager = ((ServerLevel) getHandle().level()).getWaypointManager();
        manager.untrackWaypoint(getHandle());
        manager.trackWaypoint(getHandle());
    }

	@Override
	public Key getWaypointStyle() {
		return PaperAdventure.asAdventure(getHandle().waypointIcon().style.identifier());
	}

	@Override
	public Color getWaypointColor() {
		return getHandle().waypointIcon().color.map(Color::fromARGB).orElse(null);
	}

	@Override
	public void kill(org.bukkit.damage.DamageSource damageSource) {
        // Preconditions.checkState(!this.getHandle().generation, "Cannot kill entity during world generation");
        // Preconditions.checkArgument(damageSource != null, "damageSource cannot be null");

        this.getHandle().setHealth(0);
        this.getHandle().die(((CraftDamageSource) damageSource).getHandle());
    }

}
