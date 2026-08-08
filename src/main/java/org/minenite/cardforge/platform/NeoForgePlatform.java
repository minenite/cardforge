package org.minenite.cardforge.platform;

import java.nio.file.Path;
import java.util.Optional;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;

/**
 * NeoForge implementation of {@link PlatformAdapter}.
 *
 * This is the only class in Cardforge that is allowed to import
 * {@code net.neoforged.*}; everything else goes through the interface.
 */
public final class NeoForgePlatform implements PlatformAdapter {

    @Override
    public Path gameDirectory() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public Path configDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get() != null && ModList.get().isLoaded(modId);
    }

    @Override
    public Optional<String> modVersion(String modId) {
        if (ModList.get() == null) {
            return Optional.empty();
        }
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString());
    }

    @Override
    public String platformName() {
        return "NeoForge";
    }

    @Override
    public String platformVersion() {
        return modVersion("neoforge").orElse("unknown");
    }

    @Override
    public boolean isDedicatedServer() {
        return FMLEnvironment.getDist().isDedicatedServer();
    }
}
