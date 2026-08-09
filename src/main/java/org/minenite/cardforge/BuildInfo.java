package org.minenite.cardforge;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The build's own identity, stamped in by Gradle's {@code buildInfo} task.
 *
 * <p>Cardboard derived this from a {@code org.cardboardpowered.GitVersion} class
 * that nothing in this project generates, so every lookup fell through to the
 * literal {@code "-unknown-"} and {@code /version} reported the server as running
 * "version unknown". Reading a resource keeps the failure mode honest: if the
 * properties file is missing the values say so, rather than a hash-shaped string
 * that is not a hash.
 */
public final class BuildInfo {

    private static final String RESOURCE = "/cardforge-build.properties";

    private static final Properties PROPERTIES = load();

    public static final String VERSION = get("version", "unknown");
    public static final String COMMIT = get("commit", "unknown");
    public static final String BRANCH = get("branch", "unknown");
    public static final boolean DIRTY = Boolean.parseBoolean(get("dirty", "false"));
    public static final String BUILD_TIME = get("buildTime", "unknown");
    public static final String MINECRAFT = get("minecraft", "unknown");
    public static final String NEOFORGE = get("neoforge", "unknown");

    private BuildInfo() {
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream in = BuildInfo.class.getResourceAsStream(RESOURCE)) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException ignored) {
            // An unreadable stamp is not worth failing a boot over; the defaults
            // below already describe the situation accurately.
        }
        return properties;
    }

    private static String get(String key, String fallback) {
        String value = PROPERTIES.getProperty(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /** Short commit hash, or {@code "unknown"} when the build was not made from a checkout. */
    public static String shortCommit() {
        return COMMIT.length() >= 7 ? COMMIT.substring(0, 7) : COMMIT;
    }

    /** True when this build carries no usable git identity. */
    public static boolean isUnknownBuild() {
        return "unknown".equals(COMMIT);
    }

    /**
     * The version as a human reads it: {@code 26.2-1c60f99}, with a {@code -dirty}
     * suffix when the tree had uncommitted changes at build time. That suffix is
     * the useful part - it is the difference between a build someone else can
     * reproduce from the commit and one only this machine has ever had.
     */
    public static String versionString() {
        if (isUnknownBuild()) {
            return VERSION;
        }
        return VERSION + "-" + shortCommit() + (DIRTY ? "-dirty" : "");
    }
}
