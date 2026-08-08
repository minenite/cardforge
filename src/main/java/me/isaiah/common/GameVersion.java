package me.isaiah.common;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public class GameVersion {
    
    public static GameVersion INSTANCE;

    private final int protocolVersion;
    private final String releaseTarget;

    public GameVersion(JsonObject jsonObject) {
		String relTarget = "";
        try {
			relTarget = GsonHelper.getAsString(jsonObject, "release_target");
		} catch (Exception e) {
			// Why would mojang decide to break gameversion again?
			relTarget = GsonHelper.getAsString(jsonObject, "id");
		}
		this.releaseTarget = relTarget;
        this.protocolVersion = GsonHelper.getAsInt(jsonObject, "protocol_version");
    }

    /**
     */
    public String getReleaseTarget() {
        return releaseTarget;
    }

    /**
     */
    public int getProtocolVersion() {
        return protocolVersion;
    }
	
	/**
	 */
	public boolean is1194_or_higher() {
		return this.protocolVersion >= 762;
	}

}
