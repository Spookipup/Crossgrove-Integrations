package com.crossgrove.integrations.mixin;

import com.crossgrove.integrations.gtceu.ThermalExchangerPatternSupport;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.predicates.PredicateBlocks;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(value = PredicateBlocks.class, remap = false)
public abstract class GtceuPredicateBlocksMixin extends SimplePredicate {
    @Shadow
    public Block[] blocks;

    @Inject(method = "buildPredicate", at = @At("RETURN"), require = 1)
    private void crossgrove$allowThermalExchangerAsCasing(CallbackInfoReturnable<SimplePredicate> cir) {
        if (!ThermalExchangerPatternSupport.containsGtceuCasing(blocks)) {
            return;
        }
        ThermalExchangerPatternSupport.logCasingHook();
        Predicate<MultiblockState> original = predicate;
        predicate = state -> original.test(state) || ThermalExchangerPatternSupport.isThermalExchangerHatch(state);
    }
}
