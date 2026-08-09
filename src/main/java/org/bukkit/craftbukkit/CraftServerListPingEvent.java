package org.bukkit.craftbukkit;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.server.ServerListPingEvent;

/**
 * A pingable event that can actually be iterated.
 *
 * <p>Bukkit's {@link ServerListPingEvent} throws {@code UnsupportedOperationException}
 * from {@code iterator()} by design - the base class has no player sample, and
 * the server implementation is expected to subclass it and supply one. CardForge
 * was constructing the base class directly, so any plugin walking the sample hit
 * that exception. EssentialsX does it on every server-list ping, to hide vanished
 * players, and logged a warning each time.
 *
 * <p>The iterator is backed by a mutable list of the online players and supports
 * removal, which is what plugins use it for.
 *
 * <p>Known limit: removing a player here does not yet change what the server
 * actually sends back, because CardForge does not populate a player sample in
 * the ping response. The exception is gone and iteration behaves, but hiding a
 * player has no visible effect on the server list yet.
 */
public class CraftServerListPingEvent extends ServerListPingEvent {

    private final List<Player> sample;

    public CraftServerListPingEvent(InetAddress address, String motd, int numPlayers, int maxPlayers) {
        super("", address, motd, numPlayers, maxPlayers);
        this.sample = new ArrayList<>(Bukkit.getOnlinePlayers());
    }

    @Override
    public Iterator<Player> iterator() {
        return this.sample.iterator();
    }
}
