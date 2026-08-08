package org.cardboardpowered.mixin.paper;

import io.papermc.paper.plugin.entrypoint.classloader.group.PaperPluginClassLoaderStorage;
import io.papermc.paper.plugin.provider.classloader.PaperClassLoaderStorage;

public class PaperClassLoaderStorageAccessMixin {

	/**
     * The shared instance of the {@link PaperClassLoaderStorage}, supplied through the {@link java.util.ServiceLoader}
     * by the server.
     */
    static final PaperClassLoaderStorage INSTANCE = new PaperPluginClassLoaderStorage();
	
}
