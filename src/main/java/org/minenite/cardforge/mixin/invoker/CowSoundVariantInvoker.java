package org.minenite.cardforge.mixin.invoker;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.animal.cow.CowSoundVariant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * The sound variant accessors are private on the entity, so the API has no way
 * to read or change which set of noises this animal makes.
 */
@Mixin(net.minecraft.world.entity.animal.cow.Cow.class)
public interface CowSoundVariantInvoker {

    @Invoker("getSoundVariant")
    Holder<CowSoundVariant> cardforge$getSoundVariant();

    @Invoker("setSoundVariant")
    void cardforge$setSoundVariant(Holder<CowSoundVariant> variant);
}
