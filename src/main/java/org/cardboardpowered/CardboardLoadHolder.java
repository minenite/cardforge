package org.cardboardpowered;

import net.minecraft.server.WorldLoader;

public class CardboardLoadHolder {

	public static java.util.concurrent.atomic.AtomicReference<WorldLoader.DataLoadContext> worldLoader = new java.util.concurrent.atomic.AtomicReference<>();
	
}
