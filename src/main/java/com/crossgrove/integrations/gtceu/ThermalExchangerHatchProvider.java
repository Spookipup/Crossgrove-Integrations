package com.crossgrove.integrations.gtceu;

import com.Da_Technomancer.crossroads.api.Capabilities;
import com.Da_Technomancer.crossroads.api.heat.HeatUtil;
import com.Da_Technomancer.crossroads.api.heat.IHeatHandler;
import com.crossgrove.integrations.CrossgroveConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ThermalExchangerHatchProvider implements ICapabilitySerializable<CompoundTag> {
    private static final String TEMPERATURE_KEY = "temperature";
    private static final String INITIALIZED_KEY = "initialized";
    private static final Set<ThermalExchangerHatchProvider> ACTIVE_PROVIDERS = ConcurrentHashMap.newKeySet();

    private final BlockEntity blockEntity;
    private final HatchHeatHandler heatHandler = new HatchHeatHandler();
    private final LazyOptional<IHeatHandler> heatCapability = LazyOptional.of(() -> heatHandler);

    private boolean initialized;
    private double temperature;

    ThermalExchangerHatchProvider(BlockEntity blockEntity) {
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
        heatCapability.invalidate();
    }

    static void tickAll(Level level) {
        Iterator<ThermalExchangerHatchProvider> iterator = ACTIVE_PROVIDERS.iterator();
        while (iterator.hasNext()) {
            ThermalExchangerHatchProvider provider = iterator.next();
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
        if (Capabilities.HEAT_CAPABILITY == null || blockEntity.isRemoved()) {
            return;
        }

        Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        IHeatHandler multiblockHeat = findLinkedMultiblockHeat(level, blockEntity.getBlockPos());
        if (multiblockHeat == null) {
            return;
        }

        double transfer = (multiblockHeat.getTemp() - heatHandler.getTemp())
                * CrossgroveConfig.thermalExchangerBodyHeatTransferRate;
        if (Math.abs(transfer) >= 0.001D) {
            multiblockHeat.addHeat(-transfer);
            heatHandler.addHeat(transfer);
        }
    }

    private @Nullable IHeatHandler findLinkedMultiblockHeat(Level level, BlockPos pos) {
        IHeatHandler ownStructureHeat = GtceuHeatProvider.findSharedMultiblockHeatAtStructureBlock(level, pos);
        if (ownStructureHeat != null) {
            return ownStructureHeat;
        }
        for (Direction direction : Direction.values()) {
            IHeatHandler heat = GtceuHeatProvider.findSharedMultiblockHeatAtStructureBlock(level, pos.relative(direction));
            if (heat != null) {
                return heat;
            }
        }
        return null;
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

    private final class HatchHeatHandler implements IHeatHandler {
        @Override
        public double getTemp() {
            init();
            return temperature;
        }

        @Override
        public void setTemp(double temperature) {
            initialized = true;
            ThermalExchangerHatchProvider.this.temperature = clampTemperature(temperature);
            markChanged();
        }

        @Override
        public void addHeat(double heat) {
            init();
            temperature = clampTemperature(temperature + heat);
            markChanged();
        }

        private void init() {
            if (initialized) {
                return;
            }
            temperature = initialTemperature();
            initialized = true;
            markChanged();
        }
    }
}
