package com.crossgrove.integrations.mixin;

import net.minecraft.world.level.block.entity.BlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thedarkcolour.exdeorum.blockentity.LavaCrucibleBlockEntity;

import com.crossgrove.integrations.gtceu.exdeorum.ExDeorumHeatProvider;

@Mixin(value = LavaCrucibleBlockEntity.class, remap = false)
public abstract class ExDeorumLavaCrucibleHeatMixin {
	@Inject(method = "getMeltingRate", at = @At("RETURN"), cancellable = true, remap = false)
	private void crossgrove$useCrossroadsHeat(CallbackInfoReturnable<Integer> cir) {
		int heatRate = ExDeorumHeatProvider.meltingRateFromTemperature(
				ExDeorumHeatProvider.getTemperature((BlockEntity) (Object) this)
		);
		if (heatRate > cir.getReturnValue()) {
			cir.setReturnValue(heatRate);
		}
	}
}
