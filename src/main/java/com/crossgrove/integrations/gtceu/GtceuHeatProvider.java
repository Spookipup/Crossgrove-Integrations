package com.crossgrove.integrations.gtceu;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
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

import com.Da_Technomancer.crossroads.api.Capabilities;
import com.Da_Technomancer.crossroads.api.heat.HeatUtil;
import com.Da_Technomancer.crossroads.api.heat.IHeatHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.crossgrove.integrations.CrossgroveConfig;
import com.crossgrove.integrations.CrossgroveIntegrations;

public final class GtceuHeatProvider implements ICapabilitySerializable<CompoundTag> {
	private static final String TEMPERATURE_KEY = "temperature";
	private static final String INITIALIZED_KEY = "initialized";
	private static final String HEAT_SUSPENDED_KEY = "heat_suspended";
	private static final String HEAT_SUSPENSION_REASON_KEY = "heat_suspension_reason";
	private static final String SUSPENSION_NONE = "";
	private static final String SUSPENSION_TOO_COLD = "too_cold";
	private static final String SUSPENSION_TOO_HOT = "too_hot";
	private static final String SUSPENSION_NO_ROTARY = "no_rotary";
	private static final String GTCEU_MULTIBLOCK_CONTROLLER = "com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController";
	private static final String GTCEU_MULTIBLOCK_PART = "com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart";
	// forge gives us attach hooks, but the wrapper still needs a server tick
	private static final Set<GtceuHeatProvider> ACTIVE_PROVIDERS = ConcurrentHashMap.newKeySet();
	private static final Map<BlockEntity, GtceuHeatProvider> ACTIVE_BY_BLOCK_ENTITY = new ConcurrentHashMap<>();

	private final BlockEntity blockEntity;
	private final MachineHeatHandler heatHandler = new MachineHeatHandler();
	private final LazyOptional<IHeatHandler> heatCapability = LazyOptional.of(() -> heatHandler);
	private boolean heatSuspended;
	private String heatSuspensionReason = SUSPENSION_NONE;
	private double lastMinimumWorkingTemperature = Double.NaN;
	private double lastIdealMinTemperature = Double.NaN;
	private double lastIdealMaxTemperature = Double.NaN;
	private boolean lastHeatPowersMachine;
	private double lastMinimumRotarySpeed = Double.NaN;
	private double lastIdealMinRotarySpeed = Double.NaN;
	private double lastIdealMaxRotarySpeed = Double.NaN;
	private double lastRotarySpeed;
	private double lastRotaryEnergy;
	private double lastSafeTemperature = Double.NaN;
	private double lastDangerTemperature = Double.NaN;
	private boolean lastSharedMultiblockHeat;
	private int lastMultiblockHeatParts = 1;
	private int lastMultiblockStructureBlocks = 1;
	private int diagnosticTimer;

	GtceuHeatProvider(BlockEntity blockEntity) {
		this.blockEntity = blockEntity;
		ACTIVE_PROVIDERS.add(this);
		ACTIVE_BY_BLOCK_ENTITY.put(blockEntity, this);
	}

