package org.minenite.cardforge;

import static java.util.Arrays.asList;

import java.io.File;

import joptsimple.OptionParser;
import joptsimple.util.PathConverter;

/**
 * The server command-line options Cardboard parses.
 *
 * This lived as an anonymous {@code new OptionParser() { ... }} inside
 * MainMixin. Mixin merges any class nested in a mixin into the target under a
 * synthesised name, and NeoForge's module class loader cannot resolve those, so
 * it has to be a top-level class outside the mixin package.
 */
public final class CardforgeOptionParser extends OptionParser {

    public CardforgeOptionParser() {
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
    }
}
