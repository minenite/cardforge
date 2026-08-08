package me.isaiah.common.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.isaiah.common.cmixin.MixinInfo;
import me.isaiah.common.event.EventRegistery;
import me.isaiah.common.event.entity.EntityPortalCollideEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;

@MixinInfo(minVersion = "1.18.2", maxVersion = "1.21.4")
@Mixin(NetherPortalBlock.class)
public class MixinNetherPortalBlock {

	/**
	 * TODO: 1.21.5+ adds extra EntityCollisionHandler after Entity
	 */
	@Inject(at = @At("HEAD"), method = "entityInside", cancellable = true)
	private void onEntityCollision( BlockState state, Level world, BlockPos pos, Entity entity, CallbackInfo ci) {
		if (!(entity instanceof ServerPlayer)) {
			return;
		}

		EntityPortalCollideEvent ev = (EntityPortalCollideEvent)
                EventRegistery.invoke(EntityPortalCollideEvent.class, new EntityPortalCollideEvent(state, world, pos, entity));

        if (ev.isCanceled()) {
            ci.cancel();
            return;
        }
	}

}