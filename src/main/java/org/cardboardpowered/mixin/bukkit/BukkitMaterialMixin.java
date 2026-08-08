package org.cardboardpowered.mixin.bukkit;

import org.bukkit.Material;
import org.bukkit.inventory.ItemType;
import org.cardboardpowered.impl.CardboardModdedMaterial;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.cardboardpowered.bridge.bukkit.BukkitMaterialBridge;

@Mixin(value = Material.class, remap = false)
public class BukkitMaterialMixin implements BukkitMaterialBridge {

	
	
	
	/**
	 * @reason We need API update
	 * @see https://hub.spigotmc.org/stash/projects/SPIGOT/repos/bukkit/diff/src/main/java/org/bukkit/Material.java?until=ad2fd61c8784c7bac6542e39fca7e506c7966865
	 */
	@Inject(at = @At("HEAD"), method = "isBlock", cancellable = true, remap = false)
	public void fix_material_block(CallbackInfoReturnable<Boolean> ci) {
		if ( ((Material)(Object)this).name().equalsIgnoreCase("GRASS") ) {
			ci.setReturnValue(false);
		}
	}
	
	
    //public static final String LEGACY_PREFIX = "LEGACY_";
    
	@Shadow
	private int id;
    //private final Constructor<? extends MaterialData> ctor;
    //private static final Map<String, Material> BY_NAME;
    //private final int maxStack;
    
	// @Shadow
	// private short durability;
    //public final Class<?> data;
    //private final boolean legacy;
    //private final NamespacedKey key;
    //private boolean isBlock;
	
	@Shadow
	public ItemType asItemType() {
        return null; // Shadowed
    }
	
	/**
	 * Makes Material.values() observe materials added after class initialisation.
	 *
	 * Enum extension writes the private static final $VALUES array through Unsafe,
	 * and that write really does land - reading $VALUES reflectively shows the
	 * modded entries. But values() compiles to a getstatic on a static final
	 * field, which HotSpot is free to constant-fold once the class is initialised,
	 * and does, because values() is hot during registration. The result is that
	 * every modded Material is reachable by name and by key while being invisible
	 * to any plugin that iterates values() - 1691 entries returned against 2204
	 * actually present.
	 *
	 * Rather than fighting the folding at the read site, registration publishes
	 * the extended array to MaterialValues and this returns that.
	 *
	 * @author CardForge
	 * @reason $VALUES is constant-folded, so the extended entries are never seen
	 */
	@Inject(method = "values", at = @At("HEAD"), cancellable = true, remap = false)
	private static void cardboard$liveValues(CallbackInfoReturnable<Material[]> cir) {
		Material[] live = org.cardboardpowered.impl.MaterialValues.get();
		if (live != null) {
			cir.setReturnValue(live.clone());
		}
	}

	@Shadow(aliases = "$VALUES")
	private static Material[] cardboard$VALUES;

	@Shadow
	@org.spongepowered.asm.mixin.Mutable
	private org.bukkit.NamespacedKey key;

	@Override
	public void cardboard$setKey(org.bukkit.NamespacedKey key) {
		this.key = key;
	}

	private org.cardboardpowered.impl.CardboardModdedMaterial moddedData;

	@Override
	public boolean isModded() {
		return null != moddedData;
	}

	@Override
	public CardboardModdedMaterial getModdedData() {
		return moddedData;
	}

	@Override
	public void setModdedData(CardboardModdedMaterial data) {
		this.moddedData = data;
	}

	/*private Material(final int id, org.cardboardpowered.impl.CardboardModdedMaterial data) {
		this(id, 64);
		setModdedData(data);
	}*/
	
	/**
	 * @author Cardboard
	 * @reason Support Modded Materials
	 */
	@Overwrite
    public short getMaxDurability() {
		if (isModded()) return moddedData.getDamage(); // CARDBOARD
        // return this.durability;
        
        ItemType type = asItemType();
        return type == null ? 0 : type.getMaxDurability();
    }
	
	/**
	 * @author Cardboard
	 * @reason Support Modded Materials
	 */
    @Overwrite
    public int getId() {
    	// CARDBOARD REMOVED: Preconditions.checkArgument(this.legacy, "Cannot get ID of Modern Material");
        return this.id;
    }
	
    /*
	@Inject(at = @At("HEAD"), method = "isBlock0", cancellable = true, remap = false)
	public void mod_is_block(CallbackInfoReturnable<Boolean> ci) {
		if (isModded()) {
			ci.setReturnValue(moddedData.isBlock());
		}
	}
	*/
	
	@Inject(at = @At("HEAD"), method = "isItem", cancellable = true, remap = false)
	public void mod_is_item(CallbackInfoReturnable<Boolean> ci) {
		if (isModded()) {
			ci.setReturnValue(moddedData.isItem());
		}
	}

}
