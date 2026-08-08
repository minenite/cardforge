package org.bukkit.craftbukkit.util;

/**
 * Utility class for converting between Minecraft and Bukkit Difficulty enums.
 */
public final class CraftDifficulty {

    /**
     * Converts a Minecraft Difficulty to a Bukkit Difficulty.
     */
    public static org.bukkit.Difficulty toBukkit(net.minecraft.world.Difficulty diff) {
        return switch (diff) {
            case EASY -> org.bukkit.Difficulty.EASY;
            case HARD -> org.bukkit.Difficulty.HARD;
            case NORMAL -> org.bukkit.Difficulty.NORMAL;
            case PEACEFUL -> org.bukkit.Difficulty.PEACEFUL;
        };
    }

    /**
     * Converts a Bukkit Difficulty to a Minecraft Difficulty.
     */
    public static net.minecraft.world.Difficulty toMinecraft(org.bukkit.Difficulty diff) {
        return switch (diff) {
            case EASY -> net.minecraft.world.Difficulty.EASY;
            case HARD -> net.minecraft.world.Difficulty.HARD;
            case NORMAL -> net.minecraft.world.Difficulty.NORMAL;
            case PEACEFUL -> net.minecraft.world.Difficulty.PEACEFUL;
        };
    }

}