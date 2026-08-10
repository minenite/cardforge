package org.cardboardpowered.bridge.network;

import com.mojang.authlib.GameProfile;

/**
 * Lets the forwarding path fire Bukkit's login events for a profile it verified.
 *
 * <p>Those events live on the inherited Spigot login path, which runs them on a
 * thread started from {@code handleHello} using whatever profile exists at that
 * moment. Under proxy forwarding no profile exists yet - it arrives a few
 * milliseconds later, from the proxy - so the events have to be fired by the
 * code that receives it, and this is how it reaches them.
 */
public interface LoginEventsBridge {

    /**
     * Fires {@code AsyncPlayerPreLoginEvent} and {@code PlayerPreLoginEvent}.
     *
     * <p>Blocks: the synchronous event is handed to the server thread and waited
     * on, so this must not be called from the server thread itself.
     */
    void cardboard$fireLoginEvents(GameProfile profile);
}
