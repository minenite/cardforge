package org.cardboardpowered.mixin.server;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.cardboardpowered.asm.TransformAccess;
import org.cardboardpowered.bridge.IMinecraftServerStatic;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.PlayerDataStorage;

@Mixin(value=MinecraftServer.class)
public class MCServerMixin implements IMinecraftServerStatic {

    // TODO: 1.18.2 @Shadow @Final public DynamicRegistryManager.Impl registryManager;
    @Shadow @Final public PlayerDataStorage playerDataStorage;
    
    /*
    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    public static MinecraftServer getServer() {
        return Bukkit.getServer() instanceof CraftServer ? ((CraftServer) Bukkit.getServer()).getServer() : null;
    }
    */
    
}