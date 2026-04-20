package com.crossgrove.integrations.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.items.ItemStackHandler;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import net.stirdrem.overgeared.block.entity.AbstractSmithingAnvilBlockEntity;
import net.stirdrem.overgeared.recipe.ForgingRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.crossgrove.integrations.gtceu.CrossgroveMaterials;

@Mixin(value = AbstractSmithingAnvilBlockEntity.class, remap = false)
public abstract class OvergearedAnvilSlagMixin extends BlockEntity {
	private static final ResourceLocation BLOOM_FORGING_RECIPE =
			ResourceLocation.fromNamespaceAndPath("crossgrove", "overgeared/hot_iron_bloom_to_hot_iron_ingot");
	private static final ResourceLocation HOT_IRON_INGOT =
			ResourceLocation.fromNamespaceAndPath("gtceu", "hot_iron_ingot");

	@Shadow
	protected ForgingRecipe lastRecipe;

	@Shadow
	@Final
	protected ItemStackHandler itemHandler;

	private OvergearedAnvilSlagMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Inject(method = "craftItem", at = @At("TAIL"), remap = false)
	private void crossgrove$dropBloomSlag(CallbackInfo ci) {
		if (this.level == null || this.level.isClientSide || this.lastRecipe == null) {
			return;
		}
		if (!BLOOM_FORGING_RECIPE.equals(this.lastRecipe.getId())) {
			return;
		}

		ItemStack output = this.itemHandler.getStackInSlot(10);
		if (output.isEmpty() || !HOT_IRON_INGOT.equals(BuiltInRegistries.ITEM.getKey(output.getItem()))) {
			return;
		}

		Containers.dropItemStack(
				this.level,
				this.worldPosition.getX() + 0.5D,
				this.worldPosition.getY() + 1.0D,
				this.worldPosition.getZ() + 0.5D,
				ChemicalHelper.get(TagPrefix.dust, CrossgroveMaterials.IronSlag)
		);
	}
}
