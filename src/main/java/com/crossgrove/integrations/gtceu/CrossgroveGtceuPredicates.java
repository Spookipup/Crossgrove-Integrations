package com.crossgrove.integrations.gtceu;

import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import net.minecraft.network.chat.Component;

public final class CrossgroveGtceuPredicates {
    private CrossgroveGtceuPredicates() {
    }

    public static TraceabilityPredicate thermalExchangerHatch() {
        return hatchPredicate(
                CrossgroveGtceuPartAbilities.THERMAL_EXCHANGER,
                "Thermal Exchanger Hatch"
        );
    }

    public static TraceabilityPredicate thermalExchangerHatch(int minGlobal) {
        return requireCount(thermalExchangerHatch(), minGlobal);
    }

    public static TraceabilityPredicate requireHeatPower() {
        return thermalExchangerHatch(1)
                .addTooltips(Component.literal("Requires at least one Thermal Exchanger Hatch in the structure."));
    }

    public static TraceabilityPredicate rotaryInputHatch() {
        return hatchPredicate(
                CrossgroveGtceuPartAbilities.ROTARY_INPUT,
                "Rotary Input Hatch"
        );
    }

    public static TraceabilityPredicate rotaryInputHatch(int minGlobal) {
        return requireCount(rotaryInputHatch(), minGlobal);
    }

    public static TraceabilityPredicate requireRotaryPower() {
        return rotaryInputHatch(1)
                .addTooltips(Component.literal("Requires at least one Rotary Input Hatch in the structure."));
    }

    public static TraceabilityPredicate rotaryOutputHatch() {
        return hatchPredicate(
                CrossgroveGtceuPartAbilities.ROTARY_OUTPUT,
                "Rotary Output Hatch"
        );
    }

    public static TraceabilityPredicate rotaryOutputHatch(int minGlobal) {
        return requireCount(rotaryOutputHatch(), minGlobal);
    }

    public static TraceabilityPredicate requireRotaryOutput() {
        return rotaryOutputHatch(1)
                .addTooltips(Component.literal("Requires at least one Rotary Output Hatch in the structure."));
    }

    private static TraceabilityPredicate hatchPredicate(PartAbility ability, String name) {
        return Predicates.ability(ability)
                .setPreviewCount(1)
                .addTooltips(Component.literal(name));
    }

    private static TraceabilityPredicate requireCount(TraceabilityPredicate predicate, int minGlobal) {
        return predicate.setMinGlobalLimited(Math.max(1, minGlobal));
    }
}
