package org.cardboardpowered.mixin.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;


@Mixin(value = Bukkit.class, remap = false)
public class BukkitMixin {

	/**
	 * Use Fabric's ModMetadata for version
	 * info instead of grabbing from META-INF
	 * 
	 * @author cardboard
	 * @reason META-INF
	 */
	@Overwrite(remap = false)
    public static String getVersionMessage() {
		// One source of truth with /version: the build stamp, not a registry lookup.
		String ver = CraftServer.INSTANCE.getShortVersion();

		return "This server is running " + Bukkit.getName() + " version " + ver + " (Implementing API version " + Bukkit.getBukkitVersion() + ")";
    }
	
}