	@Override
	public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
		if (side != null && hidesSideHeatCapability()) {
			return LazyOptional.empty();
		}
		return Capabilities.HEAT_CAPABILITY.orEmpty(capability, heatCapability);
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putDouble(TEMPERATURE_KEY, heatHandler.temperature);
		tag.putBoolean(INITIALIZED_KEY, heatHandler.initialized);
		tag.putBoolean(HEAT_SUSPENDED_KEY, heatSuspended);
		tag.putString(HEAT_SUSPENSION_REASON_KEY, heatSuspensionReason);
		return tag;
	}

	@Override
	public void deserializeNBT(CompoundTag tag) {
		heatHandler.temperature = clampTemperature(tag.getDouble(TEMPERATURE_KEY));
		heatHandler.initialized = tag.getBoolean(INITIALIZED_KEY);
		heatSuspended = tag.getBoolean(HEAT_SUSPENDED_KEY);
		heatSuspensionReason = tag.getString(HEAT_SUSPENSION_REASON_KEY);
		if (!SUSPENSION_TOO_COLD.equals(heatSuspensionReason)
				&& !SUSPENSION_TOO_HOT.equals(heatSuspensionReason)
				&& !SUSPENSION_NO_ROTARY.equals(heatSuspensionReason)) {
			heatSuspensionReason = heatSuspended ? SUSPENSION_TOO_HOT : SUSPENSION_NONE;
		}
	}

	public void invalidate() {
		ACTIVE_PROVIDERS.remove(this);
		ACTIVE_BY_BLOCK_ENTITY.remove(blockEntity);
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

	public static @Nullable IHeatHandler findSharedMultiblockHeatAtStructureBlock(Level level, BlockPos pos) {
		if (!CrossgroveConfig.shareMultiblockHeat) {
			return null;
		}
		for (GtceuHeatProvider provider : ACTIVE_PROVIDERS) {
			if (provider.blockEntity.isRemoved() || provider.blockEntity.getLevel() != level || !provider.isSelectedHeatMachine()) {
				continue;
			}
			Object machine = provider.getMetaMachine();
			if (machine == null || !isMultiblockController(machine) || !booleanCall(machine, "isFormed", false)) {
				continue;
			}
			Set<BlockPos> structurePositions = provider.getMultiblockStructurePositions(machine);
			if (structurePositions.contains(pos)) {
				return provider.heatHandler;
			}
		}
		return null;
	}

	private void serverTick() {
		if (blockEntity.isRemoved() || blockEntity.getLevel() == null || blockEntity.getLevel().isClientSide()) {
			return;
		}
		Object machine = getMetaMachine();
		if (machine == null || booleanCall(machine, "isInValid", false)) {
			return;
		}

		if (CrossgroveConfig.shareMultiblockHeat && isMultiblockPart(machine)) {
			GtceuHeatProvider controllerProvider = findLinkedSelectedControllerProvider(machine);
			if (controllerProvider != null) {
				controllerProvider.spreadMultiblockHeat();
				copySharedMultiblockState(controllerProvider);
			} else {
				clearSharedMultiblockState();
				coolTowardAmbient(GtceuHeatProfiles.get(getBlockId()));
			}
			return;
		}

		if (CrossgroveConfig.shareMultiblockHeat && isMultiblockController(machine)) {
			spreadMultiblockHeat(machine);
			updateSharedMultiblockState(machine);
		} else {
			clearSharedMultiblockState();
		}

		GtceuHeatProfile profile = GtceuHeatProfiles.get(getBlockId());
		lastMinimumWorkingTemperature = profile.minimumWorkingTemperature();
		lastIdealMinTemperature = profile.idealMinTemperature();
		lastIdealMaxTemperature = profile.idealMaxTemperature();
		lastHeatPowersMachine = profile.heatPowersMachine();
		lastMinimumRotarySpeed = profile.minimumRotarySpeed();
		lastIdealMinRotarySpeed = profile.idealMinRotarySpeed();
		lastIdealMaxRotarySpeed = profile.idealMaxRotarySpeed();
		double safeTemperature = getSafeTemperature(machine, profile);
		double dangerTemperature = getDangerTemperature(machine, profile, safeTemperature);
		lastSafeTemperature = safeTemperature;
		lastDangerTemperature = dangerTemperature;
		double temperature = heatHandler.getTemp();

		if (temperature >= dangerTemperature) {
			heatSuspend(machine, SUSPENSION_TOO_HOT, "Heat suspended", temperature, dangerTemperature);
		} else if (heatSuspended && SUSPENSION_TOO_HOT.equals(heatSuspensionReason) && temperature <= safeTemperature) {
			heatResume(machine, temperature, safeTemperature);
		}

		if (!SUSPENSION_TOO_HOT.equals(heatSuspensionReason) && profile.requiresWorkingHeat() && temperature < profile.minimumWorkingTemperature()) {
			heatSuspend(machine, SUSPENSION_TOO_COLD, "Waiting for heat", temperature, profile.minimumWorkingTemperature());
		} else if (heatSuspended && SUSPENSION_TOO_COLD.equals(heatSuspensionReason) && temperature >= profile.minimumWorkingTemperature()) {
			heatResume(machine, temperature, profile.minimumWorkingTemperature());
		}

		GtceuCrossroadsPower.RotaryState rotaryState = readRotaryState();
		if (!SUSPENSION_TOO_HOT.equals(heatSuspensionReason)
				&& !SUSPENSION_TOO_COLD.equals(heatSuspensionReason)
				&& profile.requiresRotaryPower()
				&& rotaryState.speed() < profile.minimumRotarySpeed()) {
			heatSuspend(machine, SUSPENSION_NO_ROTARY, "Waiting for rotary power", rotaryState.speed(), profile.minimumRotarySpeed());
		} else if (heatSuspended && SUSPENSION_NO_ROTARY.equals(heatSuspensionReason) && rotaryState.speed() >= profile.minimumRotarySpeed()) {
			heatResume(machine, rotaryState.speed(), profile.minimumRotarySpeed());
		}

		Object recipeLogic = getRecipeLogic(machine);
		if (recipeLogic != null && booleanCall(recipeLogic, "isWorking", false)) {
			if (profile.requiresRotaryPower() && !GtceuCrossroadsPower.consumeRotaryEnergy(rotaryState, profile.rotaryEnergyPerTick())) {
				heatSuspend(machine, SUSPENSION_NO_ROTARY, "Waiting for rotary power", rotaryState.energy(), profile.rotaryEnergyPerTick());
				coolTowardAmbient(profile);
				return;
			}
			double wasteTemperatureGain = calculateWasteTemperatureGain(machine, recipeLogic, profile);
			heatHandler.addInternalTemperature(wasteTemperatureGain);
			logHeatDiagnostic(wasteTemperatureGain, recipeLogic);
		}

		coolTowardAmbient(profile);

		if (CrossgroveConfig.shareMultiblockHeat && isMultiblockController(machine)) {
			spreadMultiblockHeat(machine);
			updateSharedMultiblockState(machine);
		}
	}

	private GtceuCrossroadsPower.RotaryState readRotaryState() {
		Level level = blockEntity.getLevel();
		GtceuCrossroadsPower.RotaryState state = level == null
				? GtceuCrossroadsPower.NO_ROTARY
				: GtceuCrossroadsPower.findBestRotary(level, blockEntity.getBlockPos());
		lastRotarySpeed = state.speed();
		lastRotaryEnergy = state.energy();
		return state;
	}

	private void logHeatDiagnostic(double wasteTemperatureGain, Object recipeLogic) {
		if (++diagnosticTimer < 100) {
			return;
		}
		diagnosticTimer = 0;
		CrossgroveIntegrations.LOGGER.debug("GTCEu heat: {} working={} temp={}C wasteTempGain={} progress={}/{}",
				getBlockId(),
				booleanCall(recipeLogic, "isWorking", false),
				Math.round(heatHandler.temperature * 1000D) / 1000D,
				Math.round(wasteTemperatureGain * 1000D) / 1000D,
				intCall(recipeLogic, "getProgress", -1),
				intCall(recipeLogic, "getDuration", -1));
	}

	private Object getMetaMachine() {
		return invokeNoArg(blockEntity, "getMetaMachine");
	}

	private boolean hidesSideHeatCapability() {
		if (!CrossgroveConfig.shareMultiblockHeat) {
			return false;
		}
		Object machine = getMetaMachine();
		return machine != null && (isMultiblockController(machine) || isMultiblockPart(machine));
	}

	private Object getRecipeLogic(Object machine) {
		return invokeNoArg(machine, "getRecipeLogic");
	}

	private void heatSuspend(Object machine, String reason, String message, double temperature, double thresholdTemperature) {
		if (heatSuspended) {
			if (booleanCall(machine, "isWorkingEnabled", false)) {
				invoke(machine, "setWorkingEnabled", new Class<?>[]{boolean.class}, false);
			}
			if (!reason.equals(heatSuspensionReason)) {
				heatSuspensionReason = reason;
				setRecipeWaitingMessage(machine, message, temperature, thresholdTemperature);
				markChanged();
			}
			return;
		}
		if (!booleanCall(machine, "isWorkingEnabled", false)) {
			return;
		}
		invoke(machine, "setWorkingEnabled", new Class<?>[]{boolean.class}, false);
		heatSuspended = true;
		heatSuspensionReason = reason;
		setRecipeWaitingMessage(machine, message, temperature, thresholdTemperature);
		markChanged();
	}

	private void setRecipeWaitingMessage(Object machine, String message, double temperature, double thresholdTemperature) {
		Object recipeLogic = getRecipeLogic(machine);
		if (recipeLogic != null) {
			invoke(recipeLogic, "setWaiting", new Class<?>[]{Component.class}, Component.literal(message + ": "
					+ Math.round(temperature) + " C / " + Math.round(thresholdTemperature) + " C"));
		}
	}

	private void heatResume(Object machine, double temperature, double thresholdTemperature) {
		invoke(machine, "setWorkingEnabled", new Class<?>[]{boolean.class}, true);
		heatSuspended = false;
		heatSuspensionReason = SUSPENSION_NONE;
		CrossgroveIntegrations.LOGGER.debug("Resumed {} at {} C past threshold {} C", getBlockId(), temperature, thresholdTemperature);
		markChanged();
	}

	private double calculateWasteTemperatureGain(Object machine, Object recipeLogic, GtceuHeatProfile profile) {
		double eut = Math.max(0D, getRecipeEUt(invokeNoArg(recipeLogic, "getLastRecipe")));
		if (eut <= 0D) {
			eut = Math.max(1D, longCall(machine, "getMaxVoltage", 0L));
		}

		double profileMultiplier = getWasteHeatMultiplier();
		profileMultiplier *= 1D + Math.max(0, intCall(machine, "getOverclockTier", 0)) * 0.08D;
		double temperatureGain = Math.max(profile.activeTemperatureGainPerTick(),
				eut * Math.max(CrossgroveConfig.wasteHeatPerEUt, profile.temperatureGainPerEUt()) * profileMultiplier);
		if (!GtceuHeatProfiles.isProfiled(getBlockId())) {
			temperatureGain = Math.max(temperatureGain, CrossgroveConfig.minimumActiveWasteHeat / getThermalMass());
		}
		return temperatureGain;
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
				? Math.max(CrossgroveConfig.highTemperatureSafeTemperatureC, profile.safeTemperature())
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
				? Math.max(CrossgroveConfig.highTemperatureDangerTemperatureC, profile.dangerTemperature())
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
		double mass = Math.max(1D, GtceuHeatProfiles.get(getBlockId()).thermalMass());
		if (CrossgroveConfig.shareMultiblockHeat) {
			Object machine = getMetaMachine();
			if (machine != null && isMultiblockController(machine) && booleanCall(machine, "isFormed", false)) {
				mass += Math.max(0, getMultiblockStructureBlockCount(machine) - 1)
						* Math.max(0D, CrossgroveConfig.multiblockStructureBlockThermalMass);
			}
		}
		return Math.max(1D, mass);
	}

	private Set<BlockPos> getMultiblockStructurePositions(Object controllerMachine) {
		Set<BlockPos> positions = new HashSet<>();
		positions.add(blockEntity.getBlockPos());

		Object state = invokeNoArg(controllerMachine, "getMultiblockState");
		Object cache = state == null ? null : invokeNoArg(state, "getCache");
		addPositions(positions, cache);

		Object partPositions = invokeNoArg(controllerMachine, "getPartPositions");
		addPositions(positions, partPositions);

		Object parts = invokeNoArg(controllerMachine, "getParts");
		if (parts instanceof Iterable<?> iterable) {
			for (Object part : iterable) {
				Object pos = invokeNoArg(part, "getPos");
				if (pos instanceof BlockPos blockPos) {
					positions.add(blockPos);
				}
			}
		}
		return positions;
	}

	private static void addPositions(Set<BlockPos> positions, @Nullable Object source) {
		if (source instanceof BlockPos[] blockPositions) {
			positions.addAll(List.of(blockPositions));
			return;
		}
		if (source instanceof Iterable<?> iterable) {
			for (Object value : iterable) {
				if (value instanceof BlockPos blockPos) {
					positions.add(blockPos);
				}
			}
		}
	}

	private void spreadMultiblockHeat() {
		Object machine = getMetaMachine();
		if (machine != null) {
			spreadMultiblockHeat(machine);
		}
	}

	private void spreadMultiblockHeat(Object controllerMachine) {
		if (!booleanCall(controllerMachine, "isFormed", false)) {
			clearSharedMultiblockState();
			return;
		}
		List<GtceuHeatProvider> providers = getMultiblockHeatProviders(controllerMachine);
		lastSharedMultiblockHeat = true;
		lastMultiblockHeatParts = providers.size();
		lastMultiblockStructureBlocks = getMultiblockStructureBlockCount(controllerMachine);
		if (providers.size() <= 1) {
			return;
		}

		double weightedTemperature = 0D;
		double totalMass = 0D;
		for (GtceuHeatProvider provider : providers) {
			double mass = provider == this
					? provider.getThermalMass()
					: Math.max(1D, CrossgroveConfig.thermalMass);
			weightedTemperature += provider.heatHandler.getLocalTemp() * mass;
			totalMass += mass;
		}
		if (totalMass <= 0D) {
			return;
		}

		double sharedTemperature = clampTemperature(weightedTemperature / totalMass);
		for (GtceuHeatProvider provider : providers) {
			provider.heatHandler.setLocalTemp(sharedTemperature);
		}
	}

	private void updateSharedMultiblockState(Object controllerMachine) {
		if (!booleanCall(controllerMachine, "isFormed", false)) {
			clearSharedMultiblockState();
			return;
		}
		lastSharedMultiblockHeat = true;
		lastMultiblockHeatParts = getMultiblockHeatProviders(controllerMachine).size();
		lastMultiblockStructureBlocks = getMultiblockStructureBlockCount(controllerMachine);
	}

	private void copySharedMultiblockState(GtceuHeatProvider controllerProvider) {
		lastSharedMultiblockHeat = controllerProvider.lastSharedMultiblockHeat;
		lastMultiblockHeatParts = controllerProvider.lastMultiblockHeatParts;
		lastMultiblockStructureBlocks = controllerProvider.lastMultiblockStructureBlocks;
	}

	private void clearSharedMultiblockState() {
		lastSharedMultiblockHeat = false;
		lastMultiblockHeatParts = 1;
		lastMultiblockStructureBlocks = 1;
	}

	private List<GtceuHeatProvider> getMultiblockHeatProviders(Object controllerMachine) {
		List<GtceuHeatProvider> providers = new ArrayList<>();
		providers.add(this);

		Object parts = invokeNoArg(controllerMachine, "getParts");
		if (!(parts instanceof Iterable<?> iterable)) {
			return providers;
		}

		Level level = blockEntity.getLevel();
		if (level == null) {
			return providers;
		}
		for (Object part : iterable) {
			Object pos = invokeNoArg(part, "getPos");
			if (!(pos instanceof BlockPos blockPos)) {
				continue;
			}
			BlockEntity partBlockEntity = level.getBlockEntity(blockPos);
			GtceuHeatProvider provider = ACTIVE_BY_BLOCK_ENTITY.get(partBlockEntity);
			if (provider != null && provider != this && !providers.contains(provider)) {
				providers.add(provider);
			}
		}
		return providers;
	}

	private int getMultiblockStructureBlockCount(Object controllerMachine) {
		Set<BlockPos> positions = getMultiblockStructurePositions(controllerMachine);
		if (positions.size() > 1) {
			return positions.size();
		}

		Object partPositions = invokeNoArg(controllerMachine, "getPartPositions");
		if (partPositions instanceof BlockPos[] blockPositions) {
			return Math.max(1, blockPositions.length + 1);
		}
		if (partPositions instanceof Collection<?> collection) {
			return Math.max(1, collection.size() + 1);
		}

		Object parts = invokeNoArg(controllerMachine, "getParts");
		if (parts instanceof Collection<?> collection) {
			return Math.max(1, collection.size() + 1);
		}
		return 1;
	}

	private @Nullable GtceuHeatProvider findLinkedSelectedControllerProvider() {
		Object machine = getMetaMachine();
		return machine == null ? null : findLinkedSelectedControllerProvider(machine);
	}

	private @Nullable GtceuHeatProvider findLinkedSelectedControllerProvider(Object partMachine) {
		if (!CrossgroveConfig.shareMultiblockHeat || !isMultiblockPart(partMachine)) {
			return null;
		}

		Object controllers = invokeNoArg(partMachine, "getControllers");
		if (!(controllers instanceof Iterable<?> iterable)) {
			return null;
		}

		Level level = blockEntity.getLevel();
		if (level == null) {
			return null;
		}
		for (Object controller : iterable) {
			Object pos = invokeNoArg(controller, "getPos");
			if (!(pos instanceof BlockPos blockPos)) {
				continue;
			}
			GtceuHeatProvider provider = ACTIVE_BY_BLOCK_ENTITY.get(level.getBlockEntity(blockPos));
			if (provider != null && provider.isSelectedHeatMachine()) {
				return provider;
			}
		}
		return null;
	}

	private boolean isSelectedHeatMachine() {
		return GtceuHeatBridge.isSelectedGtceuMachine(getBlockId());
	}

	private static boolean isMultiblockController(Object machine) {
		return implementsNamedInterface(machine.getClass(), GTCEU_MULTIBLOCK_CONTROLLER);
	}

	private static boolean isMultiblockPart(Object machine) {
		return implementsNamedInterface(machine.getClass(), GTCEU_MULTIBLOCK_PART);
	}

	private static boolean implementsNamedInterface(Class<?> type, String interfaceName) {
		Class<?> cursor = type;
		while (cursor != null) {
			if (interfaceName.equals(cursor.getName())) {
				return true;
			}
			for (Class<?> candidate : cursor.getInterfaces()) {
				if (implementsNamedInterface(candidate, interfaceName)) {
					return true;
				}
			}
			cursor = cursor.getSuperclass();
		}
		return false;
	}

	public static void writeTooltipData(BlockEntity blockEntity, CompoundTag data) {
		if (Capabilities.HEAT_CAPABILITY == null) {
			return;
		}

		blockEntity.getCapability(Capabilities.HEAT_CAPABILITY, null).resolve().ifPresent(heatHandler -> {
			GtceuHeatProvider provider = findProvider(blockEntity);
			GtceuHeatProvider displayProvider = provider == null ? null : provider.findLinkedSelectedControllerProvider();
			if (displayProvider == null) {
				displayProvider = provider;
			}
			GtceuHeatTooltipData.write(
					data,
					heatHandler.getTemp(),
					displayProvider != null && displayProvider.heatSuspended,
					displayProvider == null ? SUSPENSION_NONE : displayProvider.heatSuspensionReason,
					displayProvider != null && displayProvider.lastHeatPowersMachine,
					displayProvider == null ? Double.NaN : displayProvider.lastMinimumWorkingTemperature,
					displayProvider == null ? Double.NaN : displayProvider.lastIdealMinTemperature,
					displayProvider == null ? Double.NaN : displayProvider.lastIdealMaxTemperature,
					displayProvider == null ? Double.NaN : displayProvider.lastMinimumRotarySpeed,
					displayProvider == null ? Double.NaN : displayProvider.lastIdealMinRotarySpeed,
					displayProvider == null ? Double.NaN : displayProvider.lastIdealMaxRotarySpeed,
					displayProvider == null ? 0D : displayProvider.lastRotarySpeed,
					displayProvider == null ? 0D : displayProvider.lastRotaryEnergy,
					displayProvider == null ? Double.NaN : displayProvider.lastSafeTemperature,
					displayProvider == null ? Double.NaN : displayProvider.lastDangerTemperature,
					displayProvider != null && displayProvider.lastSharedMultiblockHeat,
					displayProvider == null ? 1 : displayProvider.lastMultiblockHeatParts,
					displayProvider == null ? 1 : displayProvider.lastMultiblockStructureBlocks
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
		return blockId == null ? ResourceLocation.fromNamespaceAndPath("minecraft", "air") : blockId;
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
			GtceuHeatProvider controllerProvider = findLinkedSelectedControllerProvider();
			if (controllerProvider != null && controllerProvider != GtceuHeatProvider.this) {
				return controllerProvider.heatHandler.getTemp();
			}
			init();
			return temperature;
		}

		@Override
		public void setTemp(double temperature) {
			GtceuHeatProvider controllerProvider = findLinkedSelectedControllerProvider();
			if (controllerProvider != null && controllerProvider != GtceuHeatProvider.this) {
				controllerProvider.heatHandler.setTemp(temperature);
				setLocalTemp(temperature);
				return;
			}
			setLocalTemp(temperature);
		}

		@Override
		public void addHeat(double heat) {
			GtceuHeatProvider controllerProvider = findLinkedSelectedControllerProvider();
			if (controllerProvider != null && controllerProvider != GtceuHeatProvider.this) {
				controllerProvider.heatHandler.addHeat(heat);
				setLocalTemp(controllerProvider.heatHandler.getTemp());
				return;
			}
			setLocalTemp(getLocalTemp() + heat);
		}

		private void addInternalTemperature(double temperatureDelta) {
			setLocalTemp(getLocalTemp() + temperatureDelta);
		}

		private double getLocalTemp() {
			init();
			return temperature;
		}

		private void setLocalTemp(double temperature) {
			initialized = true;
			double clamped = clampTemperature(temperature);
			if (Math.abs(this.temperature - clamped) < 0.001D) {
				this.temperature = clamped;
				return;
			}
			this.temperature = clamped;
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
