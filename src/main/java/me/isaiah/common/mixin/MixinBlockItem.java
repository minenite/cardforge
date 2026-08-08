package me.isaiah.common.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.isaiah.common.event.EventRegistery;
import me.isaiah.common.event.block.BlockItemPlaceEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;

@Mixin(BlockItem.class)
public class MixinBlockItem {

    /**
     * {@link BlockItemPlaceEvent}
     */
    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/context/BlockPlaceContext;getClickedPos()Lnet/minecraft/core/BlockPos;"),
            method = "place",cancellable = true)
    public void icommon_doBlockItemPlaceEvent(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> info) {

        BlockItemPlaceEvent e = (BlockItemPlaceEvent) EventRegistery.invoke(BlockItemPlaceEvent.class, 
                new BlockItemPlaceEvent(context));
        if (e.isCanceled()) {
            info.setReturnValue(InteractionResult.SUCCESS);
            info.cancel();
            return;
        }
    }

}