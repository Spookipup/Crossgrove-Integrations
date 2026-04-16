package com.crossgrove.integrations;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = CrossgroveIntegrations.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CrossgroveConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ENABLE_GTCEU_HEAT_BRIDGE = BUILDER
            .comment("Enables Crossroads heat capability attachment on selected GregTech CEu machines.")
            .define("gtceuHeatBridge.enabled", true);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENABLED_GTCEU_BLOCKS = BUILDER
            .comment("Exact GTCEu block IDs that should expose Crossroads heat, for example gtceu:lv_electric_furnace.")
            .defineListAllowEmpty("gtceuHeatBridge.enabledBlocks", List.of(), CrossgroveConfig::isResourceLocation);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENABLED_GTCEU_PATH_SUFFIXES = BUILDER
            .comment("GTCEu block path suffixes that should expose Crossroads heat. This keeps tiered machines selectable without listing every voltage tier.")
            .defineListAllowEmpty("gtceuHeatBridge.enabledPathSuffixes", List.of(
                    "_electric_furnace",
                    "_alloy_smelter",
                    "_arc_furnace",
                    "_fluid_heater",
                    "_thermal_centrifuge"
            ), CrossgroveConfig::isPathSuffix);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISABLED_GTCEU_BLOCKS = BUILDER
            .comment("Exact GTCEu block IDs that should never expose Crossroads heat, even if matched by a suffix.")
            .defineListAllowEmpty("gtceuHeatBridge.disabledBlocks", List.of(), CrossgroveConfig::isResourceLocation);

    private static final ForgeConfigSpec.DoubleValue AMBIENT_TEMPERATURE_C = BUILDER
            .comment("Fallback initial temperature in degrees Celsius when biome temperature cannot be read.")
            .defineInRange("gtceuHeatBridge.fallbackAmbientTemperatureC", 20D, -273D, 1_000_000D);

    private static final ForgeConfigSpec.DoubleValue THERMAL_MASS = BUILDER
            .comment("Multiplier applied to incoming heat changes. 1 behaves like a small Crossroads heat machine; 200 behaves like a heat reservoir.")
            .defineInRange("gtceuHeatBridge.thermalMass", 50D, 1D, 1_000_000D);

    private static final ForgeConfigSpec.DoubleValue PASSIVE_COOLING_RATE = BUILDER
            .comment("Fraction of the temperature difference between a GTCEu machine and biome ambient moved per tick. Crossroads heat cables still do the real heat transport; this only prevents orphaned machines from staying hot forever.")
            .defineInRange("gtceuHeatBridge.passiveCoolingRate", 0.0005D, 0D, 1D);

    private static final ForgeConfigSpec.DoubleValue WASTE_HEAT_PER_EUT = BUILDER
            .comment("Base Crossroads heat added to a running GTCEu machine per recipe EU/t per tick before machine profile multipliers.")
            .defineInRange("gtceuHeatBridge.wasteHeatPerEUt", 0.00035D, 0D, 10D);

    private static final ForgeConfigSpec.DoubleValue MINIMUM_ACTIVE_WASTE_HEAT = BUILDER
            .comment("Minimum Crossroads heat generated per tick by a running selected GTCEu machine.")
            .defineInRange("gtceuHeatBridge.minimumActiveWasteHeat", 250D, 0D, 1_000D);

    private static final ForgeConfigSpec.DoubleValue SAFE_TEMPERATURE_C = BUILDER
            .comment("Default safe operating temperature in degrees Celsius for selected GTCEu machines.")
            .defineInRange("gtceuHeatBridge.safeTemperatureC", 800D, -273D, 1_000_000D);

    private static final ForgeConfigSpec.DoubleValue DANGER_TEMPERATURE_C = BUILDER
            .comment("Default temperature in degrees Celsius where selected GTCEu machines are heat-suspended until they cool below the safe temperature.")
            .defineInRange("gtceuHeatBridge.dangerTemperatureC", 1200D, -273D, 1_000_000D);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> HIGH_TEMPERATURE_GTCEU_BLOCKS = BUILDER
            .comment("Exact GTCEu block IDs treated as high-temperature machines. These get larger safe/danger ranges so GregTech's existing coil/blast temperature mechanics remain dominant.")
            .defineListAllowEmpty("gtceuHeatBridge.highTemperatureBlocks", List.of(
                    "gtceu:electric_blast_furnace",
                    "gtceu:alloy_blast_smelter"
            ), CrossgroveConfig::isResourceLocation);

    private static final ForgeConfigSpec.DoubleValue HIGH_TEMPERATURE_SAFE_TEMPERATURE_C = BUILDER
            .comment("Safe operating temperature in degrees Celsius for high-temperature GTCEu machines.")
            .defineInRange("gtceuHeatBridge.highTemperatureSafeTemperatureC", 1800D, -273D, 1_000_000D);

    private static final ForgeConfigSpec.DoubleValue HIGH_TEMPERATURE_DANGER_TEMPERATURE_C = BUILDER
            .comment("Heat-suspension temperature in degrees Celsius for high-temperature GTCEu machines.")
            .defineInRange("gtceuHeatBridge.highTemperatureDangerTemperatureC", 2400D, -273D, 1_000_000D);

    private static final ForgeConfigSpec.BooleanValue ENABLE_LSO_CROSSROADS_HEAT_BRIDGE = BUILDER
            .comment("Lets Legendary Survival Overhaul's nearby block temperature scan use live Crossroads heat capability temperatures.")
            .define("lsoCrossroadsHeatBridge.enabled", true);

    private static final ForgeConfigSpec.DoubleValue LSO_AMBIENT_DEADBAND_C = BUILDER
            .comment("Crossroads heat within this many degrees Celsius of biome ambient has no LSO temperature influence.")
            .defineInRange("lsoCrossroadsHeatBridge.ambientDeadbandC", 5D, 0D, 1_000_000D);

    private static final ForgeConfigSpec.DoubleValue LSO_MAXIMUM_HEAT_INFLUENCE = BUILDER
            .comment("Maximum positive LSO temperature influence from a very hot Crossroads heat source. LSO uses 10 for campfires and 12.5 for lava.")
            .defineInRange("lsoCrossroadsHeatBridge.maximumHeatInfluence", 12.5D, 0D, 1_000D);

    private static final ForgeConfigSpec.DoubleValue LSO_MAXIMUM_COLD_INFLUENCE = BUILDER
            .comment("Maximum negative LSO temperature influence magnitude from a very cold Crossroads heat source.")
            .defineInRange("lsoCrossroadsHeatBridge.maximumColdInfluence", 12.5D, 0D, 1_000D);

    private static final ForgeConfigSpec.BooleanValue ENABLE_CROP_HEAT_BRIDGE = BUILDER
            .comment("Lets crop growth react to local Crossroads heat and cold.")
            .define("cropHeatBridge.enabled", true);

    private static final ForgeConfigSpec.IntValue CROP_HEAT_SCAN_RADIUS = BUILDER
            .comment("Radius in blocks around a crop scanned for Crossroads heat capability sources during crop growth attempts.")
            .defineInRange("cropHeatBridge.scanRadius", 5, 0, 16);

    private static final ForgeConfigSpec.DoubleValue CROP_HEAT_SOURCE_COUPLING = BUILDER
            .comment("Multiplier for how strongly nearby Crossroads heat sources shift crop temperature after distance falloff.")
            .defineInRange("cropHeatBridge.sourceCoupling", 0.5D, 0D, 10D);

    private static final ForgeConfigSpec.DoubleValue CROP_HEAT_SOURCE_DELTA_CAP_C = BUILDER
            .comment("Maximum degrees Celsius above or below ambient that one Crossroads heat source can contribute before distance falloff.")
            .defineInRange("cropHeatBridge.sourceDeltaCapC", 120D, 0D, 1_000_000D);

    private static final ForgeConfigSpec.DoubleValue CROP_HEAT_MAXIMUM_TEMPERATURE_SHIFT_C = BUILDER
            .comment("Maximum net degrees Celsius that all nearby Crossroads heat sources can shift a crop away from biome ambient.")
            .defineInRange("cropHeatBridge.maximumTemperatureShiftC", 60D, 0D, 1_000_000D);

    private static final ForgeConfigSpec.DoubleValue CROP_TOO_COLD_C = BUILDER
            .comment("Effective crop temperature at or below this value fully blocks normal crop growth.")
            .defineInRange("cropHeatBridge.tooColdC", 5D, -273D, 1_000_000D);

    private static final ForgeConfigSpec.DoubleValue CROP_IDEAL_MIN_C = BUILDER
            .comment("Lower bound of the ideal effective crop temperature range.")
            .defineInRange("cropHeatBridge.idealMinC", 18D, -273D, 1_000_000D);

    private static final ForgeConfigSpec.DoubleValue CROP_IDEAL_MAX_C = BUILDER
            .comment("Upper bound of the ideal effective crop temperature range.")
            .defineInRange("cropHeatBridge.idealMaxC", 32D, -273D, 1_000_000D);

    private static final ForgeConfigSpec.DoubleValue CROP_TOO_HOT_C = BUILDER
            .comment("Effective crop temperature at or above this value fully blocks normal crop growth.")
            .defineInRange("cropHeatBridge.tooHotC", 45D, -273D, 1_000_000D);

    private static final ForgeConfigSpec.BooleanValue CROP_ENABLE_GROWTH_BOOST = BUILDER
            .comment("Allows ideal crop temperatures to occasionally force a failed vanilla/mod crop growth attempt to succeed.")
            .define("cropHeatBridge.enableGrowthBoost", true);

    private static final ForgeConfigSpec.DoubleValue CROP_MAXIMUM_BOOST_CHANCE = BUILDER
            .comment("Maximum chance per crop random tick for ideal Crossroads heat conditions to force growth. Keep this low; vanilla crops already roll their own growth chance.")
            .defineInRange("cropHeatBridge.maximumBoostChance", 0.04D, 0D, 1D);

    private static final ForgeConfigSpec.BooleanValue CROP_ENABLE_STRESS_REGRESSION = BUILDER
            .comment("Allows extreme heat or cold to slowly regress age-based crops.")
            .define("cropHeatBridge.enableStressRegression", true);

    private static final ForgeConfigSpec.DoubleValue CROP_STRESS_REGRESSION_CHANCE = BUILDER
            .comment("Chance per crop random tick to lower crop age by one when the effective temperature is outside the growth range.")
            .defineInRange("cropHeatBridge.stressRegressionChance", 0.02D, 0D, 1D);

    private static final ForgeConfigSpec.BooleanValue ENABLE_AGRICRAFT_HEAT_BRIDGE = BUILDER
            .comment("Registers an AgriCraft growth condition that makes AgriCraft crops care about Crossroads-powered crop temperature.")
            .define("agriCraftHeatBridge.enabled", true);

    private static final ForgeConfigSpec.BooleanValue AGRICRAFT_HEAT_STRESS_IS_LETHAL = BUILDER
            .comment("When true, AgriCraft crops outside their viable temperature range use AgriCraft's lethal response, slowly regressing growth. When false, they only become infertile.")
            .define("agriCraftHeatBridge.lethalTemperatureStress", true);

    private static final ForgeConfigSpec.DoubleValue AGRICRAFT_STRENGTH_TOLERANCE_C = BUILDER
            .comment("Extra degrees Celsius of too-cold/too-hot tolerance per AgriCraft strength stat point above 1.")
            .defineInRange("agriCraftHeatBridge.strengthToleranceC", 0.75D, 0D, 100D);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enableGtceuHeatBridge;
    public static Set<ResourceLocation> enabledGtceuBlocks = Set.of();
    public static Set<String> enabledGtceuPathSuffixes = Set.of();
    public static Set<ResourceLocation> disabledGtceuBlocks = Set.of();
    public static double ambientTemperatureC;
    public static double thermalMass;
    public static double passiveCoolingRate;
    public static double wasteHeatPerEUt;
    public static double minimumActiveWasteHeat;
    public static double safeTemperatureC;
    public static double dangerTemperatureC;
    public static Set<ResourceLocation> highTemperatureGtceuBlocks = Set.of();
    public static double highTemperatureSafeTemperatureC;
    public static double highTemperatureDangerTemperatureC;
    public static boolean enableLsoCrossroadsHeatBridge;
    public static double lsoAmbientDeadbandC;
    public static double lsoMaximumHeatInfluence;
    public static double lsoMaximumColdInfluence;
    public static boolean enableCropHeatBridge;
    public static int cropHeatScanRadius;
    public static double cropHeatSourceCoupling;
    public static double cropHeatSourceDeltaCapC;
    public static double cropHeatMaximumTemperatureShiftC;
    public static double cropTooColdC;
    public static double cropIdealMinC;
    public static double cropIdealMaxC;
    public static double cropTooHotC;
    public static boolean cropEnableGrowthBoost;
    public static double cropMaximumBoostChance;
    public static boolean cropEnableStressRegression;
    public static double cropStressRegressionChance;
    public static boolean enableAgriCraftHeatBridge;
    public static boolean agriCraftHeatStressIsLethal;
    public static double agriCraftStrengthToleranceC;

    private CrossgroveConfig() {
    }

    private static boolean isResourceLocation(Object value) {
        if (!(value instanceof String stringValue)) {
            return false;
        }
        return ResourceLocation.tryParse(stringValue) != null;
    }

    private static boolean isPathSuffix(Object value) {
        return value instanceof String stringValue && !stringValue.isBlank() && !stringValue.contains(":");
    }

    @SubscribeEvent
    static void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }

        enableGtceuHeatBridge = ENABLE_GTCEU_HEAT_BRIDGE.get();
        enabledGtceuBlocks = ENABLED_GTCEU_BLOCKS.get().stream()
                .map(ResourceLocation::tryParse)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
        enabledGtceuPathSuffixes = ENABLED_GTCEU_PATH_SUFFIXES.get().stream()
                .collect(Collectors.toUnmodifiableSet());
        disabledGtceuBlocks = DISABLED_GTCEU_BLOCKS.get().stream()
                .map(ResourceLocation::tryParse)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
        ambientTemperatureC = AMBIENT_TEMPERATURE_C.get();
        thermalMass = THERMAL_MASS.get();
        passiveCoolingRate = PASSIVE_COOLING_RATE.get();
        wasteHeatPerEUt = WASTE_HEAT_PER_EUT.get();
        minimumActiveWasteHeat = MINIMUM_ACTIVE_WASTE_HEAT.get();
        safeTemperatureC = SAFE_TEMPERATURE_C.get();
        dangerTemperatureC = DANGER_TEMPERATURE_C.get();
        highTemperatureGtceuBlocks = HIGH_TEMPERATURE_GTCEU_BLOCKS.get().stream()
                .map(ResourceLocation::tryParse)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
        highTemperatureSafeTemperatureC = HIGH_TEMPERATURE_SAFE_TEMPERATURE_C.get();
        highTemperatureDangerTemperatureC = HIGH_TEMPERATURE_DANGER_TEMPERATURE_C.get();
        enableLsoCrossroadsHeatBridge = ENABLE_LSO_CROSSROADS_HEAT_BRIDGE.get();
        lsoAmbientDeadbandC = LSO_AMBIENT_DEADBAND_C.get();
        lsoMaximumHeatInfluence = LSO_MAXIMUM_HEAT_INFLUENCE.get();
        lsoMaximumColdInfluence = LSO_MAXIMUM_COLD_INFLUENCE.get();
        enableCropHeatBridge = ENABLE_CROP_HEAT_BRIDGE.get();
        cropHeatScanRadius = CROP_HEAT_SCAN_RADIUS.get();
        cropHeatSourceCoupling = CROP_HEAT_SOURCE_COUPLING.get();
        cropHeatSourceDeltaCapC = CROP_HEAT_SOURCE_DELTA_CAP_C.get();
        cropHeatMaximumTemperatureShiftC = CROP_HEAT_MAXIMUM_TEMPERATURE_SHIFT_C.get();
        cropTooColdC = CROP_TOO_COLD_C.get();
        cropIdealMinC = CROP_IDEAL_MIN_C.get();
        cropIdealMaxC = CROP_IDEAL_MAX_C.get();
        cropTooHotC = CROP_TOO_HOT_C.get();
        cropEnableGrowthBoost = CROP_ENABLE_GROWTH_BOOST.get();
        cropMaximumBoostChance = CROP_MAXIMUM_BOOST_CHANCE.get();
        cropEnableStressRegression = CROP_ENABLE_STRESS_REGRESSION.get();
        cropStressRegressionChance = CROP_STRESS_REGRESSION_CHANCE.get();
        enableAgriCraftHeatBridge = ENABLE_AGRICRAFT_HEAT_BRIDGE.get();
        agriCraftHeatStressIsLethal = AGRICRAFT_HEAT_STRESS_IS_LETHAL.get();
        agriCraftStrengthToleranceC = AGRICRAFT_STRENGTH_TOLERANCE_C.get();
    }
}
