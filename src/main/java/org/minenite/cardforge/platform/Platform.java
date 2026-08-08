package org.minenite.cardforge.platform;

import java.util.Objects;

/** Holder for the active {@link PlatformAdapter}. */
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
            throw new IllegalStateException(
                    "Platform adapter not initialised; the mod entrypoint must call Platform.set() first");
        }
        return current;
    }

    public static boolean isInitialised() {
        return adapter != null;
    }
}
