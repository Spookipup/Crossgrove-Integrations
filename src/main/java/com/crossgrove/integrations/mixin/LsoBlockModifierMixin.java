package com.crossgrove.integrations.mixin;

import com.crossgrove.integrations.survival.CrossroadsLsoHeatBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sfiomn.legendarysurvivaloverhaul.api.data.json.JsonTemperatureBlock;
import sfiomn.legendarysurvivaloverhaul.util.SpreadPoint;

import java.util.List;
import java.util.Map;

// lets crossroads heat beat lso's block temp only when it is stronger
@Mixin(targets = "sfiomn.legendarysurvivaloverhaul.common.temperature.BlockModifier", remap = false)
public abstract class LsoBlockModifierMixin {
    @Inject(method = "getTemperatureFromSpreadPoint", at = @At("RETURN"), cancellable = true, remap = false)
    private void crossgrove$useCrossroadsHeatCapability(Level level, SpreadPoint spreadPoint,
                                                       Map<ResourceLocation, List<JsonTemperatureBlock>> blockTemperatureCache,
                                                       CallbackInfoReturnable<Float> callbackInfo) {
        float crossroadsInfluence = CrossroadsLsoHeatBridge.clampForSign(
                CrossroadsLsoHeatBridge.getInfluence(level, spreadPoint.position())
        );
        if (Math.abs(crossroadsInfluence) > Math.abs(callbackInfo.getReturnValueF())) {
            callbackInfo.setReturnValue(crossroadsInfluence);
        }
    }
}
