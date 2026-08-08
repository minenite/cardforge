package org.cardboardpowered.mixin.world.item;

import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;

/**
 * Fires Bukkit's PlayerShearEntityEvent on NeoForge.
 *
 * <p>Vanilla implements shearing per entity, inside each mob's
 * {@code mobInteract}, and Cardboard hooks each of those individually. NeoForge
 * neutralises those branches outright:
 *
 * <pre>if (false &amp;&amp; itemStack.is(Items.SHEARS)) // Neo: handled by IShearable</pre>
 *
 * and migrates the whole subsystem into {@link ShearsItem#interactLivingEntity},
 * which checks {@code IShearable#isShearable} and calls {@code IShearable#onSheared}.
 * The per-entity injections therefore have no target and cannot simply be
 * retargeted - the hook has to move to the replacement subsystem.
 *
 * <p>Doing so is also strictly better for a compatibility layer: a single hook
 * covers every shearable entity, including ones added by NeoForge mods, so a
 * plugin can cancel shearing a modded mob exactly as it would a vanilla sheep.
 */
@Mixin(ShearsItem.class)
public class ShearsItemMixin {

    @Inject(
            method = "interactLivingEntity(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;"
                    + "Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;)"
                    + "Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"),
            cancellable = true)
    private void cardforge$playerShearEntityEvent(ItemStack stack, Player player, LivingEntity entity,
                                                  InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        // Only server-side, and only for things NeoForge would actually shear.
        if (entity.level().isClientSide()) {
            return;
        }
        if (!(entity instanceof net.neoforged.neoforge.common.IShearable shearable)) {
            return;
        }
        if (!shearable.isShearable(player, stack, entity.level(), entity.blockPosition())) {
            return;
        }

        if (!CraftEventFactory.handlePlayerShearEntityEvent(player, entity, stack, hand)) {
            // Matches the vanilla-path behaviour Cardboard used: refuse the interaction
            // without consuming the item or playing effects.
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
