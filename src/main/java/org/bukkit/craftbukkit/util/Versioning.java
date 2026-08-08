package org.bukkit.craftbukkit.util;

import org.bukkit.Bukkit;
import org.cardboardpowered.impl.CardboardAbstractServer;

public final class Versioning {

    private static final String API_VERSION = CardboardAbstractServer.API_VERSION;

    public static String getBukkitVersion() {
        return Bukkit.getVersion();
    }

    public static String getCurrentApiVersion() {
        return API_VERSION;
    }

}