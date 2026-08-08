package me.isaiah.common.mixin;

import static me.isaiah.common.ICommonMod.LOGGER;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.isaiah.common.ICommonMod;
import me.isaiah.common.fabric.FabricServer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;

@Mixin(IntegratedServer.class)
public class MixinIntegratedServer {

    @Inject(at = @At("HEAD"), method = "initServer()Z")
    void onServerStart(CallbackInfoReturnable<Boolean> callbackInfo) {
        LOGGER.info("Setting IServer instance..");
        ICommonMod.set( new FabricServer((MinecraftServer)(Object)this) );
    }

}
