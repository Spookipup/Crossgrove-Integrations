package com.crossgrove.integrations.mixin;

import net.minecraftforge.fluids.FluidStack;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thedarkcolour.exdeorum.recipe.barrel.BarrelFluidMixingRecipe;

import com.crossgrove.integrations.jei.CrossgroveBarrelFluidMixingRecipe;

@Mixin(targets = "thedarkcolour.exdeorum.compat.jei.BarrelMixingCategory$Fluids", remap = false)
public abstract class ExDeorumBarrelFluidMixingCategoryMixin {
	private static final int FLUID_CAPACITY = 1000;
	private static final int BASE_X = 1;
	private static final int ADDITIVE_X = 33;
	private static final int OUTPUT_X = 79;
	private static final int SLOT_Y = 1;
	private static final int SLOT_SIZE = 16;

	@Inject(
			method = "setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;Lthedarkcolour/exdeorum/recipe/barrel/BarrelFluidMixingRecipe;Lmezz/jei/api/recipe/IFocusGroup;)V",
			at = @At("HEAD"),
			cancellable = true,
			remap = false
	)
	private void crossgrove$setAlloyRecipe(IRecipeLayoutBuilder builder, BarrelFluidMixingRecipe recipe,
										   IFocusGroup focuses, CallbackInfo ci) {
		if (!(recipe instanceof CrossgroveBarrelFluidMixingRecipe alloyRecipe)) {
			return;
		}

		addFluidSlot(builder, RecipeIngredientRole.INPUT, BASE_X, alloyRecipe.baseInput());
		addFluidSlot(builder, RecipeIngredientRole.INPUT, ADDITIVE_X, alloyRecipe.additiveInput());
		addFluidSlot(builder, RecipeIngredientRole.OUTPUT, OUTPUT_X, alloyRecipe.outputFluid());
		ci.cancel();
	}

	private static void addFluidSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, FluidStack stack) {
		builder.addSlot(role, x, SLOT_Y)
				.addFluidStack(stack.getFluid(), stack.getAmount(), stack.getTag())
				.setFluidRenderer(FLUID_CAPACITY, false, SLOT_SIZE, SLOT_SIZE);
	}
}
