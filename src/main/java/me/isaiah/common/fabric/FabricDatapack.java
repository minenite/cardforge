package me.isaiah.common.fabric;

import me.isaiah.common.IDatapack;
import me.isaiah.common.cmixin.IMixinResourcePackProfile;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;

public class FabricDatapack implements IDatapack {

    private Pack handle;
    private PackMetadataSection resourcePackInfo;

    public FabricDatapack(Pack handler) {
        this.handle = handler;

        IMixinResourcePackProfile im = ((IMixinResourcePackProfile)(Object)handle);
        PackMetadataSection data = im.IC$open_and_parse_metadata();
        if (null != data) {
        	resourcePackInfo = data;
        }
    }
    
    @Override
    public PackMetadataSection get_metadata() {
    	if (null == resourcePackInfo) {
    		return null;
    	}
    	return resourcePackInfo;
    }
	
	@Override
	public Pack get_minecraft() {
		return handle;
	}

	@Override
	public String get_raw_id() {
		return ((IMixinResourcePackProfile)(Object)handle).IC$get_raw_id();
	}

	@Override
	public boolean is_required() {
		return ((IMixinResourcePackProfile)(Object)handle).IC$is_required();
	}

	@Override
	public boolean is_enabled() {
		return ((IMixinResourcePackProfile)(Object)handle).IC$is_enabled();
	}

}
