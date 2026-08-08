/**
 * Cardboard - Bukkit/Spigot/Paper API for Fabric
 * Copyright (C) 2023-2026, CardboardPowered.org
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.cardboardpowered.util.nms;

import org.cardboardpowered.mohistremap.RemapUtilProvider;

import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Re-mapping of Reflection.
 */
public class ReflectionRemapper {

    public static final String NMS_VERSION = "v1_21_R7";
    public static JavaPlugin plugin;

    public static String mapClassName(String className) {
    	// TODO check why Essentials
    	if (className.startsWith("net.ess3.provider.providers.LegacyPotionMetaProvider")) {
    		return "net.ess3.provider.providers.ModernPotionMetaProvider";
    	}
    	
    	RemapUtils ru = (RemapUtils) RemapUtilProvider.get();

        if (className.startsWith("org.bukkit.craftbukkit." + NMS_VERSION + "."))
            return ru.map("org.bukkit.craftbukkit." + className.substring(23 + NMS_VERSION.length() + 1));

        if (className.startsWith("org.bukkit.craftbukkit.CraftServer."))
            return ru.map(className.replace("org.bukkit.craftbukkit.CraftServer.", "org.bukkit.craftbukkit."));

        if (className.startsWith("net.minecraft.server." + NMS_VERSION + "."))
            return ru.map(className.replace("net.minecraft.server." + NMS_VERSION + ".", "net.minecraft.server."));

        if (className.startsWith("net.minecraft.") && !className.startsWith("class_"))
            return ru.map(className);

        if (className.startsWith("org.bukkit.craftbukkit."))
            return ru.map(className); // We are not CraftBukkit, check for our own version of the class.

        if (className.startsWith("net.minecraft.server.CraftServer."))
            return ru.map(className.replace("net.minecraft.server.CraftServer.", "net.minecraft.server."));

        return className;
    }

    @Deprecated
    public static Field getFieldByName(Class<?> calling, String f) throws ClassNotFoundException {
        try {
            Field field = calling.getDeclaredField(f);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException | SecurityException e) {
            try {
                Field a = calling.getDeclaredField(f);
                a.setAccessible(true);
                return a;
            } catch (NoSuchFieldException | SecurityException e1) {
                if (f.contains("B_STATS_VERSION")) {
                    return getBstatsVersionField();
                }
            	e1.printStackTrace();
                return null;
            }
        }
    }

    private static int BV_CALLED = 0;
    public static Field getBstatsVersionField() {
        Field f = null;
        int i = 0;
        for (final Class<?> service : Bukkit.getServicesManager().getKnownServices()) {
            if (i < BV_CALLED) {
                i++;
                continue;
            }
            try {
                f = service.getField("B_STATS_VERSION"); // Identifies bStats classes
                break;
            } catch (final NoSuchFieldException ignored) {
            }
        }
        BV_CALLED++;
        return f;
    }

    public static CraftServer getCraftServer() {
        return CraftServer.INSTANCE;
    }

    public static MinecraftServer getNmsServer() {
        return CraftServer.server;
    }

    public static Method[] getMethods(Class<?> calling) {
        Method[] r = calling.getMethods();
        if (calling.getSimpleName().contains("MinecraftServer")) {
            Method[] nr = new Method[r.length+1];
            for (int i = 0; i < r.length; i++) {
                nr[i] = r[i];
            }
            try {
                nr[r.length] = ReflectionRemapper.class.getMethod("getNmsServer");
            } catch (NoSuchMethodException | SecurityException e) {
                e.printStackTrace();
            }
            return nr;
        }
        return r;
    }

    /**
     */
    public static String getPackageName(Package pkage) {
        String name = pkage.getName();
        if (name.startsWith("org.bukkit.craftbukkit"))
            name = name.replace("org.bukkit.craftbukkit", "org.bukkit.craftbukkit." + NMS_VERSION);
        return name;
    }

    /**
     */
    public static String getClassName(Class<?> clazz) {
        String name = clazz.getName();
        if (name.startsWith("org.bukkit.craftbukkit"))
            name = name.replace("org.bukkit.craftbukkit", "org.bukkit.craftbukkit." + NMS_VERSION);
        return name;
    }

    /**
     */
    public static String getCanonicalName(Class<?> clazz) {
        String name = clazz.getName();
        if (name.startsWith("org.bukkit.craftbukkit"))
            name = name.replace("org.bukkit.craftbukkit", "org.bukkit.craftbukkit." + NMS_VERSION);
        return name;
    }

    /**
     */
    public static String getMinecraftServerVersion() {
        return SharedConstants.getCurrentVersion().name();
    }
    
    /**
     * *
     * @param <E>
     * @param access
     * @param key
     * @return
     */
    public static <E> Registry<E> lookupOrThrow(RegistryAccess access, ResourceKey<? extends Registry<? extends E>> key) {
        return access.lookupOrThrow(key);
    }

}
