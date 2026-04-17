package com.crossgrove.integrations.crop;

import com.crossgrove.integrations.CrossgroveConfig;
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
public final class CropHeatProfiles extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static final String DIRECTORY = "crossgrove_integrations/crop_heat_profiles";
    private static final List<CropHeatProfile> BUILTIN_PROFILES = List.of(
            CropHeatProfile.builtin(resources(
                    "minecraft:nether_wart",
                    "minecraft:cactus",
                    "mysticalagriculture:fire_crop",
                    "mysticalagriculture:blaze_crop",
                    "mysticalagriculture:nether_crop"
            ), List.of(), 10D, 28D, 48D, 70D, 1D, 0.7D),
            CropHeatProfile.builtin(resources(
                    "minecraft:sweet_berry_bush",
                    "mysticalagriculture:ice_crop",
                    "mysticalagriculture:blizz_crop"
            ), List.of(), -15D, 0D, 18D, 35D, 1D, 0.7D),
            CropHeatProfile.builtin(resources(
                    "farmersdelight:cabbages"
            ), List.of(), -2D, 10D, 24D, 38D, 1D, 1D),
            CropHeatProfile.builtin(resources(
                    "farmersdelight:tomatoes",
                    "farmersdelight:budding_tomatoes"
            ), List.of(), 8D, 22D, 34D, 48D, 1D, 1D),
            CropHeatProfile.builtin(resources(
                    "farmersdelight:rice",
                    "farmersdelight:rice_panicles"
            ), List.of(), 12D, 24D, 36D, 50D, 1D, 1D)
    );
    private static volatile List<CropHeatProfile> profiles = BUILTIN_PROFILES;

    private CropHeatProfiles() {
        super(GSON, DIRECTORY);
    }

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new CropHeatProfiles());
    }

    public static CropHeatProfile get(ResourceLocation blockId) {
        return profiles.stream()
                .filter(profile -> profile.matches(blockId))
                .findFirst()
                .orElseGet(CropHeatProfiles::defaultProfile);
    }

    public static CropHeatProfile getPlant(ResourceLocation plantId) {
        return profiles.stream()
                .filter(profile -> profile.matchesPlant(plantId))
                .findFirst()
                .orElseGet(CropHeatProfiles::defaultProfile);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        List<CropHeatProfile> loaded = new ArrayList<>(BUILTIN_PROFILES);
        for (var entry : jsons.entrySet()) {
            try {
                JsonElement json = entry.getValue();
                if (json.isJsonArray()) {
                    JsonArray profilesJson = GsonHelper.convertToJsonArray(json, entry.getKey().toString());
                    for (JsonElement profileJson : profilesJson) {
                        loaded.add(parseProfile(GsonHelper.convertToJsonObject(profileJson, entry.getKey().toString())));
                    }
                } else {
                    loaded.add(parseProfile(GsonHelper.convertToJsonObject(json, entry.getKey().toString())));
                }
            } catch (RuntimeException exception) {
                CrossgroveIntegrations.LOGGER.warn("Failed loading crop heat profile {}", entry.getKey(), exception);
            }
        }
        // specific entries need to win over broad suffix catches
        loaded.sort(Comparator.comparingInt(CropHeatProfiles::specificity).reversed());
        profiles = List.copyOf(loaded);
        CrossgroveIntegrations.LOGGER.info("Loaded {} crop heat profiles", profiles.size());
    }

    private static CropHeatProfile parseProfile(JsonObject json) {
        return new CropHeatProfile(
                readResourceLocations(json, "blocks"),
                readResourceLocations(json, "plants"),
                readStrings(json, "path_suffixes"),
                GsonHelper.getAsDouble(json, "too_cold", CrossgroveConfig.cropTooColdC),
                GsonHelper.getAsDouble(json, "ideal_min", CrossgroveConfig.cropIdealMinC),
                GsonHelper.getAsDouble(json, "ideal_max", CrossgroveConfig.cropIdealMaxC),
                GsonHelper.getAsDouble(json, "too_hot", CrossgroveConfig.cropTooHotC),
                GsonHelper.getAsDouble(json, "boost_multiplier", 1D),
                GsonHelper.getAsDouble(json, "stress_regression_multiplier", 1D)
        );
    }

    static CropHeatProfile defaultProfile() {
        return new CropHeatProfile(List.of(), List.of(), List.of(),
                CrossgroveConfig.cropTooColdC,
                CrossgroveConfig.cropIdealMinC,
                CrossgroveConfig.cropIdealMaxC,
                CrossgroveConfig.cropTooHotC,
                1D,
                1D);
    }

    private static int specificity(CropHeatProfile profile) {
        return profile.blocks().size() * 2 + profile.plants().size() * 2 + profile.pathSuffixes().size();
    }

    private static List<ResourceLocation> resources(String... ids) {
        List<ResourceLocation> resources = new ArrayList<>();
        for (String id : ids) {
            ResourceLocation resource = ResourceLocation.tryParse(id);
            if (resource != null) {
                resources.add(resource);
            }
        }
        return List.copyOf(resources);
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
