package me.isaiah.common.cmixin;

import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;

public interface IMixinResourcePackProfile {

	/**
	 * 1.20.2+ Only
	 * NULL on <=1.20.1
	 */
    public PackResources IC$open_pack(String id);
    
	/**
	 * 1.20.2+ Only
	 * NULL on <=1.20.1
	 */
    public PackMetadataSection IC$open_and_parse_metadata();
    
    /**
     */
	public String IC$get_raw_id();

	/**
	 */
	public boolean IC$is_required();

	/**
	 */
	public boolean IC$is_enabled();
	
}
