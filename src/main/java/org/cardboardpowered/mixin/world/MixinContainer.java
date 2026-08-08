package org.cardboardpowered.mixin.world;

import org.cardboardpowered.bridge.world.ContainerBridge;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.Container;

@Mixin(Container.class)
public interface MixinContainer extends ContainerBridge {

}
