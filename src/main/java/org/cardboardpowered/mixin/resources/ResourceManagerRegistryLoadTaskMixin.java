package org.cardboardpowered.mixin.resources;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;

import net.minecraft.resources.RegistryOps.RegistryInfoLookup;
import net.minecraft.server.packs.resources.ResourceManager;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;

import io.papermc.paper.registry.PaperRegistryAccess;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.resources.RegistryDataLoader.RegistryData;
import net.minecraft.resources.RegistryLoadTask.PendingRegistration;
import net.minecraft.resources.*;
import net.minecraft.resources.RegistryOps.RegistryInfoLookup;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.tags.TagLoader.ElementLookup;
import net.minecraft.util.Util;
import net.minecraft.util.thread.ParallelMapTransform;

@Mixin(ResourceManagerRegistryLoadTask.class)
public class ResourceManagerRegistryLoadTaskMixin<T> extends RegistryLoadTask<T> {

	protected ResourceManagerRegistryLoadTaskMixin(RegistryData<T> data, Lifecycle lifecycle,
			Map<ResourceKey<?>, Exception> loadingErrors) {
		super(data, lifecycle, loadingErrors);
	}
	
	@Shadow
	private static Function<Optional<KnownPack>, RegistrationInfo> REGISTRATION_INFO_CACHE;

	@Shadow
	private ResourceManager resourceManager;
	
	public CompletableFuture<?> load(final RegistryInfoLookup context, final Executor executor) {
		FileToIdConverter lister = FileToIdConverter.registry(this.registryKey());
		return CompletableFuture.supplyAsync(() -> lister.listMatchingResources(this.resourceManager), executor)
				.thenCompose(registryResources -> {
					RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, context);
					return ParallelMapTransform.schedule(registryResources, (resourceId, thunk) -> {
						ResourceKey<T> elementKey = ResourceKey.create(this.registryKey(), lister.fileToId(resourceId));
						RegistrationInfo registrationInfo = (RegistrationInfo) REGISTRATION_INFO_CACHE
								.apply(thunk.knownPackInfo());
						return new PendingRegistration(elementKey,
								PendingRegistration.loadFromResource(this.data.elementCodec(), ops, elementKey, thunk),
								registrationInfo);
					}, executor);
				}).thenAcceptAsync(loadedEntries -> {
					this.registerElements(
							loadedEntries.entrySet().stream().sorted(Entry.comparingByKey()).map(Entry::getValue));
					
					 PaperRegistryAccess.instance().lockReferenceHolders(registry.key());
					
					ElementLookup<Holder<T>> tagElementLookup = ElementLookup.fromGetters(this.registryKey(),
							this.concurrentRegistrationGetter, this.readOnlyRegistry());
					Map<TagKey<T>, List<Holder<T>>> pendingTags = TagLoader.loadTagsForRegistry(this.resourceManager,
							this.registryKey(), tagElementLookup);
					this.registerTags(pendingTags);
				}, executor);
	}
	
}
