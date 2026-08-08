package org.minenite.cardforge;

import org.minenite.cardforge.platform.NeoForgePlatform;
import org.minenite.cardforge.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Mod entrypoint.
 *
 * Its only job is to install the platform adapter early, so the Bukkit layer
 * ported from Cardboard never has to know which loader it is running under.
 */
@Mod(Cardforge.MOD_ID)
public final class Cardforge {

    public static final String MOD_ID = "cardforge";

    private static final Logger LOGGER = LoggerFactory.getLogger("Cardforge");

    public Cardforge(IEventBus modBus) {
        Platform.set(new NeoForgePlatform());

        // Bukkit's EntityDamageEvent is driven from NeoForge's damage pipeline
        // rather than from a Mixin; see BukkitDamageBridge.
        org.minenite.cardforge.event.BukkitDamageBridge.register(
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS);
        LOGGER.info("Cardforge on {} {} (Minecraft {})",
                Platform.get().platformName(),
                Platform.get().platformVersion(),
                Platform.get().modVersion("minecraft").orElse("unknown"));
    }
}
