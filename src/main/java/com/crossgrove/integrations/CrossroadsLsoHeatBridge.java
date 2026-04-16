package com.crossgrove.integrations;

import com.Da_Technomancer.crossroads.api.Capabilities;
import com.Da_Technomancer.crossroads.api.heat.HeatUtil;
import com.Da_Technomancer.crossroads.api.heat.IHeatHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class CrossroadsLsoHeatBridge {
    private CrossroadsLsoHeatBridge() {
    }

    public static float getInfluence(Level level, BlockPos pos) {
        if (!CrossgroveConfig.enableLsoCrossroadsHeatBridge || Capabilities.HEAT_CAPABILITY == null) {
            return 0F;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return 0F;
        }

        return blockEntity.getCapability(Capabilities.HEAT_CAPABILITY, null)
                .resolve()
                .map(heatHandler -> convertToLsoInfluence(level, pos, heatHandler))
                .orElse(0F);
    }

    private static float convertToLsoInfluence(Level level, BlockPos pos, IHeatHandler heatHandler) {
        double ambient = HeatUtil.convertBiomeTemp(level, pos);
        double delta = heatHandler.getTemp() - ambient;
        double magnitude = Math.abs(delta);
        double deadband = Math.max(0D, CrossgroveConfig.lsoAmbientDeadbandC);
        if (magnitude <= deadband) {
            return 0F;
        }

        double cap = delta < 0D
                ? Math.max(0D, CrossgroveConfig.lsoMaximumColdInfluence)
                : Math.max(0D, CrossgroveConfig.lsoMaximumHeatInfluence);
        double influence = interpolateLsoMagnitude(magnitude - deadband, cap);
        return (float) (delta < 0D ? -influence : influence);
    }

    private static double interpolateLsoMagnitude(double adjustedDelta, double maximumInfluence) {
        // lso gets touchy at high temps, so ease into the cap
        if (adjustedDelta <= 45D) {
            return lerp(0D, 1.5D, adjustedDelta / 45D);
        }
        if (adjustedDelta <= 195D) {
            return lerp(1.5D, 6D, (adjustedDelta - 45D) / 150D);
        }
        if (adjustedDelta <= 595D) {
            return lerp(6D, 10D, (adjustedDelta - 195D) / 400D);
        }
        if (adjustedDelta <= 995D) {
            return lerp(10D, maximumInfluence, (adjustedDelta - 595D) / 400D);
        }
        return maximumInfluence;
    }

    public static float clampForSign(float influence) {
        if (influence > 0F) {
            return (float) Math.min(influence, Math.max(0D, CrossgroveConfig.lsoMaximumHeatInfluence));
        }
        if (influence < 0F) {
            return (float) Math.max(influence, -Math.max(0D, CrossgroveConfig.lsoMaximumColdInfluence));
        }
        return 0F;
    }

    private static double lerp(double from, double to, double progress) {
        return from + (to - from) * Math.max(0D, Math.min(1D, progress));
    }
}
