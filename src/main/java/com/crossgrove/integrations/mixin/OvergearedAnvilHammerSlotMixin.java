package com.crossgrove.integrations.mixin;

import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.crossgrove.integrations.gtceu.overgeared.OvergearedHammerCompat;

@Mixin(targets = "net.stirdrem.overgeared.screen.AbstractSmithingAnvilMenu$2", remap = false)
public abstract class OvergearedAnvilHammerSlotMixin {
	@Inject(method = "m_5857_", at = @At("HEAD"), cancellable = true, remap = false)
	private void crossgrove$allowGtceuHammer(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (OvergearedHammerCompat.isUsableForgingTool(stack)) {
			cir.setReturnValue(true);
		}
	}
}
