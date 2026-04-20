package com.crossgrove.integrations.gtceu.exdeorum;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;

import com.Da_Technomancer.crossroads.api.Capabilities;
import com.Da_Technomancer.crossroads.api.heat.HeatUtil;
import com.Da_Technomancer.crossroads.api.heat.IHeatHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import thedarkcolour.exdeorum.blockentity.AbstractCrucibleBlockEntity;
import thedarkcolour.exdeorum.blockentity.ETankBlockEntity;
import thedarkcolour.exdeorum.recipe.RecipeUtil;

import com.crossgrove.integrations.CrossgroveConfig;

public final class ExDeorumHeatProvider implements ICapabilitySerializable<CompoundTag> {
	private static final String TEMPERATURE_KEY = "temperature";
	private static final String INITIALIZED_KEY = "initialized";
	private static final double PASSIVE_RATE = 0.004D;
	private static final double ACTIVE_SOURCE_RATE = 0.02D;
	private static final Set<ExDeorumHeatProvider> ACTIVE_PROVIDERS = ConcurrentHashMap.newKeySet();
	private static final Map<BlockEntity, ExDeorumHeatProvider> ACTIVE_BY_BLOCK_ENTITY = new ConcurrentHashMap<>();

	private final ETankBlockEntity blockEntity;
	private final TankHeatHandler heatHandler = new TankHeatHandler();
	private final LazyOptional<IHeatHandler> heatCapability = LazyOptional.of(() -> heatHandler);
	private boolean initialized;
	private double temperature;

	ExDeorumHeatProvider(ETankBlockEntity blockEntity) {
		this.blockEntity = blockEntity;
		ACTIVE_PROVIDERS.add(this);
		ACTIVE_BY_BLOCK_ENTITY.put(blockEntity, this);
	}

	@Override
	public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
		return Capabilities.HEAT_CAPABILITY.orEmpty(capability, heatCapability);
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putDouble(TEMPERATURE_KEY, temperature);
		tag.putBoolean(INITIALIZED_KEY, initialized);
		return tag;
	}

	@Override
	public void deserializeNBT(CompoundTag tag) {
		temperature = clampTemperature(tag.getDouble(TEMPERATURE_KEY));
		initialized = tag.getBoolean(INITIALIZED_KEY);
	}

	public void invalidate() {
		ACTIVE_PROVIDERS.remove(this);
		ACTIVE_BY_BLOCK_ENTITY.remove(blockEntity);
		heatCapability.invalidate();
	}

	static void tickAll(Level level) {
		Iterator<ExDeorumHeatProvider> iterator = ACTIVE_PROVIDERS.iterator();
		while (iterator.hasNext()) {
			ExDeorumHeatProvider provider = iterator.next();
			if (provider.blockEntity.isRemoved()) {
				iterator.remove();
				ACTIVE_BY_BLOCK_ENTITY.remove(provider.blockEntity);
				continue;
			}
			if (provider.blockEntity.getLevel() == level) {
				provider.serverTick();
			}
		}
	}

	private void serverTick() {
		Level level = blockEntity.getLevel();
		if (Capabilities.HEAT_CAPABILITY == null || level == null || level.isClientSide() || blockEntity.isRemoved()) {
			return;
		}

		double ambient = initialTemperature();
		double target = Math.max(ambient, fluidTemperature());
		double rate = PASSIVE_RATE;
		if (blockEntity instanceof AbstractCrucibleBlockEntity) {
			double sourceTemperature = sourceTemperature(level, blockEntity.getBlockPos());
			if (sourceTemperature > target) {
				target = sourceTemperature;
				rate = ACTIVE_SOURCE_RATE;
			}
		}

		heatHandler.moveToward(target, rate);
	}

	public static double getTemperature(BlockEntity blockEntity) {
		ExDeorumHeatProvider provider = ACTIVE_BY_BLOCK_ENTITY.get(blockEntity);
		return provider == null ? Double.NaN : provider.heatHandler.getTemp();
	}

	public static int meltingRateFromTemperature(double temperature) {
		if (Double.isNaN(temperature) || temperature < 500D) {
			return 0;
		}
		if (temperature >= 1200D) {
			return 16;
		}
		if (temperature >= 1000D) {
			return 8;
		}
		if (temperature >= 750D) {
			return 4;
		}
		return 1;
	}

	private double sourceTemperature(Level level, BlockPos pos) {
		BlockState source = level.getBlockState(pos.below());
		int heatValue = RecipeUtil.getHeatValue(source);
		if (heatValue <= 0) {
			return HeatUtil.ABSOLUTE_ZERO;
		}
		return Math.max(575D, heatValue * 250D);
	}

	private double fluidTemperature() {
		FluidStack stored = blockEntity.getTank().getFluid();
		if (stored.isEmpty()) {
			return HeatUtil.ABSOLUTE_ZERO;
		}
		return Math.max(HeatUtil.ABSOLUTE_ZERO, stored.getFluid().getFluidType().getTemperature() - 273D);
	}

	private double initialTemperature() {
		Level level = blockEntity.getLevel();
		return level == null ? CrossgroveConfig.ambientTemperatureC : HeatUtil.convertBiomeTemp(level, blockEntity.getBlockPos());
	}

	private void markChanged() {
		blockEntity.setChanged();
	}

	private static double clampTemperature(double temperature) {
		return Math.max(HeatUtil.ABSOLUTE_ZERO, Math.min(temperature, HeatUtil.MAX_TEMP));
	}

	private final class TankHeatHandler implements IHeatHandler {
		@Override
		public double getTemp() {
			init();
			return temperature;
		}

		@Override
		public void setTemp(double temperature) {
			initialized = true;
			setLocalTemp(temperature);
		}

		@Override
		public void addHeat(double heat) {
			setLocalTemp(getTemp() + heat);
		}

		private void moveToward(double target, double rate) {
			init();
			double difference = target - temperature;
			if (Math.abs(difference) < 0.001D) {
				return;
			}
			setLocalTemp(temperature + difference * rate);
		}

		private void setLocalTemp(double temperature) {
			double clamped = clampTemperature(temperature);
			if (Math.abs(ExDeorumHeatProvider.this.temperature - clamped) < 0.001D) {
				ExDeorumHeatProvider.this.temperature = clamped;
				return;
			}
			ExDeorumHeatProvider.this.temperature = clamped;
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
