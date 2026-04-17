package com.crossgrove.integrations.crop;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.plant.AgriPlant;
import com.agricraft.agricraft.api.requirement.AgriGrowthConditionRegistry;
import com.agricraft.agricraft.api.requirement.AgriGrowthResponse;
import com.crossgrove.integrations.CrossgroveConfig;
import com.crossgrove.integrations.CrossgroveIntegrations;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class AgriCraftHeatBridge {
    private static final String CONDITION_ID = "crossgrove_heat";
    private static boolean registered;

    private AgriCraftHeatBridge() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        AgriGrowthConditionRegistry.BaseGrowthCondition<Double> condition =
                new AgriGrowthConditionRegistry.BaseGrowthCondition<>(
                        CONDITION_ID,
                        AgriCraftHeatBridge::checkTemperature,
                        AgriCraftHeatBridge::getEffectiveTemperature
                );
        if (AgriApi.getGrowthConditionRegistry().add(condition)) {
            CrossgroveIntegrations.LOGGER.info("Registered AgriCraft heat growth condition");
        }
    }

    private static Double getEffectiveTemperature(Level level, BlockPos pos) {
        return CropHeatBridge.getEffectiveCropTemperature(level, pos);
    }

    private static AgriGrowthResponse checkTemperature(AgriPlant plant, int strength, Double temperature) {
        if (!CrossgroveConfig.enableCropHeatBridge || !CrossgroveConfig.enableAgriCraftHeatBridge) {
            return AgriGrowthResponse.FERTILE;
        }

        CropHeatProfile profile = AgriApi.getPlantId(plant)
                .map(AgriCraftHeatBridge::profileForPlant)
                .orElseGet(CropHeatProfiles::defaultProfile);
        double tolerance = Math.max(0, strength - 1) * CrossgroveConfig.agriCraftStrengthToleranceC;
        if (temperature <= profile.tooCold() - tolerance || temperature >= profile.tooHot() + tolerance) {
            return CrossgroveConfig.agriCraftHeatStressIsLethal ? AgriGrowthResponse.LETHAL : AgriGrowthResponse.INFERTILE;
        }
        if (temperature < profile.tooCold() || temperature > profile.tooHot()) {
            return AgriGrowthResponse.INFERTILE;
        }
        return AgriGrowthResponse.FERTILE;
    }

    private static CropHeatProfile profileForPlant(ResourceLocation plantId) {
        return CropHeatProfiles.getPlant(plantId);
    }

    public static Component conditionDescription() {
        return Component.literal("This plant can not grow at the current heat level");
    }
}
