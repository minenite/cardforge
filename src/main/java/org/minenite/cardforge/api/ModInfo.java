package org.minenite.cardforge.api;

/**
 * A NeoForge mod loaded on this server.
 *
 * @param id          the mod id, as it appears in namespaced keys
 * @param displayName the human-readable name
 * @param version     the mod version
 */
public record ModInfo(String id, String displayName, String version) {
}
