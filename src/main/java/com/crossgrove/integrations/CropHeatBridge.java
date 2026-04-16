package com.crossgrove.integrations;

import com.Da_Technomancer.crossroads.api.Capabilities;
import com.Da_Technomancer.crossroads.api.heat.HeatUtil;
import com.Da_Technomancer.crossroads.api.heat.IHeatHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class CropHeatBridge {
    private CropHeatBridge() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCropGrowthAttempt(BlockEvent.CropGrowEvent.Pre event) {
        if (!CrossgroveConfig.enableCropHeatBridge) {
            return;
        }

        LevelAccessor levelAccessor = event.getLevel();
        if (!(levelAccessor instanceof Level level) || level.isClientSide()) {
            return;
        }

        double cropTemperature = getEffectiveCropTemperature(level, event.getPos());
        CropHeatProfile profile = CropHeatProfiles.get(getBlockId(event.getState()));
        double growthQuality = getGrowthQuality(cropTemperature, profile);
        if (growthQuality <= 0D) {
            event.setResult(Event.Result.DENY);
            tryStressRegression(level, event.getPos(), event.getState(), cropTemperature, profile);
            return;
        }

        RandomSource random = level.getRandom();
        if (growthQuality < 1D && random.nextDouble() > growthQuality) {
            event.setResult(Event.Result.DENY);
            return;
        }

        if (CrossgroveConfig.cropEnableGrowthBoost
                && event.getResult() != Event.Result.DENY
                && random.nextDouble() < CrossgroveConfig.cropMaximumBoostChance * profile.boostMultiplier() * growthQuality) {
            event.setResult(Event.Result.ALLOW);
        }
    }

    public static double getEffectiveCropTemperature(Level level, BlockPos cropPos) {
        double ambient = HeatUtil.convertBiomeTemp(level, cropPos);
        if (Capabilities.HEAT_CAPABILITY == null || CrossgroveConfig.cropHeatScanRadius <= 0) {
            return ambient;
        }

        int radius = CrossgroveConfig.cropHeatScanRadius;
        double netShift = 0D;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        // nearby heat should nudge crops, not replace the biome outright
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double distanceSquared = x * x + y * y + z * z;
                    if (distanceSquared <= 0D || distanceSquared > radius * radius) {
                        continue;
                    }

                    cursor.set(cropPos.getX() + x, cropPos.getY() + y, cropPos.getZ() + z);
                    BlockEntity blockEntity = level.getBlockEntity(cursor);
                    if (blockEntity == null) {
                        continue;
                    }
                    netShift += getSourceShift(level, cursor, blockEntity, ambient, distanceSquared);
                }
            }
        }

        double maximumShift = Math.max(0D, CrossgroveConfig.cropHeatMaximumTemperatureShiftC);
        return ambient + clamp(netShift, -maximumShift, maximumShift);
    }

    private static double getSourceShift(Level level, BlockPos sourcePos, BlockEntity blockEntity,
                                         double cropAmbient, double distanceSquared) {
        return blockEntity.getCapability(Capabilities.HEAT_CAPABILITY, null)
                .resolve()
                .map(heatHandler -> calculateSourceShift(level, sourcePos, heatHandler, cropAmbient, distanceSquared))
                .orElse(0D);
    }

    private static double calculateSourceShift(Level level, BlockPos sourcePos, IHeatHandler heatHandler,
                                               double cropAmbient, double distanceSquared) {
        double sourceAmbient = HeatUtil.convertBiomeTemp(level, sourcePos);
        double sourceDelta = heatHandler.getTemp() - sourceAmbient;
        double cappedDelta = clamp(sourceDelta,
                -CrossgroveConfig.cropHeatSourceDeltaCapC,
                CrossgroveConfig.cropHeatSourceDeltaCapC);
        double ambientDifference = sourceAmbient - cropAmbient;
        double falloff = 1D / (distanceSquared + 1D);
        return (cappedDelta + ambientDifference) * falloff * CrossgroveConfig.cropHeatSourceCoupling;
    }

    private static double getGrowthQuality(double cropTemperature, CropHeatProfile profile) {
        double tooCold = profile.tooCold();
        double idealMin = Math.max(tooCold, profile.idealMin());
        double idealMax = Math.max(idealMin, profile.idealMax());
        double tooHot = Math.max(idealMax, profile.tooHot());

        if (cropTemperature <= tooCold || cropTemperature >= tooHot) {
            return 0D;
        }
        if (cropTemperature >= idealMin && cropTemperature <= idealMax) {
            return 1D;
        }
        if (cropTemperature < idealMin) {
            return (cropTemperature - tooCold) / (idealMin - tooCold);
        }
        return (tooHot - cropTemperature) / (tooHot - idealMax);
    }

    private static void tryStressRegression(Level level, BlockPos pos, BlockState state, double cropTemperature,
                                            CropHeatProfile profile) {
        if (!CrossgroveConfig.cropEnableStressRegression
                || level.getRandom().nextDouble() >= CrossgroveConfig.cropStressRegressionChance * profile.stressRegressionMultiplier()) {
            return;
        }

        IntegerProperty ageProperty = getAgeProperty(state);
        if (ageProperty == null) {
            return;
        }

        int currentAge = state.getValue(ageProperty);
        int minimumAge = ageProperty.getPossibleValues().stream().mapToInt(Integer::intValue).min().orElse(0);
        if (currentAge <= minimumAge) {
            return;
        }

        BlockState regressedState = state.setValue(ageProperty, currentAge - 1);
        level.setBlock(pos, regressedState, 2);
        CrossgroveIntegrations.LOGGER.debug("Regressed crop at {} from heat stress temp={}C", pos, cropTemperature);
    }

    private static IntegerProperty getAgeProperty(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty integerProperty && "age".equals(property.getName())) {
                return integerProperty;
            }
        }
        return null;
    }

    private static ResourceLocation getBlockId(BlockState state) {
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return blockId == null ? ResourceLocation.tryParse("minecraft:air") : blockId;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
