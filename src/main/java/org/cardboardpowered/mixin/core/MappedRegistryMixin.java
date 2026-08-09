package org.cardboardpowered.mixin.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.cardboardpowered.bridge.core.MappedRegistryBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MappedRegistry.class)
public class MappedRegistryMixin<T> implements MappedRegistryBridge<T> {
	
	@Shadow
	private Map<T, Holder.Reference<T>> unregisteredIntrusiveHolders;
	
	@Shadow
	private boolean frozen;
	
	// Cardboard - Paper - support pre-filling in registry mod API
	public final Map<Identifier, T> temporaryUnfrozenMap = new HashMap<>();

	@Override
	public Map<Identifier, T> cb$temporaryUnfrozenMap() {
		return temporaryUnfrozenMap;
	}
	
	// Cardboard - Paper
	// used to clear intrusive holders from GameEvent, Item, Block, EntityType, and Fluid from unused instances of those types
	@Override
	public void clearIntrusiveHolder(final T instance) {
		if (null != this.unregisteredIntrusiveHolders) {
			this.unregisteredIntrusiveHolders.remove(instance);
		}
	}
	
	@Inject(at = @At("HEAD"), method = "freeze")
	public void cb$paper_clear_unfrozen_map(CallbackInfoReturnable<net.minecraft.core.Registry> ci) {
		if (!this.frozen) {
			 this.temporaryUnfrozenMap.clear(); // Paper - support pre-filling in registry mod API
		}
	}
	
	@Inject(at = @At("RETURN"), method = "register(Lnet/minecraft/resources/ResourceKey;Ljava/lang/Object;Lnet/minecraft/core/RegistrationInfo;)Lnet/minecraft/core/Holder$Reference;")
	public void cb$paper_unfrozen_map(ResourceKey<T> key, T value, RegistrationInfo info, CallbackInfoReturnable<Holder.Reference> ci) {
		// Lnet/minecraft/registry/MutableRegistry;add(Lnet/minecraft/registry/RegistryKey;Ljava/lang/Object;Lnet/minecraft/registry/entry/RegistryEntryInfo;)Lnet/minecraft/registry/entry/RegistryEntry$Reference;
	
		 this.temporaryUnfrozenMap.put(key.identifier(), value); // Paper - support pre-filling in registry mod API
	}
	
	/**
	 * Paper reads this while a registry is unfrozen, to copy an existing value as
	 * the base of a modified one.
	 *
	 * <p>It used to consult {@link #temporaryUnfrozenMap} whenever the registry was
	 * unfrozen, and that map is filled by an inject on
	 * {@code register(ResourceKey, T, RegistrationInfo)}. NeoForge turned that
	 * signature into a delegate and moved the body to a four-argument
	 * {@code register(int, ResourceKey, T, RegistrationInfo)}, so anything calling
	 * the wide overload directly - which NeoForge's own registration does - never
	 * reaches the inject, and the map silently misses those entries.
	 *
	 * <p>NeoForge also made the map unnecessary. It binds the value at
	 * registration time for exactly this reason:
	 *
	 * <pre>// Neo: Bind the value immediately so it can be queried while the registry is not frozen</pre>
	 *
	 * <p>So the bound value is asked first and answers for every registration
	 * regardless of which overload made it. The map stays as a fallback rather
	 * than being deleted, since it still covers anything registered before a
	 * holder is bound, and costs nothing when the lookup already succeeded.
	 */
	@Override
    public Optional<T> getValueForCopying(ResourceKey<T> resourceKey) {
        Optional<T> bound = ((MappedRegistry) (Object) this).getOptional(resourceKey);
        if (bound.isPresent()) {
            return bound;
        }
        return this.frozen ? Optional.empty()
                : Optional.ofNullable(this.temporaryUnfrozenMap.get(resourceKey.identifier()));
    }
	
}
