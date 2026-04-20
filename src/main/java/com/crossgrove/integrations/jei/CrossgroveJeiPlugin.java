package com.crossgrove.integrations.jei;

import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.fml.ModList;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;

import com.crossgrove.integrations.CrossgroveIntegrations;

@JeiPlugin
public final class CrossgroveJeiPlugin implements IModPlugin {
	@Override
	public ResourceLocation getPluginUid() {
		return ResourceLocation.fromNamespaceAndPath(CrossgroveIntegrations.MOD_ID, "jei_plugin");
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		if (isClaySculptingAvailable()) {
			ClaySculptingJeiCompat.registerCategories(registration);
		}
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		if (isClaySculptingAvailable()) {
			ClaySculptingJeiCompat.registerRecipes(registration);
		}
		if (isBarrelFluidMixingAvailable()) {
			ExDeorumBarrelFluidMixingJeiCompat.registerRecipes(registration);
		}
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		if (isClaySculptingAvailable()) {
			ClaySculptingJeiCompat.registerRecipeCatalysts(registration);
		}
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
		if (isClaySculptingAvailable()) {
			ClaySculptingJeiCompat.onRuntimeAvailable(jeiRuntime);
		}
	}

	private static boolean isClaySculptingAvailable() {
		return ModList.get().isLoaded("overgeared");
	}

	private static boolean isBarrelFluidMixingAvailable() {
		return ModList.get().isLoaded("exdeorum") && ModList.get().isLoaded("gtceu");
	}
}
