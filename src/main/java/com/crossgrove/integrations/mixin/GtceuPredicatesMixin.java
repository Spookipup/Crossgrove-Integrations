package com.crossgrove.integrations.mixin;

import com.crossgrove.integrations.gtceu.ThermalExchangerPatternSupport;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Predicates.class, remap = false)
public abstract class GtceuPredicatesMixin {
    @Inject(
            method = "autoAbilities([Lcom/gregtechceu/gtceu/api/recipe/GTRecipeType;ZZZZZZ)Lcom/gregtechceu/gtceu/api/pattern/TraceabilityPredicate;",
            at = @At("RETURN"),
            cancellable = true,
            require = 1
    )
    private static void crossgrove$allowThermalExchangerInRecipeAutoAbilities(GTRecipeType[] recipeTypes,
                                                                              boolean importItems,
                                                                              boolean exportItems,
                                                                              boolean importFluids,
                                                                              boolean exportFluids,
                                                                              boolean inputEnergy,
                                                                              boolean outputEnergy,
                                                                              CallbackInfoReturnable<TraceabilityPredicate> cir) {
        cir.setReturnValue(ThermalExchangerPatternSupport.withCrossgroveParts(cir.getReturnValue()));
    }

    @Inject(
            method = "autoAbilities(ZZZ)Lcom/gregtechceu/gtceu/api/pattern/TraceabilityPredicate;",
            at = @At("RETURN"),
            cancellable = true,
            require = 1
    )
    private static void crossgrove$allowThermalExchangerInBasicAutoAbilities(boolean checkEnergyIn,
                                                                             boolean checkMaintenance,
                                                                             boolean checkMuffler,
                                                                             CallbackInfoReturnable<TraceabilityPredicate> cir) {
        cir.setReturnValue(ThermalExchangerPatternSupport.withCrossgroveParts(cir.getReturnValue()));
    }
}
