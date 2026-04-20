package com.crossgrove.integrations.jei;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import net.minecraftforge.fluids.FluidStack;

import thedarkcolour.exdeorum.recipe.barrel.BarrelFluidMixingRecipe;

public final class CrossgroveBarrelFluidMixingRecipe extends BarrelFluidMixingRecipe {
	private final FluidStack baseInput;
	private final FluidStack additiveInput;
	private final FluidStack outputFluid;

	public CrossgroveBarrelFluidMixingRecipe(ResourceLocation id, FluidStack baseInput, FluidStack additiveInput,
											 FluidStack outputFluid) {
		super(id, baseInput.getFluid(), baseInput.getAmount(), additiveInput.getFluid(), Items.AIR, null, false);
		this.baseInput = baseInput.copy();
		this.additiveInput = additiveInput.copy();
		this.outputFluid = outputFluid.copy();
	}

	public FluidStack baseInput() {
		return this.baseInput.copy();
	}

	public FluidStack additiveInput() {
		return this.additiveInput.copy();
	}

	public FluidStack outputFluid() {
		return this.outputFluid.copy();
	}
}
