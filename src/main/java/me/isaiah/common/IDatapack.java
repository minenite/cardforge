package me.isaiah.common;

import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;

public interface IDatapack {

    public Pack get_minecraft();

    public String get_raw_id();

    public boolean is_required();

    public boolean is_enabled();

    /**
     * 1.20.2+ only
     * NULL on <=1.20.1
     */
	PackMetadataSection get_metadata();

	
}
