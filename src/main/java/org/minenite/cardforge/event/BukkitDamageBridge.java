package org.minenite.cardforge.event;

import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.cardboardpowered.bridge.world.entity.EntityBridge;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Fires Bukkit's EntityDamageEvent from NeoForge's damage pipeline.
 *
 * <p>Cardboard never implemented this event at all - nothing anywhere in the
 * codebase raised it - so a plugin could not observe or cancel damage. That is
 * not a small gap: protection plugins, arenas, god-mode commands and combat
 * plugins are all built on it.
 *
 * <p>It is driven from NeoForge's {@code LivingIncomingDamageEvent} rather than
 * from a Mixin on the vanilla path. NeoForge replaced damage handling with a
 * pipeline of its own - {@code LivingIncomingDamageEvent}, then
 * {@code LivingDamageEvent.Pre} and {@code Post}, around a {@code DamageContainer}
 * that tracks reductions - and injecting into the middle of it would mean
 * competing with that machinery. Listening to the event NeoForge already fires
 * for exactly this purpose costs nothing, runs for every source of damage
 * including mods, and lets a Bukkit cancel become a NeoForge cancel directly.
 *
 * <p>A plugin adjusting the damage amount is honoured too, since NeoForge's event
 * carries a mutable amount.
 */
public final class BukkitDamageBridge {

    private BukkitDamageBridge() {
    }

    public static void register(IEventBus gameBus) {
        // Explicit event class rather than relying on inference from a method
        // reference, which is the sort of thing that silently registers nothing.
        gameBus.addListener(LivingIncomingDamageEvent.class, BukkitDamageBridge::onIncomingDamage);
        org.cardboardpowered.CardboardMod.LOGGER.info(
                "EntityDamageEvent bridged to NeoForge's damage pipeline");
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        net.minecraft.world.entity.LivingEntity hurt = event.getEntity();
        if (hurt.level().isClientSide()) {
            return;
        }

        CraftEntity bukkitEntity;
        try {
            bukkitEntity = ((EntityBridge) (Object) hurt).getBukkitEntity();
        } catch (Throwable notBridged) {
            return;
        }
        if (bukkitEntity == null) {
            return;
        }

        DamageSource source = event.getSource();
        DamageCause cause = causeOf(source);
        double amount = event.getAmount();

        EntityDamageEvent bukkitEvent;
        net.minecraft.world.entity.Entity damager = source.getEntity();
        if (damager != null) {
            Entity bukkitDamager = ((EntityBridge) (Object) damager).getBukkitEntity();
            bukkitEvent = new EntityDamageByEntityEvent(bukkitDamager, bukkitEntity, cause,
                    org.bukkit.damage.DamageSource.builder(org.bukkit.damage.DamageType.GENERIC).build(), amount);
        } else {
            bukkitEvent = new EntityDamageEvent(bukkitEntity, cause,
                    org.bukkit.damage.DamageSource.builder(org.bukkit.damage.DamageType.GENERIC).build(), amount);
        }

        org.bukkit.Bukkit.getPluginManager().callEvent(bukkitEvent);

        if (bukkitEvent.isCancelled()) {
            event.setCanceled(true);
            return;
        }
        if (bukkitEvent.getDamage() != amount) {
            event.setAmount((float) bukkitEvent.getDamage());
        }
    }

    /** Best-effort mapping from a Minecraft damage type to Bukkit's cause enum. */
    private static DamageCause causeOf(DamageSource source) {
        if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE)) {
            return DamageCause.FIRE;
        }
        if (source.is(DamageTypes.LAVA)) {
            return DamageCause.LAVA;
        }
        if (source.is(DamageTypes.DROWN)) {
            return DamageCause.DROWNING;
        }
        if (source.is(DamageTypes.FALL)) {
            return DamageCause.FALL;
        }
        if (source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION)) {
            return DamageCause.ENTITY_EXPLOSION;
        }
        if (source.is(DamageTypes.MAGIC)) {
            return DamageCause.MAGIC;
        }
        if (source.is(DamageTypes.STARVE)) {
            return DamageCause.STARVATION;
        }
        if (source.is(DamageTypes.CACTUS) || source.is(DamageTypes.SWEET_BERRY_BUSH)) {
            return DamageCause.CONTACT;
        }
        if (source.is(DamageTypes.FALLING_BLOCK) || source.is(DamageTypes.FALLING_ANVIL)) {
            return DamageCause.FALLING_BLOCK;
        }
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return DamageCause.VOID;
        }
        if (source.getEntity() instanceof net.minecraft.world.entity.player.Player) {
            return DamageCause.ENTITY_ATTACK;
        }
        if (source.getEntity() != null) {
            return DamageCause.ENTITY_ATTACK;
        }
        return DamageCause.CUSTOM;
    }
}
