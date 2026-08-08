package org.cardboardpowered.mixin.world.level.storage;

import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.PrimaryLevelData;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.cardboardpowered.bridge.level.ILevelSettings;
import org.cardboardpowered.bridge.world.level.storage.PrimaryLevelDataBridge;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// @MixinInfo(events = {"ThunderChangeEvent","WeatherChangeEvent"})
@Mixin(PrimaryLevelData.class)
public class PrimaryLevelDataMixin implements PrimaryLevelDataBridge {

    @Shadow
    private LevelSettings settings;

    // moved to PrimaryLevelDataBridge
    // private static final String PAPER_RESPAWN_DIMENSION = "paperSpawnDimension"; // Paper

    @Unique
    public net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> respawnDimension = net.minecraft.world.level.Level.OVERWORLD; // Paper

    // TODO: 26.1: Reimplement WeatherChangeEvent and ThunderChangeEvent injections
    /*
    @Inject(at = @At("HEAD"), method = "setThundering")
    public void thunder(boolean flag, CallbackInfo info) {
        PrimaryLevelData p = (PrimaryLevelData)(Object) this;
        if (p.isThundering() == flag)
            return;
        World world = Bukkit.getWorld(p.getLevelName());
        if (world != null) {
            ThunderChangeEvent thunder = new ThunderChangeEvent(world, flag);
            Bukkit.getServer().getPluginManager().callEvent(thunder);
            if (thunder.isCancelled())
                return;
        }
    }

    @Inject(at = @At("HEAD"), method = "setRaining")
    public void rain(boolean flag, CallbackInfo info) {
        PrimaryLevelData p = (PrimaryLevelData)(Object) this;
        if (p.isRaining() == flag)
            return;
        World world = Bukkit.getWorld(p.getLevelName());
        if (world != null) {
            WeatherChangeEvent event = new WeatherChangeEvent(world, flag);
            Bukkit.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled())
                return;
        }
    }
    */

    @Override
    public void checkName(String name) {
    	if (!this.settings.levelName().equals(name)) {
            this.settings = ((ILevelSettings) this.settings).cardboard$withLevelName(name);
        }
    }

    @Override
    public ResourceKey<Level> cardboard$getRespawnDimension() {
        return respawnDimension;
    }

    @Override
    public void cardboard$setRespawnDimension(ResourceKey<Level> respawnDimension) {
        this.respawnDimension = respawnDimension;
    }

    @Inject(method = "parse", at = @At("RETURN"))
    private static <T> void parsePaper(Dynamic<T> dynamic, LevelSettings levelSettings, PrimaryLevelData.SpecialWorldProperty specialWorldProperty, Lifecycle lifecycle, CallbackInfoReturnable<PrimaryLevelData> cir) {
        ((PrimaryLevelDataBridge)cir.getReturnValue()).cardboard$setRespawnDimension(dynamic.get(PrimaryLevelDataBridge.PAPER_RESPAWN_DIMENSION)
                .read(net.minecraft.world.level.Level.RESOURCE_KEY_CODEC)
                .result()
                .orElse(cir.getReturnValue().getRespawnData().dimension()));
    }

    @Inject(method = "setTagData", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;store(Ljava/lang/String;Lcom/mojang/serialization/Codec;Ljava/lang/Object;)V", ordinal = 0, shift = At.Shift.AFTER))
    private void setTagDataPaper(CompoundTag tag, UUID uuid, CallbackInfo ci) {
        tag.store(PrimaryLevelDataBridge.PAPER_RESPAWN_DIMENSION, net.minecraft.world.level.Level.RESOURCE_KEY_CODEC, this.respawnDimension); // Paper
    }
}