package org.minenite.cardforge.mixin.invoker;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * A barrel keeps its backing list private, unlike the other containers whose
 * lists the container bridge can shadow directly.
 */
@Mixin(BarrelBlockEntity.class)
public interface BarrelBlockEntityAccessor {

    @Accessor("items")
    NonNullList<ItemStack> cardforge$getItems();
}
