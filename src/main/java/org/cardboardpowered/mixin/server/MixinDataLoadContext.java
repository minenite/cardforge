package org.cardboardpowered.mixin.server;

import org.cardboardpowered.CardboardLoadHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.WorldLoader.DataLoadContext;

@Mixin(DataLoadContext.class)
public abstract class MixinDataLoadContext {

	private static Logger cb$LOGGER = LoggerFactory.getLogger("Cardboard|Preload");
	
	/**
	 * Cardboard - Capture the instance of DataLoadContext when it's created and store it in CardboardLoadHolder for later use.
	 * This is needed because DataLoadContext is created during world loading and we need to access it for our custom world loading logic.
	 * 
	 * @implNote Paper changes the constructor of MinecraftServer to take the extra parameter of DataLoadContext.
	 * @author Cardboard
	 */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void afterInit(CallbackInfo ci) {
    	
    	cb$LOGGER.info("Captured instance of DataLoadContext");

    	if (null != CardboardLoadHolder.worldLoader.get()) {
    		cb$LOGGER.warn("Overwriting existing DataLoadContext instance in CardboardLoadHolder! Previous instance: " + CardboardLoadHolder.worldLoader.get());
    	}
    	
        CardboardLoadHolder.worldLoader.set( (DataLoadContext)(Object)this );
        
    }
}

