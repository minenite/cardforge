package org.bukkit.craftbukkit.inventory;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.cardboardpowered.bridge.world.item.crafting.RecipeManagerBridge;

public class CraftCampfireRecipe extends CampfireRecipe implements CraftRecipe {
    public CraftCampfireRecipe(NamespacedKey key, ItemStack result, RecipeChoice source, float experience, int cookingTime) {
        super(key, result, source, experience, cookingTime);
    }

    public static CraftCampfireRecipe fromBukkitRecipe(CampfireRecipe recipe) {
        if (recipe instanceof CraftCampfireRecipe) {
            return (CraftCampfireRecipe) recipe;
        }
        CraftCampfireRecipe ret = new CraftCampfireRecipe(recipe.getKey(), recipe.getResult(), recipe.getInputChoice(), recipe.getExperience(), recipe.getCookingTime());
        ret.setGroup(recipe.getGroup());
        ret.setCategory(recipe.getCategory());
        return ret;
    }
    
    // @Override
    public void addToRecipeManager() {
        CampfireCookingRecipe recipe = new net.minecraft.world.item.crafting.CampfireCookingRecipe(
            new net.minecraft.world.item.crafting.Recipe.CommonInfo(true),
            new net.minecraft.world.item.crafting.AbstractCookingRecipe.CookingBookInfo(CraftRecipe.getCategory(this.getCategory()), this.getGroup()),
            CraftRecipe.toIngredient(this.getInputChoice(), true),
            CraftItemStack.asTemplate(this.getResult()),
            this.getExperience(),
            this.getCookingTime()
        );
        ((RecipeManagerBridge)CraftServer.INSTANCE.getServer().getRecipeManager()).cardboard$addRecipe(new RecipeHolder<>(CraftNamespacedKey.toResourceKey(Registries.RECIPE, this.getKey()), recipe));
    }

    @Override
    public void addToCraftingManager() {
    	addToRecipeManager();
    	//ItemStack result = this.getResult();

        //((RecipeManagerBridge)CraftServer.INSTANCE.getServer().getRecipeManager()).cardboard$addRecipe(new RecipeHolder<>(CraftRecipe.toMinecraft(this.getKey()), new net.minecraft.world.item.crafting.CampfireCookingRecipe(this.getGroup(), CraftRecipe.getCategory(this.getCategory()), this.toNMS(this.getInputChoice(), true), CraftItemStack.asNMSCopy(result), this.getExperience(), this.getCookingTime())));
    }
}
