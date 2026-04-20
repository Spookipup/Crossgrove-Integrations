package com.crossgrove.integrations.jei;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.stirdrem.overgeared.compat.jei.KnappingRecipeCategory;
import net.stirdrem.overgeared.recipe.ModRecipeTypes;
import net.stirdrem.overgeared.recipe.RockKnappingRecipe;

final class ClaySculptingJeiCompat {
	private ClaySculptingJeiCompat() {
	}

	static void registerCategories(IRecipeCategoryRegistration registration) {
		registration.addRecipeCategories(new ClaySculptingRecipeCategory(
				registration.getJeiHelpers().getGuiHelper()
		));
	}

	static void registerRecipes(IRecipeRegistration registration) {
		List<RockKnappingRecipe> clayRecipes = getClaySculptingRecipes();
		if (!clayRecipes.isEmpty()) {
			registration.addRecipes(ClaySculptingRecipeCategory.TYPE, clayRecipes);
		}
	}

	static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalyst(new ItemStack(Items.CLAY_BALL), ClaySculptingRecipeCategory.TYPE);
	}

	static void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
		List<RockKnappingRecipe> clayRecipes = getClaySculptingRecipes();
		if (!clayRecipes.isEmpty()) {
			jeiRuntime.getRecipeManager().hideRecipes(KnappingRecipeCategory.KNAPPING_RECIPE_TYPE, clayRecipes);
		}
	}

	private static List<RockKnappingRecipe> getClaySculptingRecipes() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return List.of();
		}

		return minecraft.level.getRecipeManager()
				.getAllRecipesFor(ModRecipeTypes.KNAPPING.get())
				.stream()
				.filter(ClaySculptingRecipeCategory::isClaySculptingRecipe)
				.toList();
	}
}
