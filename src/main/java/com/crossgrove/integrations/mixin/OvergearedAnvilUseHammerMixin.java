package com.crossgrove.integrations.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import net.stirdrem.overgeared.block.custom.AbstractSmithingAnvil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.crossgrove.integrations.gtceu.overgeared.OvergearedHammerCompat;

@Mixin(value = AbstractSmithingAnvil.class, remap = false)
public abstract class OvergearedAnvilUseHammerMixin {
	@Inject(method = "m_6227_", at = @At("HEAD"), remap = false)
	private void crossgrove$captureForgingTool(BlockState state, Level level, BlockPos pos, Player player,
											   InteractionHand hand, BlockHitResult hit,
											   CallbackInfoReturnable<InteractionResult> cir) {
		OvergearedHammerCompat.captureActiveTool(player.getItemInHand(hand));
	}

	@Inject(method = "m_6227_", at = @At("RETURN"), remap = false)
	private void crossgrove$clearForgingTool(BlockState state, Level level, BlockPos pos, Player player,
											 InteractionHand hand, BlockHitResult hit,
											 CallbackInfoReturnable<InteractionResult> cir) {
		OvergearedHammerCompat.clearActiveTool();
	}

	@Redirect(
			method = "m_6227_",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/item/ItemStack;m_204117_(Lnet/minecraft/tags/TagKey;)Z",
					ordinal = 0,
					remap = false
			),
			remap = false
	)
	private boolean crossgrove$allowGtceuHammerUse(ItemStack stack, TagKey<Item> tag) {
		return OvergearedHammerCompat.isForgeActionTool(stack);
	}
}
