package com.crossgrove.integrations;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record GtceuHeatProfile(
        List<ResourceLocation> blocks,
        List<String> pathSuffixes,
        double thermalMass,
        double activeTemperatureGainPerTick,
        double temperatureGainPerEUt,
        double passiveCoolingRate,
        double safeTemperature,
        double dangerTemperature
) {
    public static GtceuHeatProfile builtin(List<String> suffixes, double thermalMass, double activeTemperatureGainPerTick,
                                           double temperatureGainPerEUt, double passiveCoolingRate,
                                           double safeTemperature, double dangerTemperature) {
        return new GtceuHeatProfile(List.of(), suffixes, thermalMass, activeTemperatureGainPerTick,
                temperatureGainPerEUt, passiveCoolingRate, safeTemperature, dangerTemperature);
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
                temperatureGainPerEUt, passiveCoolingRate, safeTemperature, dangerTemperature);
    }
}
