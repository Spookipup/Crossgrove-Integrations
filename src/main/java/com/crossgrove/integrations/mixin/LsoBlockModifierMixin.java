package com.crossgrove.integrations.mixin;

import java.util.List;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sfiomn.legendarysurvivaloverhaul.api.data.json.JsonTemperatureBlock;
import sfiomn.legendarysurvivaloverhaul.util.SpreadPoint;

import com.crossgrove.integrations.survival.CrossroadsLsoHeatBridge;

@Mixin(targets = "sfiomn.legendarysurvivaloverhaul.common.temperature.BlockModifier", remap = false)
public abstract class LsoBlockModifierMixin {
	@Shadow(remap = false)
	public abstract int tempInfluenceMaximumDist();

	@Inject(method = "getTemperatureFromSpreadPoint", at = @At("RETURN"), cancellable = true, remap = false)
	private void crossgrove$useCrossroadsHeatCapability(Level level, SpreadPoint spreadPoint,
														Map<ResourceLocation, List<JsonTemperatureBlock>> blockTemperatureCache,
														CallbackInfoReturnable<Float> callbackInfo) {
		float crossroadsInfluence = CrossroadsLsoHeatBridge.clampForSign(
				CrossroadsLsoHeatBridge.applyLsoDistanceFalloff(
						CrossroadsLsoHeatBridge.getInfluence(level, spreadPoint.position()),
						spreadPoint.spreadCapacity(),
						tempInfluenceMaximumDist()
				)
		);
		if (Math.abs(crossroadsInfluence) > Math.abs(callbackInfo.getReturnValueF())) {
			callbackInfo.setReturnValue(crossroadsInfluence);
		}
	}
}
