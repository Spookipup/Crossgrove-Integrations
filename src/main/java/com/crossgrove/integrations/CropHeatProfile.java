package com.crossgrove.integrations;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record CropHeatProfile(
        List<ResourceLocation> blocks,
        List<ResourceLocation> plants,
        List<String> pathSuffixes,
        double tooCold,
        double idealMin,
        double idealMax,
        double tooHot,
        double boostMultiplier,
        double stressRegressionMultiplier
) {
    public static CropHeatProfile builtin(List<ResourceLocation> blocks, List<String> pathSuffixes,
                                          double tooCold, double idealMin, double idealMax, double tooHot,
                                          double boostMultiplier, double stressRegressionMultiplier) {
        return new CropHeatProfile(blocks, List.of(), pathSuffixes, tooCold, idealMin, idealMax, tooHot,
                boostMultiplier, stressRegressionMultiplier);
    }

    public boolean matches(ResourceLocation blockId) {
        if (blocks.contains(blockId)) {
            return true;
        }
        String path = blockId.getPath();
        return pathSuffixes.stream().anyMatch(path::endsWith);
    }

    public boolean matchesPlant(ResourceLocation plantId) {
        return plants.contains(plantId);
    }
}
