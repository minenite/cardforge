package org.cardboardpowered.mixin.world.entity.player;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.cardboardpowered.bridge.world.entity.player.PlayerBridge;
import org.cardboardpowered.bridge.world.entity.EntityBridge;
import org.cardboardpowered.bridge.world.entity.LivingEntityBridge;
import org.cardboardpowered.mixin.world.entity.LivingEntityMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntityMixin implements EntityBridge, LivingEntityBridge, PlayerBridge {

    @org.spongepowered.asm.mixin.Shadow
    protected float hurtDir;

    @Override
    public void cardboard$setHurtDir(float hurtDir) {
        this.hurtDir = hurtDir;
    }
    @Shadow
    public abstract Inventory getInventory();

    @Shadow
    public AbstractContainerMenu containerMenu;

    @Override
    public org.bukkit.craftbukkit.entity.CraftHumanEntity getBukkitEntity() {
        return (org.bukkit.craftbukkit.entity.CraftHumanEntity) super.getBukkitEntity();
    }

    /**
     * Applies the player's flying fall damage setting.
     *
     * <p>Vanilla spares a player who was flying; the API lets a plugin say
     * otherwise, and there was nowhere for that answer to be asked. Without this
     * the setter stored a value nothing read, which is worse than refusing.
     */
    @org.spongepowered.asm.mixin.injection.Inject(method = "causeFallDamage", at = @org.spongepowered.asm.mixin.injection.At("HEAD"), cancellable = true)
    private void cardboard$flyingFallDamage(double fallDistance, float multiplier,
                                            net.minecraft.world.damagesource.DamageSource source,
                                            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        org.bukkit.craftbukkit.entity.CraftHumanEntity bukkit = this.getBukkitEntity();
        if (!(bukkit instanceof org.bukkit.craftbukkit.entity.CraftPlayer player)) {
            return;
        }
        net.kyori.adventure.util.TriState setting = player.hasFlyingFallDamage();
        if (setting == net.kyori.adventure.util.TriState.NOT_SET) {
            return;
        }
        if (setting == net.kyori.adventure.util.TriState.FALSE && player.isFlying()) {
            cir.setReturnValue(false);
        }
    }
}
