package org.bukkit.craftbukkit.inventory;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Objects;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.cardboardpowered.bridge.world.item.crafting.RecipeManagerBridge;

public class CraftShapedRecipe extends ShapedRecipe implements CraftRecipe {
    // TODO: Could eventually use this to add a matches() method or some such
    private net.minecraft.world.item.crafting.ShapedRecipe recipe;

    public CraftShapedRecipe(NamespacedKey key, ItemStack result) {
        super(key, result);
    }

    public CraftShapedRecipe(NamespacedKey key, ItemStack result, net.minecraft.world.item.crafting.ShapedRecipe recipe) {
        this(key, result);
        this.recipe = recipe;
    }

    public static CraftShapedRecipe fromBukkitRecipe(ShapedRecipe recipe) {
        if (recipe instanceof CraftShapedRecipe) {
            return (CraftShapedRecipe) recipe;
        }
        CraftShapedRecipe ret = new CraftShapedRecipe(recipe.getKey(), recipe.getResult());
        ret.setGroup(recipe.getGroup());
        ret.setCategory(recipe.getCategory());
        String[] shape = recipe.getShape();
        ret.shape(shape);
        Map<Character, RecipeChoice> ingredientMap = recipe.getChoiceMap();
        for (char c : ingredientMap.keySet()) {
            RecipeChoice stack = ingredientMap.get(c);
            if (stack != null) {
                ret.setIngredient(c, stack);
            }
        }
        return ret;
    }
    
    public void addToRecipeManager() {
        Map<Character, org.bukkit.inventory.RecipeChoice> choices = this.getChoiceMap();
        String[] shape = CraftShapedRecipe.replaceUndefinedIngredientsWithEmpty(this.getShape(), choices);
        choices.values().removeIf(Objects::isNull);
        Map<Character, Ingredient> ingredients = Maps.transformValues(choices, (bukkit) -> CraftRecipe.toIngredient(bukkit, false));
        ShapedRecipePattern pattern = ShapedRecipePattern.of(ingredients, shape);

        net.minecraft.world.item.crafting.ShapedRecipe recipe = new net.minecraft.world.item.crafting.ShapedRecipe(
            new net.minecraft.world.item.crafting.Recipe.CommonInfo(true),
            new net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo(CraftRecipe.getCategory(this.getCategory()), this.getGroup()),
            pattern,
            CraftItemStack.asTemplate(this.getResult())
        );
        ((RecipeManagerBridge)CraftServer.INSTANCE.getServer().getRecipeManager()).cardboard$addRecipe(new RecipeHolder<>(CraftNamespacedKey.toResourceKey(Registries.RECIPE, this.getKey()), recipe));
    }

    @Override
    public void addToCraftingManager() {
        this.addToRecipeManager();
    }

    private static String[] replaceUndefinedIngredientsWithEmpty(String[] shape, Map<Character, org.bukkit.inventory.RecipeChoice> ingredients) {
        for (int i = 0; i < shape.length; i++) {
            String row = shape[i];
            StringBuilder filteredRow = new StringBuilder(row.length());

            for (char character : row.toCharArray()) {
                filteredRow.append(ingredients.get(character) == null ? ' ' : character);
            }

            shape[i] = filteredRow.toString();
        }

        return shape;
    }
}
