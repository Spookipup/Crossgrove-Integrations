package com.crossgrove.integrations.mixin;

import java.util.Optional;

import net.minecraft.world.item.ItemStack;

import net.stirdrem.overgeared.block.entity.AbstractSmithingAnvilBlockEntity;
import net.stirdrem.overgeared.recipe.ForgingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.crossgrove.integrations.gtceu.overgeared.CrossgroveOvergearedRecipes;
import com.crossgrove.integrations.gtceu.overgeared.OvergearedHammerCompat;

@Mixin(value = AbstractSmithingAnvilBlockEntity.class, remap = false)
public abstract class OvergearedAnvilToolResultMixin {
	@Shadow
	public abstract Optional<ForgingRecipe> getCurrentRecipe();

	@ModifyVariable(method = "craftItem", at = @At(value = "STORE"), ordinal = 0, remap = false)
	private ItemStack crossgrove$useToolSpecificCraftResult(ItemStack result) {
		return toolSpecificResultOrDefault(result);
	}

	@ModifyVariable(method = "hasRecipe", at = @At(value = "STORE"), ordinal = 0, remap = false)
	private ItemStack crossgrove$useToolSpecificOutputCheck(ItemStack result) {
		return toolSpecificResultOrDefault(result);
	}

	private ItemStack toolSpecificResultOrDefault(ItemStack result) {
		Optional<ForgingRecipe> recipe = getCurrentRecipe();
		if (recipe.isEmpty()) {
			return result;
		}

		ItemStack alternate = CrossgroveOvergearedRecipes.toolSpecificResult(
				recipe.get().getId(), OvergearedHammerCompat.activeToolType());
		return alternate.isEmpty() ? result : alternate;
	}
}
