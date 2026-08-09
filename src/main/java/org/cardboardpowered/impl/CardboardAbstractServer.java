package org.cardboardpowered.impl;

import java.io.File;
import java.util.logging.Level;

import org.cardboardpowered.impl.command.VersionCommand;
import org.cardboardpowered.impl.util.CardboardCachedServerIcon;

import net.minecraft.SharedConstants;
import net.minecraft.server.dedicated.DedicatedServer;

public abstract class CardboardAbstractServer implements org.bukkit.Server {

	public static final String API_VERSION = "26.2";

	public final String serverName = "CardForge";
	
	public final String serverVersion;
    public final String shortVersion;
    
    public CardboardCachedServerIcon icon;
    public static DedicatedServer server;
	
    public CardboardAbstractServer(DedicatedServer dserver) {
    	server = dserver;
    	// Cardboard read this from a generated GitVersion class that does not exist
    	// here, so the hash was always the literal "-unknow". BuildInfo is stamped
    	// in by the build and degrades to a stated "unknown" rather than a fake hash.
    	serverVersion = "git-CardForge-" + org.minenite.cardforge.BuildInfo.shortCommit();
        shortVersion = org.minenite.cardforge.BuildInfo.versionString();
	}

	@Override
    public String toString() {
        return "CraftServer{" + "serverName=" + serverName + ",serverVersion=" + serverVersion + ",minecraftVersion=" + SharedConstants.getCurrentVersion().name() + '}';
    }
	
    @Override
    public String getName() {
        return serverName;
    }
    
    public String getShortVersion() {
        return org.minenite.cardforge.BuildInfo.versionString() + " (MC: " + server.getServerVersion() + ")";
    }
    
    public void loadIcon() {
        icon = new CardboardCachedServerIcon(null);
        try {
            final File file = new File(new File("."), "server-icon.png");
            if (file.isFile()) {
                icon = CardboardCachedServerIcon.createFromFile(file);
            }
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, "Couldn't load server icon", ex);
        }
    }

    @Override
    public CardboardCachedServerIcon getServerIcon() {
        return icon;
    }
    
}
