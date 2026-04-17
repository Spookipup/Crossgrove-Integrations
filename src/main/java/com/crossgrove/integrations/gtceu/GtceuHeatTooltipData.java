package com.crossgrove.integrations.gtceu;

import com.crossgrove.integrations.CrossgroveIntegrations;
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
    public static final String HEAT_SUSPENSION_REASON_KEY = CrossgroveIntegrations.MOD_ID + "_heat_suspension_reason";
    public static final String HEAT_POWERS_MACHINE_KEY = CrossgroveIntegrations.MOD_ID + "_heat_powers_machine";
    public static final String MINIMUM_WORKING_TEMPERATURE_KEY = CrossgroveIntegrations.MOD_ID + "_minimum_working_temperature";
    public static final String IDEAL_MIN_TEMPERATURE_KEY = CrossgroveIntegrations.MOD_ID + "_ideal_min_temperature";
    public static final String IDEAL_MAX_TEMPERATURE_KEY = CrossgroveIntegrations.MOD_ID + "_ideal_max_temperature";
    public static final String MINIMUM_ROTARY_SPEED_KEY = CrossgroveIntegrations.MOD_ID + "_minimum_rotary_speed";
    public static final String IDEAL_MIN_ROTARY_SPEED_KEY = CrossgroveIntegrations.MOD_ID + "_ideal_min_rotary_speed";
    public static final String IDEAL_MAX_ROTARY_SPEED_KEY = CrossgroveIntegrations.MOD_ID + "_ideal_max_rotary_speed";
    public static final String ROTARY_SPEED_KEY = CrossgroveIntegrations.MOD_ID + "_rotary_speed";
    public static final String ROTARY_ENERGY_KEY = CrossgroveIntegrations.MOD_ID + "_rotary_energy";
    public static final String SAFE_TEMPERATURE_KEY = CrossgroveIntegrations.MOD_ID + "_safe_temperature";
    public static final String DANGER_TEMPERATURE_KEY = CrossgroveIntegrations.MOD_ID + "_danger_temperature";
    public static final String SHARED_MULTIBLOCK_HEAT_KEY = CrossgroveIntegrations.MOD_ID + "_shared_multiblock_heat";
    public static final String MULTIBLOCK_HEAT_PARTS_KEY = CrossgroveIntegrations.MOD_ID + "_multiblock_heat_parts";
    public static final String MULTIBLOCK_STRUCTURE_BLOCKS_KEY = CrossgroveIntegrations.MOD_ID + "_multiblock_structure_blocks";

    private GtceuHeatTooltipData() {
    }

    public static void write(CompoundTag data, double temperature, boolean heatSuspended, String suspensionReason,
                             boolean heatPowersMachine,
                             double minimumWorkingTemperature, double idealMinTemperature, double idealMaxTemperature,
                             double minimumRotarySpeed, double idealMinRotarySpeed, double idealMaxRotarySpeed,
                             double rotarySpeed, double rotaryEnergy,
                             double safeTemperature, double dangerTemperature,
                             boolean sharedMultiblockHeat, int multiblockHeatParts, int multiblockStructureBlocks) {
        data.putBoolean(HAS_HEAT_KEY, true);
        data.putDouble(TEMPERATURE_KEY, temperature);
        data.putBoolean(HEAT_SUSPENDED_KEY, heatSuspended);
        data.putString(HEAT_SUSPENSION_REASON_KEY, suspensionReason);
        data.putBoolean(HEAT_POWERS_MACHINE_KEY, heatPowersMachine);
        if (Double.isFinite(minimumWorkingTemperature)) {
            data.putDouble(MINIMUM_WORKING_TEMPERATURE_KEY, minimumWorkingTemperature);
        }
        if (Double.isFinite(idealMinTemperature)) {
            data.putDouble(IDEAL_MIN_TEMPERATURE_KEY, idealMinTemperature);
        }
        if (Double.isFinite(idealMaxTemperature)) {
            data.putDouble(IDEAL_MAX_TEMPERATURE_KEY, idealMaxTemperature);
        }
        if (Double.isFinite(minimumRotarySpeed)) {
            data.putDouble(MINIMUM_ROTARY_SPEED_KEY, minimumRotarySpeed);
        }
        if (Double.isFinite(idealMinRotarySpeed)) {
            data.putDouble(IDEAL_MIN_ROTARY_SPEED_KEY, idealMinRotarySpeed);
        }
        if (Double.isFinite(idealMaxRotarySpeed)) {
            data.putDouble(IDEAL_MAX_ROTARY_SPEED_KEY, idealMaxRotarySpeed);
        }
        data.putDouble(ROTARY_SPEED_KEY, rotarySpeed);
        data.putDouble(ROTARY_ENERGY_KEY, rotaryEnergy);
        if (Double.isFinite(safeTemperature)) {
            data.putDouble(SAFE_TEMPERATURE_KEY, safeTemperature);
        }
        if (Double.isFinite(dangerTemperature)) {
            data.putDouble(DANGER_TEMPERATURE_KEY, dangerTemperature);
        }
        data.putBoolean(SHARED_MULTIBLOCK_HEAT_KEY, sharedMultiblockHeat);
        if (sharedMultiblockHeat) {
            data.putInt(MULTIBLOCK_HEAT_PARTS_KEY, Math.max(1, multiblockHeatParts));
            data.putInt(MULTIBLOCK_STRUCTURE_BLOCKS_KEY, Math.max(1, multiblockStructureBlocks));
        }
    }

    public static boolean hasHeat(CompoundTag data) {
        return data.getBoolean(HAS_HEAT_KEY);
    }

    public static boolean isHeatSuspended(CompoundTag data) {
        return data.getBoolean(HEAT_SUSPENDED_KEY);
    }

    public static String getHeatSuspensionReason(CompoundTag data) {
        return data.getString(HEAT_SUSPENSION_REASON_KEY);
    }

    public static boolean heatPowersMachine(CompoundTag data) {
        return data.getBoolean(HEAT_POWERS_MACHINE_KEY);
    }

    public static boolean hasMinimumWorkingTemperature(CompoundTag data) {
        return data.contains(MINIMUM_WORKING_TEMPERATURE_KEY);
    }

    public static boolean hasIdealTemperatureRange(CompoundTag data) {
        return data.contains(IDEAL_MIN_TEMPERATURE_KEY) && data.contains(IDEAL_MAX_TEMPERATURE_KEY);
    }

    public static boolean hasMinimumRotarySpeed(CompoundTag data) {
        return data.contains(MINIMUM_ROTARY_SPEED_KEY);
    }

    public static boolean hasIdealRotarySpeedRange(CompoundTag data) {
        return data.contains(IDEAL_MIN_ROTARY_SPEED_KEY) && data.contains(IDEAL_MAX_ROTARY_SPEED_KEY);
    }

    public static boolean hasSafeTemperature(CompoundTag data) {
        return data.contains(SAFE_TEMPERATURE_KEY);
    }

    public static boolean hasSharedMultiblockHeat(CompoundTag data) {
        return data.getBoolean(SHARED_MULTIBLOCK_HEAT_KEY);
    }

    public static double getTemperature(CompoundTag data) {
        return data.getDouble(TEMPERATURE_KEY);
    }

    public static double getSafeTemperature(CompoundTag data) {
        return data.getDouble(SAFE_TEMPERATURE_KEY);
    }

    public static double getDangerTemperature(CompoundTag data) {
        return data.getDouble(DANGER_TEMPERATURE_KEY);
    }

    public static int getMultiblockHeatParts(CompoundTag data) {
        return data.getInt(MULTIBLOCK_HEAT_PARTS_KEY);
    }

    public static int getMultiblockStructureBlocks(CompoundTag data) {
        return data.getInt(MULTIBLOCK_STRUCTURE_BLOCKS_KEY);
    }

    public static double getMinimumWorkingTemperature(CompoundTag data) {
        return data.getDouble(MINIMUM_WORKING_TEMPERATURE_KEY);
    }

    public static double getIdealMinTemperature(CompoundTag data) {
        return data.getDouble(IDEAL_MIN_TEMPERATURE_KEY);
    }

    public static double getIdealMaxTemperature(CompoundTag data) {
        return data.getDouble(IDEAL_MAX_TEMPERATURE_KEY);
    }

    public static double getMinimumRotarySpeed(CompoundTag data) {
        return data.getDouble(MINIMUM_ROTARY_SPEED_KEY);
    }

    public static double getIdealMinRotarySpeed(CompoundTag data) {
        return data.getDouble(IDEAL_MIN_ROTARY_SPEED_KEY);
    }

    public static double getIdealMaxRotarySpeed(CompoundTag data) {
        return data.getDouble(IDEAL_MAX_ROTARY_SPEED_KEY);
    }

    public static double getRotarySpeed(CompoundTag data) {
        return data.getDouble(ROTARY_SPEED_KEY);
    }

    public static double getRotaryEnergy(CompoundTag data) {
        return data.getDouble(ROTARY_ENERGY_KEY);
    }

    public static String formatTemperature(double temperature) {
        if (Math.abs(temperature) >= 1000D) {
            return Long.toString(Math.round(temperature));
        }
        return String.format(Locale.ROOT, "%.1f", temperature);
    }
}
