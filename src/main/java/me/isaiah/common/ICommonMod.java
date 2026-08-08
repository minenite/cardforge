package me.isaiah.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.world.level.LevelAccessor;

/**
 * iCommon Mod
 */
public class ICommonMod {

	/** 
	 * Change API version for new or breaking changes
	 * Dependents can use this as a compatibility check
	 * 
	 * Details:
	 * 0.6: Add getIWorld
	 * 0.4: Add World API for isTheEnd & getSpawnPoint, add checkVersion
     */
    private static final double API_VERSION = 0.6;

    public enum SupportStatus {
    	REQUIRE("requires", "REQUIRED"),
    	SUGGEST("suggests", "SUGGESTED");
    	
    	private String val1;
    	private String val2;
    	
    	SupportStatus(String val, String val2) {
    		this.val1 = val;
    		this.val2 = val2;
    	}
    }
    
    public static boolean checkVersion(double req, boolean msg, String modName, SupportStatus stat) {
    	boolean isLower = req < API_VERSION;
    	
    	if (msg) {
    		LOGGER.warn("==== UPDATE " + stat.val2 + " ==== ");
    		LOGGER.warn("Mod \"" + modName + "\" " + stat.val1 + " a newer version of iCommonLib!");
    		LOGGER.warn("Currently running iCommon " + API_VERSION + ", mod wants " + req);
    		LOGGER.warn("========================= ");
    	}
    	
    	return isLower;
    }

    public static final Logger LOGGER = LogManager.getLogger("iCommon");
    private static IServer iserver;
    static Loader modloader;
    
    public static LevelAccessor fake_world;
    
    public static void set_fake_world(LevelAccessor world) {
    	fake_world = world;
    }

    /**
     * Set the instance of the {@link IServer}
     */
    public static void set(IServer impl) {
        iserver = impl;
    }

    /**
     * Set the value of the mod loader.
     */
    public static void setLoader(Loader load) {
        modloader = load;
    }

    /**
     */
    public double getAPIRevision() {
        return API_VERSION;
    }

    public void onInitialize() {
        LOGGER.info("iCommon Mod initialized.");
	}

    public static IServer getIServer() {
        return iserver;
    }

}
