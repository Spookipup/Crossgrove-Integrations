package com.crossgrove.integrations.jei;

import java.util.List;

import net.minecraft.resources.ResourceLocation;

import com.gregtechceu.gtceu.common.data.GTMaterials;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import thedarkcolour.exdeorum.recipe.barrel.BarrelFluidMixingRecipe;

import com.crossgrove.integrations.CrossgroveIntegrations;

final class ExDeorumBarrelFluidMixingJeiCompat {
	private static final RecipeType<BarrelFluidMixingRecipe> BARREL_FLUID_MIXING =
			RecipeType.create("exdeorum", "barrel_fluid_mixing", BarrelFluidMixingRecipe.class);

	private ExDeorumBarrelFluidMixingJeiCompat() {
	}

	static void registerRecipes(IRecipeRegistration registration) {
		registration.addRecipes(BARREL_FLUID_MIXING, getRecipes());
	}

	private static List<BarrelFluidMixingRecipe> getRecipes() {
		return List.of(
				new CrossgroveBarrelFluidMixingRecipe(
						ResourceLocation.fromNamespaceAndPath(
								CrossgroveIntegrations.MOD_ID,
								"barrel_fluid_mixing/bronze_from_tin_and_copper"
						),
						GTMaterials.Tin.getFluid(250),
						GTMaterials.Copper.getFluid(750),
						GTMaterials.Bronze.getFluid(1000)
				),
				new CrossgroveBarrelFluidMixingRecipe(
						ResourceLocation.fromNamespaceAndPath(
								CrossgroveIntegrations.MOD_ID,
								"barrel_fluid_mixing/bronze_from_copper_and_tin"
						),
						GTMaterials.Copper.getFluid(750),
						GTMaterials.Tin.getFluid(250),
						GTMaterials.Bronze.getFluid(1000)
				)
		);
	}
}
