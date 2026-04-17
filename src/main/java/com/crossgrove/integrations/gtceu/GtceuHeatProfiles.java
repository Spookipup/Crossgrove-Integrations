package com.crossgrove.integrations.gtceu;

import com.crossgrove.integrations.CrossgroveIntegrations;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Mod.EventBusSubscriber(modid = CrossgroveIntegrations.MOD_ID)
public final class GtceuHeatProfiles extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static final String DIRECTORY = "crossgrove_integrations/gtceu_heat_profiles";
    private static final List<GtceuHeatProfile> BUILTIN_PROFILES = List.of(
            GtceuHeatProfile.builtin(List.of("_electric_furnace"), 25D, 0.75D, 0.002D, 0.0005D, 800D, 1200D),
            GtceuHeatProfile.builtin(List.of("_alloy_smelter"), 30D, 0.85D, 0.002D, 0.00045D, 850D, 1250D),
            GtceuHeatProfile.builtin(List.of("_fluid_heater"), 25D, 1.5D, 0.003D, 0.00045D, 900D, 1300D),
            GtceuHeatProfile.builtin(List.of("_arc_furnace"), 35D, 2.5D, 0.004D, 0.00035D, 1000D, 1500D),
            GtceuHeatProfile.builtin(List.of("_thermal_centrifuge"), 40D, 1.25D, 0.003D, 0.0004D, 950D, 1400D)
    );
    private static final GtceuHeatProfile DEFAULT_PROFILE = GtceuHeatProfile.builtin(List.of(), 25D, 0.75D, 0.002D, 0.0005D, 800D, 1200D);
    private static volatile List<GtceuHeatProfile> profiles = BUILTIN_PROFILES;

    private GtceuHeatProfiles() {
        super(GSON, DIRECTORY);
    }

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new GtceuHeatProfiles());
    }

    public static boolean isProfiled(ResourceLocation blockId) {
        return profiles.stream().anyMatch(profile -> profile.matches(blockId));
    }

    public static GtceuHeatProfile get(ResourceLocation blockId) {
        return profiles.stream()
                .filter(profile -> profile.matches(blockId))
                .findFirst()
                .orElse(DEFAULT_PROFILE);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        List<GtceuHeatProfile> loaded = new ArrayList<>();
        for (var entry : jsons.entrySet()) {
            try {
                loaded.add(parseProfile(GsonHelper.convertToJsonObject(entry.getValue(), entry.getKey().toString())));
            } catch (RuntimeException exception) {
                CrossgroveIntegrations.LOGGER.warn("Failed loading GTCEu heat profile {}", entry.getKey(), exception);
            }
        }
        loaded.addAll(BUILTIN_PROFILES);
        loaded.sort(Comparator.comparingInt(profile -> profile.blocks().isEmpty() ? 1 : 0));
        profiles = List.copyOf(loaded);
        CrossgroveIntegrations.LOGGER.info("Loaded {} GTCEu heat profiles", profiles.size());
    }

    private static GtceuHeatProfile parseProfile(JsonObject json) {
        List<ResourceLocation> blocks = readResourceLocations(json, "blocks");
        List<String> suffixes = readStrings(json, "path_suffixes");
        double thermalMass = GsonHelper.getAsDouble(json, "thermal_mass", DEFAULT_PROFILE.thermalMass());
        double activeGain = GsonHelper.getAsDouble(json, "active_temperature_gain_per_tick", DEFAULT_PROFILE.activeTemperatureGainPerTick());
        double euGain = GsonHelper.getAsDouble(json, "temperature_gain_per_eut", DEFAULT_PROFILE.temperatureGainPerEUt());
        double passiveCooling = GsonHelper.getAsDouble(json, "passive_cooling_rate", DEFAULT_PROFILE.passiveCoolingRate());
        double minimumWorkingTemperature = GsonHelper.getAsDouble(json, "minimum_working_temperature", DEFAULT_PROFILE.minimumWorkingTemperature());
        double idealMinTemperature = GsonHelper.getAsDouble(json, "ideal_min_temperature", DEFAULT_PROFILE.idealMinTemperature());
        double idealMaxTemperature = GsonHelper.getAsDouble(json, "ideal_max_temperature", DEFAULT_PROFILE.idealMaxTemperature());
        boolean heatPowersMachine = GsonHelper.getAsBoolean(json, "heat_powers_machine", DEFAULT_PROFILE.heatPowersMachine());
        double minimumRotarySpeed = GsonHelper.getAsDouble(json, "minimum_rotary_speed", DEFAULT_PROFILE.minimumRotarySpeed());
        double idealMinRotarySpeed = GsonHelper.getAsDouble(json, "ideal_min_rotary_speed", DEFAULT_PROFILE.idealMinRotarySpeed());
        double idealMaxRotarySpeed = GsonHelper.getAsDouble(json, "ideal_max_rotary_speed", DEFAULT_PROFILE.idealMaxRotarySpeed());
        double rotaryEnergyPerTick = GsonHelper.getAsDouble(json, "rotary_energy_per_tick", DEFAULT_PROFILE.rotaryEnergyPerTick());
        double safeTemperature = GsonHelper.getAsDouble(json, "safe_temperature", DEFAULT_PROFILE.safeTemperature());
        double dangerTemperature = GsonHelper.getAsDouble(json, "danger_temperature", DEFAULT_PROFILE.dangerTemperature());
        return new GtceuHeatProfile(blocks, suffixes, thermalMass, activeGain, euGain, passiveCooling,
                minimumWorkingTemperature, idealMinTemperature, idealMaxTemperature, heatPowersMachine,
                minimumRotarySpeed, idealMinRotarySpeed, idealMaxRotarySpeed, rotaryEnergyPerTick,
                safeTemperature, dangerTemperature);
    }

    private static List<ResourceLocation> readResourceLocations(JsonObject json, String key) {
        return readStrings(json, key).stream()
                .map(ResourceLocation::tryParse)
                .filter(Objects::nonNull)
                .toList();
    }

    private static List<String> readStrings(JsonObject json, String key) {
        if (!json.has(key)) {
            return List.of();
        }
        JsonArray array = GsonHelper.getAsJsonArray(json, key);
        List<String> values = new ArrayList<>();
        for (JsonElement element : array) {
            values.add(GsonHelper.convertToString(element, key));
        }
        return List.copyOf(values);
    }
}
