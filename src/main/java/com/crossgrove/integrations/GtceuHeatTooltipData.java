package com.crossgrove.integrations;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Objects;

public final class GtceuHeatTooltipData {
    public static final ResourceLocation UID = Objects.requireNonNull(
            ResourceLocation.tryParse(CrossgroveIntegrations.MOD_ID + ":gtceu_heat")
    );
    public static final String HAS_HEAT_KEY = CrossgroveIntegrations.MOD_ID + "_has_heat";
    public static final String TEMPERATURE_KEY = CrossgroveIntegrations.MOD_ID + "_temperature";
    public static final String HEAT_SUSPENDED_KEY = CrossgroveIntegrations.MOD_ID + "_heat_suspended";
    public static final String SAFE_TEMPERATURE_KEY = CrossgroveIntegrations.MOD_ID + "_safe_temperature";
    public static final String DANGER_TEMPERATURE_KEY = CrossgroveIntegrations.MOD_ID + "_danger_temperature";

    private GtceuHeatTooltipData() {
    }

    public static void write(CompoundTag data, double temperature, boolean heatSuspended,
                             double safeTemperature, double dangerTemperature) {
        data.putBoolean(HAS_HEAT_KEY, true);
        data.putDouble(TEMPERATURE_KEY, temperature);
        data.putBoolean(HEAT_SUSPENDED_KEY, heatSuspended);
        if (Double.isFinite(safeTemperature)) {
            data.putDouble(SAFE_TEMPERATURE_KEY, safeTemperature);
        }
        if (Double.isFinite(dangerTemperature)) {
            data.putDouble(DANGER_TEMPERATURE_KEY, dangerTemperature);
        }
    }

    public static boolean hasHeat(CompoundTag data) {
        return data.getBoolean(HAS_HEAT_KEY);
    }

    public static boolean isHeatSuspended(CompoundTag data) {
        return data.getBoolean(HEAT_SUSPENDED_KEY);
    }

    public static boolean hasSafeTemperature(CompoundTag data) {
        return data.contains(SAFE_TEMPERATURE_KEY);
    }

    public static double getTemperature(CompoundTag data) {
        return data.getDouble(TEMPERATURE_KEY);
    }

    public static double getSafeTemperature(CompoundTag data) {
        return data.getDouble(SAFE_TEMPERATURE_KEY);
    }

    public static String formatTemperature(double temperature) {
        if (Math.abs(temperature) >= 1000D) {
            return Long.toString(Math.round(temperature));
        }
        return String.format(Locale.ROOT, "%.1f", temperature);
    }
}
