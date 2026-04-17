package com.crossgrove.integrations.gtceu;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record GtceuHeatProfile(
        List<ResourceLocation> blocks,
        List<String> pathSuffixes,
        double thermalMass,
        double activeTemperatureGainPerTick,
        double temperatureGainPerEUt,
        double passiveCoolingRate,
        double minimumWorkingTemperature,
        double idealMinTemperature,
        double idealMaxTemperature,
        boolean heatPowersMachine,
        double minimumRotarySpeed,
        double idealMinRotarySpeed,
        double idealMaxRotarySpeed,
        double rotaryEnergyPerTick,
        double safeTemperature,
        double dangerTemperature
) {
    public static GtceuHeatProfile builtin(List<String> suffixes, double thermalMass, double activeTemperatureGainPerTick,
                                           double temperatureGainPerEUt, double passiveCoolingRate,
                                           double safeTemperature, double dangerTemperature) {
        return new GtceuHeatProfile(List.of(), suffixes, thermalMass, activeTemperatureGainPerTick,
                temperatureGainPerEUt, passiveCoolingRate, Double.NaN, Double.NaN, Double.NaN,
                false, Double.NaN, Double.NaN, Double.NaN, 0D,
                safeTemperature, dangerTemperature);
    }

    public boolean requiresWorkingHeat() {
        return Double.isFinite(minimumWorkingTemperature);
    }

    public boolean requiresRotaryPower() {
        return Double.isFinite(minimumRotarySpeed);
    }

    public boolean matches(ResourceLocation blockId) {
        if (blocks.contains(blockId)) {
            return true;
        }
        String path = blockId.getPath();
        return pathSuffixes.stream().anyMatch(path::endsWith);
    }

    public GtceuHeatProfile withBlocks(List<ResourceLocation> blocks) {
        return new GtceuHeatProfile(blocks, pathSuffixes, thermalMass, activeTemperatureGainPerTick,
                temperatureGainPerEUt, passiveCoolingRate, minimumWorkingTemperature, idealMinTemperature,
                idealMaxTemperature, heatPowersMachine, minimumRotarySpeed, idealMinRotarySpeed,
                idealMaxRotarySpeed, rotaryEnergyPerTick, safeTemperature, dangerTemperature);
    }
}
