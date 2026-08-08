package org.cardboardpowered.mixin.world.item.crafting;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.spongepowered.asm.mixin.Unique;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.*;
import org.cardboardpowered.bridge.world.item.crafting.RecipeMapBridge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(RecipeMap.class)
public abstract class RecipeMapMixin implements RecipeMapBridge {
    @Shadow
    @org.spongepowered.asm.mixin.Mutable
    @Final
    public Multimap<RecipeType<?>, RecipeHolder<?>> byType;

    @Shadow
    @org.spongepowered.asm.mixin.Mutable
    @Final
    public Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey;

    @Shadow
    public abstract <I extends RecipeInput, T extends Recipe<I>> Collection<RecipeHolder<T>> byType(RecipeType<T> recipeType);

    /*
    @Inject(method = "create", at = @At("HEAD"), cancellable = true)
    private static void createCraftBukkit(Iterable<RecipeHolder<?>> recipes, CallbackInfoReturnable<RecipeMap> cir) {
        ImmutableMultimap.Builder<RecipeType<?>, RecipeHolder<?>> builder = ImmutableMultimap.builder();
        com.google.common.collect.ImmutableMap.Builder<ResourceKey<Recipe<?>>, RecipeHolder<?>> builder1 = ImmutableMap.builder();

        for (RecipeHolder<?> recipeHolder : recipes) {
            builder.put(recipeHolder.value().getType(), recipeHolder);
            builder1.put(recipeHolder.id(), recipeHolder);
        }

        // CraftBukkit start - mutable
        cir.setReturnValue(new RecipeMap(com.google.common.collect.LinkedHashMultimap.create(builder.build()), com.google.common.collect.Maps.newLinkedHashMap(builder1.build())));
    }
    */
    
    /**
     * Use @ModifyReturnValue here so fabric-api can inject during HEAD.
     * 
     * @see {@link net.fabricmc.fabric.impl.recipe.sync.SyncedSerializerAwarePreparedRecipe}
     * 
     * @author Cardboard Mod
     */
    @ModifyReturnValue(method = "create", at = @At("RETURN"))
    private static RecipeMap cardboard$recipemap_create_make_return_mutable(
    		RecipeMap original,
    		@Local ImmutableMultimap.Builder<RecipeType<?>, RecipeHolder<?>> byType,
            @Local ImmutableMap.Builder<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey
        ) {


        return new RecipeMap(com.google.common.collect.LinkedHashMultimap.create(byType.build()), com.google.common.collect.Maps.newLinkedHashMap(byKey.build()));
    }

    @Override
    public void cardboard$addRecipe(RecipeHolder<?> holder) {
        if (this.byKey.containsKey(holder.id())) {
            throw new IllegalStateException("Duplicate recipe ignored with ID " + holder.id());
        }

        // Make sure the collections can actually be written to. create() is patched
        // to hand back mutable copies, but not every RecipeMap arrives through it -
        // reloads and the empty map do not - and an immutable one made
        // Bukkit.addRecipe throw a bare UnsupportedOperationException from deep
        // inside Guava, with nothing naming the real problem.
        cardboard$ensureMutable();

        this.byType.get(holder.value().getType()).add(holder);
        this.byKey.put(holder.id(), holder);
    }

    @Unique
    private void cardboard$ensureMutable() {
        if (!(this.byType instanceof LinkedHashMultimap)) {
            this.byType = LinkedHashMultimap.create(this.byType);
        }
        if (!(this.byKey instanceof java.util.LinkedHashMap)) {
            this.byKey = com.google.common.collect.Maps.newLinkedHashMap(this.byKey);
        }
    }
    // CraftBukkit end

    // Paper start - replace removeRecipe implementation
    @Override
    public <T extends RecipeInput> boolean cardboard$removeRecipe(ResourceKey<Recipe<T>> mcKey) {
        // Same reason as the add path: byType may still be the immutable map the
        // vanilla constructor built, and removing from it throws.
        cardboard$ensureMutable();

        //noinspection unchecked
        final RecipeHolder<Recipe<T>> remove = (RecipeHolder<Recipe<T>>) this.byKey.remove(mcKey);
        if (remove == null) {
            return false;
        }
        // Through the field, not the byType(...) accessor: that returns an
        // unmodifiable view even when the backing multimap is mutable.
        return this.byType.get(remove.value().getType()).remove(remove);
        // Paper end - why are you using a loop???
    }
    // Paper end - replace removeRecipe implementation
}
