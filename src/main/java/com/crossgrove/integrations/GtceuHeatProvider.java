package com.crossgrove.integrations;

import com.Da_Technomancer.crossroads.api.Capabilities;
import com.Da_Technomancer.crossroads.api.heat.HeatUtil;
import com.Da_Technomancer.crossroads.api.heat.IHeatHandler;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class GtceuHeatProvider implements ICapabilitySerializable<CompoundTag> {
    private static final String TEMPERATURE_KEY = "temperature";
    private static final String INITIALIZED_KEY = "initialized";
    private static final String HEAT_SUSPENDED_KEY = "heat_suspended";
    // forge gives us attach hooks, but the wrapper still needs a server tick
    private static final Set<GtceuHeatProvider> ACTIVE_PROVIDERS = ConcurrentHashMap.newKeySet();

    private final BlockEntity blockEntity;
    private final MachineHeatHandler heatHandler = new MachineHeatHandler();
    private final LazyOptional<IHeatHandler> heatCapability = LazyOptional.of(() -> heatHandler);
    private boolean heatSuspended;
    private double lastSafeTemperature = Double.NaN;
    private double lastDangerTemperature = Double.NaN;
    private int diagnosticTimer;

    GtceuHeatProvider(BlockEntity blockEntity) {
        this.blockEntity = blockEntity;
        ACTIVE_PROVIDERS.add(this);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        return Capabilities.HEAT_CAPABILITY.orEmpty(capability, heatCapability);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble(TEMPERATURE_KEY, heatHandler.temperature);
        tag.putBoolean(INITIALIZED_KEY, heatHandler.initialized);
        tag.putBoolean(HEAT_SUSPENDED_KEY, heatSuspended);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        heatHandler.temperature = clampTemperature(tag.getDouble(TEMPERATURE_KEY));
        heatHandler.initialized = tag.getBoolean(INITIALIZED_KEY);
        heatSuspended = tag.getBoolean(HEAT_SUSPENDED_KEY);
    }

    public void invalidate() {
        ACTIVE_PROVIDERS.remove(this);
        heatCapability.invalidate();
    }

    static void tickAll(Level level) {
        Iterator<GtceuHeatProvider> iterator = ACTIVE_PROVIDERS.iterator();
        while (iterator.hasNext()) {
            GtceuHeatProvider provider = iterator.next();
            if (provider.blockEntity.isRemoved()) {
                iterator.remove();
                continue;
            }
            if (provider.blockEntity.getLevel() == level) {
                provider.serverTick();
            }
        }
    }

    private void serverTick() {
        if (blockEntity.isRemoved() || blockEntity.getLevel() == null || blockEntity.getLevel().isClientSide()) {
            return;
        }
        Object machine = getMetaMachine();
        if (machine == null || booleanCall(machine, "isInValid", false)) {
            return;
        }

        GtceuHeatProfile profile = GtceuHeatProfiles.get(getBlockId());
        double safeTemperature = getSafeTemperature(machine, profile);
        double dangerTemperature = getDangerTemperature(machine, profile, safeTemperature);
        lastSafeTemperature = safeTemperature;
        lastDangerTemperature = dangerTemperature;
        double temperature = heatHandler.getTemp();

        if (temperature >= dangerTemperature) {
            heatSuspend(machine, temperature, dangerTemperature);
        } else if (heatSuspended && temperature <= safeTemperature) {
            heatResume(machine, temperature, safeTemperature);
        }

        Object recipeLogic = getRecipeLogic(machine);
        if (recipeLogic != null && booleanCall(recipeLogic, "isWorking", false)) {
            double wasteHeat = calculateWasteHeat(machine, recipeLogic, profile);
            heatHandler.addHeat(wasteHeat);
            logHeatDiagnostic(wasteHeat, recipeLogic);
        }

        coolTowardAmbient(profile);
    }

    private void logHeatDiagnostic(double wasteHeat, Object recipeLogic) {
        if (++diagnosticTimer < 100) {
            return;
        }
        diagnosticTimer = 0;
        CrossgroveIntegrations.LOGGER.debug("GTCEu heat: {} working={} temp={}C wasteHeat={} progress={}/{}",
                getBlockId(),
                booleanCall(recipeLogic, "isWorking", false),
                Math.round(heatHandler.temperature * 1000D) / 1000D,
                Math.round(wasteHeat * 1000D) / 1000D,
                intCall(recipeLogic, "getProgress", -1),
                intCall(recipeLogic, "getDuration", -1));
    }

    private Object getMetaMachine() {
        return invokeNoArg(blockEntity, "getMetaMachine");
    }

    private Object getRecipeLogic(Object machine) {
        return invokeNoArg(machine, "getRecipeLogic");
    }

    private void heatSuspend(Object machine, double temperature, double dangerTemperature) {
        if (heatSuspended) {
            if (booleanCall(machine, "isWorkingEnabled", false)) {
                invoke(machine, "setWorkingEnabled", new Class<?>[]{boolean.class}, false);
            }
            return;
        }
        if (!booleanCall(machine, "isWorkingEnabled", false)) {
            return;
        }
        invoke(machine, "setWorkingEnabled", new Class<?>[]{boolean.class}, false);
        heatSuspended = true;
        Object recipeLogic = getRecipeLogic(machine);
        if (recipeLogic != null) {
            invoke(recipeLogic, "setWaiting", new Class<?>[]{Component.class}, Component.literal("Heat suspended: " + Math.round(temperature) + " C / " + Math.round(dangerTemperature) + " C"));
        }
        markChanged();
    }

    private void heatResume(Object machine, double temperature, double safeTemperature) {
        invoke(machine, "setWorkingEnabled", new Class<?>[]{boolean.class}, true);
        heatSuspended = false;
        CrossgroveIntegrations.LOGGER.debug("Resumed {} after cooling to {} C below safe {} C", getBlockId(), temperature, safeTemperature);
        markChanged();
    }

    private double calculateWasteHeat(Object machine, Object recipeLogic, GtceuHeatProfile profile) {
        double eut = Math.max(0D, getRecipeEUt(invokeNoArg(recipeLogic, "getLastRecipe")));
        if (eut <= 0D) {
            eut = Math.max(1D, longCall(machine, "getMaxVoltage", 0L));
        }

        double profileMultiplier = getWasteHeatMultiplier();
        profileMultiplier *= 1D + Math.max(0, intCall(machine, "getOverclockTier", 0)) * 0.08D;
        double temperatureGain = Math.max(profile.activeTemperatureGainPerTick(),
                eut * Math.max(CrossgroveConfig.wasteHeatPerEUt, profile.temperatureGainPerEUt()) * profileMultiplier);
        temperatureGain = Math.max(temperatureGain, CrossgroveConfig.minimumActiveWasteHeat / Math.max(1D, CrossgroveConfig.thermalMass));
        return temperatureGain * getThermalMass();
    }

    private long getRecipeEUt(@Nullable Object recipe) {
        if (recipe == null) {
            return 0L;
        }
        Object inputEUt = invokeNoArg(recipe, "getInputEUt");
        if (inputEUt == null || booleanCall(inputEUt, "isEmpty", true)) {
            return 0L;
        }
        return Math.max(0L, longCall(inputEUt, "voltage", 0L) * longCall(inputEUt, "amperage", 0L));
    }

    private double getWasteHeatMultiplier() {
        String path = getBlockId().getPath();
        if (path.endsWith("_arc_furnace")) {
            return 1.5D;
        }
        if (path.endsWith("_fluid_heater")) {
            return 1.25D;
        }
        if (path.endsWith("_thermal_centrifuge")) {
            return 1.15D;
        }
        if (path.endsWith("_electric_furnace") || path.endsWith("_alloy_smelter")) {
            return 1D;
        }
        if (CrossgroveConfig.highTemperatureGtceuBlocks.contains(getBlockId())) {
            return 1.75D;
        }
        return 1D;
    }

    private double getSafeTemperature(Object machine, GtceuHeatProfile profile) {
        double configured = CrossgroveConfig.highTemperatureGtceuBlocks.contains(getBlockId())
                ? CrossgroveConfig.highTemperatureSafeTemperatureC
                : profile.safeTemperature();

        // coil machines already have their own heat ceiling
        Object coilType = invokeNoArg(machine, "getCoilType");
        if (coilType != null) {
            return Math.max(configured, intCall(coilType, "getCoilTemperature", 0));
        }
        return configured;
    }

    private double getDangerTemperature(Object machine, GtceuHeatProfile profile, double safeTemperature) {
        double configured = CrossgroveConfig.highTemperatureGtceuBlocks.contains(getBlockId())
                ? CrossgroveConfig.highTemperatureDangerTemperatureC
                : profile.dangerTemperature();

        Object coilType = invokeNoArg(machine, "getCoilType");
        if (coilType != null) {
            return Math.max(configured, intCall(coilType, "getCoilTemperature", 0) + 600D);
        }
        return Math.max(configured, safeTemperature + 1D);
    }

    private void coolTowardAmbient(GtceuHeatProfile profile) {
        double coolingRate = profile.passiveCoolingRate();
        if (coolingRate <= 0D) {
            return;
        }
        double ambient = initialTemperature();
        double difference = heatHandler.temperature - ambient;
        if (Math.abs(difference) < 0.001D) {
            return;
        }
        heatHandler.temperature = clampTemperature(heatHandler.temperature - difference * coolingRate);
        markChanged();
    }

    private double getThermalMass() {
        return Math.max(1D, GtceuHeatProfiles.get(getBlockId()).thermalMass());
    }

    public static void writeTooltipData(BlockEntity blockEntity, CompoundTag data) {
        if (Capabilities.HEAT_CAPABILITY == null) {
            return;
        }

        blockEntity.getCapability(Capabilities.HEAT_CAPABILITY, null).resolve().ifPresent(heatHandler -> {
            GtceuHeatProvider provider = findProvider(blockEntity);
            GtceuHeatTooltipData.write(
                    data,
                    heatHandler.getTemp(),
                    provider != null && provider.heatSuspended,
                    provider == null ? Double.NaN : provider.lastSafeTemperature,
                    provider == null ? Double.NaN : provider.lastDangerTemperature
            );
        });
    }

    private static @Nullable GtceuHeatProvider findProvider(BlockEntity blockEntity) {
        for (GtceuHeatProvider provider : ACTIVE_PROVIDERS) {
            if (provider.blockEntity == blockEntity) {
                return provider;
            }
        }
        return null;
    }

    private ResourceLocation getBlockId() {
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(blockEntity.getBlockState().getBlock());
        return blockId == null ? Objects.requireNonNull(ResourceLocation.tryParse("minecraft:air")) : blockId;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        return invoke(target, methodName, new Class<?>[0]);
    }

    // reflection keeps gtceu from becoming a hard source dependency
    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = findMethod(target.getClass(), methodName, parameterTypes);
            if (method == null) {
                return null;
            }
            return method.invoke(target, args);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            CrossgroveIntegrations.LOGGER.debug("Failed reflective GTCEu call {} on {}", methodName, target.getClass().getName(), exception);
            return null;
        }
    }

    private static Method findMethod(Class<?> type, String methodName, Class<?>[] parameterTypes) {
        Class<?> cursor = type;
        while (cursor != null) {
            try {
                Method method = cursor.getMethod(methodName, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                cursor = cursor.getSuperclass();
            }
        }
        return null;
    }

    private static boolean booleanCall(Object target, String methodName, boolean fallback) {
        Object value = invokeNoArg(target, methodName);
        return value instanceof Boolean booleanValue ? booleanValue : fallback;
    }

    private static int intCall(Object target, String methodName, int fallback) {
        Object value = invokeNoArg(target, methodName);
        return value instanceof Number numberValue ? numberValue.intValue() : fallback;
    }

    private static long longCall(Object target, String methodName, long fallback) {
        Object value = invokeNoArg(target, methodName);
        return value instanceof Number numberValue ? numberValue.longValue() : fallback;
    }

    private double initialTemperature() {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return CrossgroveConfig.ambientTemperatureC;
        }
        return HeatUtil.convertBiomeTemp(level, blockEntity.getBlockPos());
    }

    private void markChanged() {
        blockEntity.setChanged();
    }

    private static double clampTemperature(double temperature) {
        return Math.max(HeatUtil.ABSOLUTE_ZERO, Math.min(temperature, HeatUtil.MAX_TEMP));
    }

    private final class MachineHeatHandler implements IHeatHandler {
        private double temperature;
        private boolean initialized;

        @Override
        public double getTemp() {
            init();
            return temperature;
        }

        @Override
        public void setTemp(double temperature) {
            initialized = true;
            this.temperature = clampTemperature(temperature);
            markChanged();
        }

        @Override
        public void addHeat(double heat) {
            init();
            temperature = clampTemperature(temperature + heat / getThermalMass());
            markChanged();
        }

        private void init() {
            if (initialized) {
                return;
            }
            temperature = clampTemperature(initialTemperature());
            initialized = true;
            markChanged();
        }
    }
}
