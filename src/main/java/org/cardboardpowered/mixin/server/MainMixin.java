package org.cardboardpowered.mixin.server;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.cardboardpowered.CardboardLogger;
import org.cardboardpowered.CardboardMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;
import com.llamalad7.mixinextras.sugar.Local;

import io.papermc.paper.plugin.PluginInitializerManager;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.minecraft.SharedConstants;
import net.minecraft.server.Main;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;

import static java.util.Arrays.asList;
import joptsimple.util.PathConverter;

/**
 * Mixin of {@link net.minecraft.server.Main}
 * 
 * @implSpec https://github.com/PaperMC/Paper/blob/main/paper-server/patches/sources/net/minecraft/server/Main.java.patch
 */
@Mixin(value = Main.class)
public class MainMixin {

	@Inject(method = "main", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/repository/ServerPacksSource;createPackRepository(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;)Lnet/minecraft/server/packs/repository/PackRepository;"))
    private static void cardboard$create_bukkit_datapack(String[] strings, CallbackInfo ci, @Local LevelStorageSource.LevelStorageAccess levelStorageAccess) {

		// Paper start - Create Bukkit Datapack
		
		File bukkitDataPackFolder = new File(levelStorageAccess.getLevelPath(LevelResource.DATAPACK_DIR).toFile(), "bukkit");
        if (!bukkitDataPackFolder.exists()) {
           bukkitDataPackFolder.mkdirs();
        }
		
        File mcMeta = new File(bukkitDataPackFolder, "pack.mcmeta");

        try {
           int major = SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA).major();
           int minor = SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA).minor();
           Files.asCharSink(mcMeta, StandardCharsets.UTF_8, new FileWriteMode[0])
              .write(
                 "{\n    \"pack\": {\n        \"description\": \"Data pack for resources provided by Bukkit plugins\",\n        \"min_format\": [%d, %d],\n        \"max_format\": [%d, %d]\n    }\n}\n"
                    .formatted(major, minor, major, minor)
              );
        } catch (IOException err) {
           throw new RuntimeException("Could not initialize Bukkit datapack", err);
        }
        // Paper end - Create Bukkit Datapack
    }
	
	@Inject(
	        method = "main",
	        at = @At(
	            value = "INVOKE",
	            target = "Lnet/minecraft/server/Bootstrap;bootStrap()V",
	            shift = At.Shift.BEFORE
	        ),
	        remap = false
	    )
	    private static void insertPluginInitializerLoad(String[] strings, CallbackInfo ci) {
			org.cardboardpowered.BukkitLogger.getLogger().info("Loading Paper PluginInitializerManager..");
	        
	        try {
	        	 OptionParser parser = new OptionParser() {
	                 {
	                     this.acceptsAll(asList("?", "help"), "Show the help");

	                     this.acceptsAll(asList("c", "config"), "Properties file to use")
	                             .withRequiredArg()
	                             .ofType(File.class)
	                             .defaultsTo(new File("server.properties"))
	                             .describedAs("Properties file");

	                     this.acceptsAll(asList("P", "plugins"), "Plugin directory to use")
	                             .withRequiredArg()
	                             .ofType(File.class)
	                             .defaultsTo(new File("plugins"))
	                             .describedAs("Plugin directory");

	                     this.acceptsAll(asList("h", "host", "server-ip"), "Host to listen on")
	                             .withRequiredArg()
	                             .ofType(String.class)
	                             .describedAs("Hostname or IP");

	                     this.acceptsAll(asList("W", "world-dir", "universe", "world-container"), "World container")
	                             .withRequiredArg()
	                             .ofType(File.class)
	                             .defaultsTo(new File("."))
	                             .describedAs("Directory containing worlds");

	                     this.acceptsAll(asList("w", "world", "level-name"), "World name")
	                             .withRequiredArg()
	                             .ofType(String.class)
	                             .describedAs("World name");

	                     this.acceptsAll(asList("p", "port", "server-port"), "Port to listen on")
	                             .withRequiredArg()
	                             .ofType(Integer.class)
	                             .describedAs("Port");

	                     this.accepts("serverId", "Server ID")
	                             .withRequiredArg();

	                     this.accepts("jfrProfile", "Enable JFR profiling");

	                     this.accepts("pidFile", "pid File")
	                             .withRequiredArg()
	                             .withValuesConvertedBy(new PathConverter());

	                     this.acceptsAll(asList("o", "online-mode"), "Whether to use online authentication")
	                             .withRequiredArg()
	                             .ofType(Boolean.class)
	                             .describedAs("Authentication");

	                     this.acceptsAll(asList("s", "size", "max-players"), "Maximum amount of players")
	                             .withRequiredArg()
	                             .ofType(Integer.class)
	                             .describedAs("Server size");

	                     this.acceptsAll(asList("b", "bukkit-settings"), "File for bukkit settings")
	                             .withRequiredArg()
	                             .ofType(File.class)
	                             .defaultsTo(new File("bukkit.yml"))
	                             .describedAs("Yml file");

	                     this.acceptsAll(asList("C", "commands-settings"), "File for command settings")
	                             .withRequiredArg()
	                             .ofType(File.class)
	                             .defaultsTo(new File("commands.yml"))
	                             .describedAs("Yml file");

	                     this.accepts("forceUpgrade", "Whether to force a world upgrade");
	                     this.accepts("eraseCache", "Whether to force cache erase during world upgrade");
	                     this.accepts("recreateRegionFiles", "Whether to recreate region files during world upgrade");
	                     this.accepts("safeMode", "Loads level with vanilla datapack only"); // Paper
	                     this.accepts("nogui", "Disables the graphical console");

	                     this.accepts("nojline", "Disables jline and emulates the vanilla console");

	                     this.accepts("noconsole", "Disables the console");

	                     this.acceptsAll(asList("v", "version"), "Show the CraftBukkit Version");

	                     this.accepts("demo", "Demo mode");

	                     this.accepts("bonusChest", "Enable the bonus chest");

	                     this.accepts("initSettings", "Only create configuration files and then exit"); // SPIGOT-5761: Add initSettings option

	                     this.acceptsAll(asList("S", "spigot-settings"), "File for spigot settings")
	                             .withRequiredArg()
	                             .ofType(File.class)
	                             .defaultsTo(new File("spigot.yml"))
	                             .describedAs("Yml file");

	                     this.acceptsAll(asList("paper-dir", "paper-settings-directory"), "Directory for Paper settings")
	                             .withRequiredArg()
	                             .ofType(File.class)
	                             .defaultsTo(new File(/*io.papermc.paper.configuration.PaperConfigurations.CONFIG_DIR*/ "config"))
	                             .describedAs("Config directory");

	                     this.acceptsAll(asList("paper", "paper-settings"), "File for Paper settings")
	                             .withRequiredArg()
	                             .ofType(File.class)
	                             .defaultsTo(new File("paper.yml"))
	                             .describedAs("Yml file");

	                     this.acceptsAll(asList("add-plugin", "add-extra-plugin-jar"), "Specify paths to extra plugin jars to be loaded in addition to those in the plugins folder. This argument can be specified multiple times, once for each extra plugin jar path.")
	                             .withRequiredArg()
	                             .ofType(File.class)
	                             .defaultsTo(new File[] {})
	                             .describedAs("Jar file");

	                     this.acceptsAll(asList("add-plugin-dir", "add-extra-plugin-dir"), "Specify paths to extra plugin directories to be loaded in addition to the plugins folder. This argument can be specified multiple times, once for each extra plugin dir path.")
	                             .withRequiredArg()
	                             .ofType(File.class)
	                             .defaultsTo(new File[] {})
	                             .describedAs("Plugin directory");

	                     this.accepts("server-name", "Name of the server")
	                             .withRequiredArg()
	                             .ofType(String.class)
	                             .defaultsTo("Unknown Server")
	                             .describedAs("Name");
	                 }
	             };
	        	OptionSet options = parser.parse(strings);
	        	CardboardMod.options = options;
				// PluginInitializerManager.load(options);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }

	
}
