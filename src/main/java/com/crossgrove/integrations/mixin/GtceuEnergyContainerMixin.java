package com.crossgrove.integrations.mixin;

import com.crossgrove.integrations.gtceu.GtceuCrossroadsPower;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

@Mixin(targets = "com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer", remap = false)
public abstract class GtceuEnergyContainerMixin {
    @Inject(method = "handleRecipeInner", at = @At("HEAD"), cancellable = true, remap = false)
    private void crossgrove$satisfyEuRecipesWithCrossroadsPower(@Coerce Object io, @Coerce Object recipe,
                                                                List<?> left, boolean simulate,
                                                                CallbackInfoReturnable<List<?>> callbackInfo) {
        if (GtceuCrossroadsPower.isHeatPoweredMachine(getMachineReflectively())) {
            callbackInfo.setReturnValue(null);
        }
    }

    @Inject(method = "acceptEnergyFromNetwork", at = @At("HEAD"), cancellable = true, remap = false)
    private void crossgrove$rejectEuInput(Direction side, long voltage, long amperage,
                                          CallbackInfoReturnable<Long> callbackInfo) {
        if (GtceuCrossroadsPower.isHeatPoweredMachine(getMachineReflectively())) {
            callbackInfo.setReturnValue(0L);
        }
    }

    @Inject(method = "inputsEnergy", at = @At("HEAD"), cancellable = true, remap = false)
    private void crossgrove$hideEuInput(Direction side, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (GtceuCrossroadsPower.isHeatPoweredMachine(getMachineReflectively())) {
            callbackInfo.setReturnValue(false);
        }
    }

    private Object getMachineReflectively() {
        try {
            Method method = getClass().getMethod("getMachine");
            method.setAccessible(true);
            return method.invoke(this);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            return null;
        }
    }
}
