package org.minenite.cardforge.platform;

import java.nio.file.Path;
import java.util.Optional;

/**
 * The seam between Cardboard's Bukkit implementation and the mod loader.
 *
 * Cardboard currently reaches for {@code net.fabricmc.loader.api.FabricLoader}
 * in 19 files. Everything it actually needs from a loader is listed here, so
 * the Bukkit layer can be compiled against this interface and the loader
 * dependency lives in exactly one implementation class per platform.
 */
public interface PlatformAdapter {

    /** Directory the server was launched from. */
    Path gameDirectory();

    /** Directory mods/plugins may write configuration into. */
    Path configDirectory();

    /** True when a mod with this id is present. */
    boolean isModLoaded(String modId);

    /** Human-readable version of a loaded mod, if present. */
    Optional<String> modVersion(String modId);

    /** Name of the platform, e.g. "NeoForge" or "Fabric". */
    String platformName();

    /** Version of the platform itself. */
    String platformVersion();

    /** True when running a dedicated (non-integrated) server. */
    boolean isDedicatedServer();

    /** True in a development//deobfuscated environment. */
    boolean isDevelopmentEnvironment();

    /** Ids of every loaded mod. */
    java.util.Collection<String> loadedModIds();

    /** Display name of a loaded mod, if present. */
    Optional<String> modName(String modId);
}
