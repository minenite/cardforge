package org.minenite.cardforge.proxy;

/**
 * Diagnostics that survive a broken logger.
 *
 * <p>This server has a log4j appender that throws on write, and Minecraft routes
 * System.out through log4j, so both channels silently discard messages from the
 * network threads. Worse, the failing appender truncates debug.log at arbitrary
 * points, which reads exactly like "the connection died here" and produced
 * several wrong conclusions before it was noticed.
 *
 * <p>Writing directly to a file is the only channel that cannot be swallowed.
 * Enabled with -Dcardforge.proxy.trace=true so it costs nothing normally.
 */
public final class ProxyTrace {

    private static final boolean ENABLED = Boolean.getBoolean("cardforge.proxy.trace");
    private static final java.nio.file.Path FILE = java.nio.file.Path.of("cardforge-proxy.log");

    private ProxyTrace() {
    }

    public static void log(String message) {
        if (!ENABLED) {
            return;
        }
        try {
            java.nio.file.Files.writeString(FILE,
                    java.time.LocalTime.now() + " [" + Thread.currentThread().getName() + "] " + message
                            + System.lineSeparator(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Throwable ignored) {
        }
    }
}
