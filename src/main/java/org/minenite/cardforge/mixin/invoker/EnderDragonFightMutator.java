package org.minenite.cardforge.mixin.invoker;

import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Write access to the "has been killed once" flag, which decides whether the egg
 * and the end gateways appear on the next kill.
 */
@Mixin(EnderDragonFight.class)
public interface EnderDragonFightMutator {

    @Accessor("hasPreviouslyKilledDragon")
    void cardforge$setHasPreviouslyKilledDragon(boolean value);
}
