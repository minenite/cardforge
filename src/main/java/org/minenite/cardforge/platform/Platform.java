package org.minenite.cardforge.platform;

import java.util.Objects;

/**
 * Holder for the active {@link PlatformAdapter}.
 *
 * Cardboard's library loader and mixin plugin both run before the @Mod
 * entrypoint, so this self-initialises on first use rather than requiring a
 * particular startup order. The entrypoint may still call {@link #set} to
 * install a different adapter.
 */
public final class Platform {

    private static volatile PlatformAdapter adapter;

    private Platform() {
    }

    public static void set(PlatformAdapter platformAdapter) {
        adapter = Objects.requireNonNull(platformAdapter, "platformAdapter");
    }

    public static PlatformAdapter get() {
        PlatformAdapter current = adapter;
        if (current == null) {
            synchronized (Platform.class) {
                current = adapter;
                if (current == null) {
                    current = new NeoForgePlatform();
                    adapter = current;
                }
            }
        }
        return current;
    }

    public static boolean isInitialised() {
        return adapter != null;
    }
}
