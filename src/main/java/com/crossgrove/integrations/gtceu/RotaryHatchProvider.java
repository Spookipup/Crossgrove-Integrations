package com.crossgrove.integrations.gtceu;

import com.Da_Technomancer.crossroads.api.Capabilities;
import com.Da_Technomancer.crossroads.api.rotary.IAxisHandler;
import com.Da_Technomancer.crossroads.api.rotary.IAxleHandler;
import com.Da_Technomancer.crossroads.api.rotary.ICogHandler;
import com.Da_Technomancer.crossroads.api.rotary.RotaryUtil;
import com.crossgrove.integrations.CrossgroveIntegrations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RotaryHatchProvider implements ICapabilitySerializable<CompoundTag> {
    private static final String ENERGY_KEY = "energy";
    private static final String GTCEU_MULTIBLOCK_PART = "com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart";
    private static final Map<BlockEntity, RotaryHatchProvider> ACTIVE_BY_BLOCK_ENTITY = new ConcurrentHashMap<>();
    private static final Map<BlockEntity, SharedRotaryState> SHARED_BY_CONTROLLER = new ConcurrentHashMap<>();

    private final BlockEntity blockEntity;
    private final RotaryHatchMode mode;
    private final SharedRotaryState standaloneState = new SharedRotaryState();
    private final HatchAxleHandler axleHandler = new HatchAxleHandler();
    private final HatchCogHandler cogHandler = new HatchCogHandler();
    private final LazyOptional<IAxleHandler> axleCapability = LazyOptional.of(() -> axleHandler);
    private final LazyOptional<ICogHandler> cogCapability = LazyOptional.of(() -> cogHandler);

    RotaryHatchProvider(BlockEntity blockEntity, RotaryHatchMode mode) {
        this.blockEntity = blockEntity;
        this.mode = mode;
        ACTIVE_BY_BLOCK_ENTITY.put(blockEntity, this);
    }

    public static @Nullable RotaryHatchProvider find(@Nullable BlockEntity blockEntity) {
        return blockEntity == null ? null : ACTIVE_BY_BLOCK_ENTITY.get(blockEntity);
    }

    public boolean isInput() {
        return mode == RotaryHatchMode.INPUT;
    }

    public GtceuCrossroadsPower.RotaryState readState() {
        return GtceuCrossroadsPower.rotaryStateForAxle(axleHandler);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        if (side != null && side != getFrontFacing()) {
            return LazyOptional.empty();
        }
        if (Capabilities.AXLE_CAPABILITY != null && capability == Capabilities.AXLE_CAPABILITY) {
            return axleCapability.cast();
        }
        if (Capabilities.COG_CAPABILITY != null && capability == Capabilities.COG_CAPABILITY) {
            return cogCapability.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble(ENERGY_KEY, getSharedState().energy);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        standaloneState.energy = tag.getDouble(ENERGY_KEY);
    }

    public void invalidate() {
        ACTIVE_BY_BLOCK_ENTITY.remove(blockEntity);
        axleCapability.invalidate();
        cogCapability.invalidate();
    }

    private SharedRotaryState getSharedState() {
        BlockEntity controllerBlockEntity = getLinkedControllerBlockEntity();
        if (controllerBlockEntity == null) {
            return standaloneState;
        }
        SharedRotaryState sharedState = SHARED_BY_CONTROLLER.computeIfAbsent(controllerBlockEntity, ignored -> new SharedRotaryState());
        sharedState.seedFrom(standaloneState.energy);
        return sharedState;
    }

    private List<RotaryHatchProvider> getLinkedHatches() {
        List<RotaryHatchProvider> providers = new ArrayList<>();
        providers.add(this);

        Object controller = getLinkedControllerMachine();
        if (controller == null) {
            return providers;
        }

        Level level = blockEntity.getLevel();
        if (level == null) {
            return providers;
        }

        Object parts = invokeNoArg(controller, "getParts");
        if (!(parts instanceof Iterable<?> iterable)) {
            return providers;
        }
        for (Object part : iterable) {
            Object pos = invokeNoArg(part, "getPos");
            if (!(pos instanceof BlockPos blockPos)) {
                continue;
            }
            RotaryHatchProvider provider = ACTIVE_BY_BLOCK_ENTITY.get(level.getBlockEntity(blockPos));
            if (provider != null && !providers.contains(provider)) {
                providers.add(provider);
            }
        }
        return providers;
    }

    private @Nullable Object getLinkedControllerMachine() {
        Object machine = getMetaMachine();
        if (machine == null || !implementsNamedInterface(machine.getClass(), GTCEU_MULTIBLOCK_PART) || !booleanCall(machine, "isFormed", false)) {
            return null;
        }
        Object controllers = invokeNoArg(machine, "getControllers");
        if (!(controllers instanceof Iterable<?> iterable)) {
            return null;
        }
        for (Object controller : iterable) {
            if (controller != null && booleanCall(controller, "isFormed", false)) {
                return controller;
            }
        }
        return null;
    }

    private @Nullable BlockEntity getLinkedControllerBlockEntity() {
        Object controller = getLinkedControllerMachine();
        if (controller == null) {
            return null;
        }
        Level level = blockEntity.getLevel();
        Object pos = invokeNoArg(controller, "getPos");
        return level != null && pos instanceof BlockPos blockPos ? level.getBlockEntity(blockPos) : null;
    }

    private @Nullable BlockEntity getFrontNeighbor() {
        Level level = blockEntity.getLevel();
        return level == null ? null : level.getBlockEntity(blockEntity.getBlockPos().relative(getFrontFacing()));
    }

    private Direction getFrontFacing() {
        BlockState blockState = blockEntity.getBlockState();
        return blockState.hasProperty(BlockStateProperties.FACING)
                ? blockState.getValue(BlockStateProperties.FACING)
                : Direction.NORTH;
    }

    private Object getMetaMachine() {
        return invokeNoArg(blockEntity, "getMetaMachine");
    }

    private void markChanged() {
        blockEntity.setChanged();
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

    private static Object invokeNoArg(Object target, String methodName) {
        return invoke(target, methodName, new Class<?>[0]);
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = findMethod(target.getClass(), methodName, parameterTypes);
            if (method == null) {
                return null;
            }
            return method.invoke(target, args);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            CrossgroveIntegrations.LOGGER.debug("Failed reflective GTCEu rotary call {} on {}", methodName, target.getClass().getName(), exception);
            return null;
        }
    }

    private static @Nullable Method findMethod(Class<?> type, String methodName, Class<?>[] parameterTypes) {
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

    private final class HatchAxleHandler implements IAxleHandler {
        private double rotationRatio = 1D;
        private byte updateKey;
        private boolean renderOffset;
        private @Nullable IAxisHandler axis;

        @Override
        public double getSpeed() {
            return axis == null ? 0D : rotationRatio * axis.getBaseSpeed();
        }

        @Override
        public double getEnergy() {
            return getSharedState().energy;
        }

        @Override
        public void setEnergy(double energy) {
            SharedRotaryState sharedState = getSharedState();
            if (Double.compare(sharedState.energy, energy) == 0) {
                return;
            }
            sharedState.energy = energy;
            markChanged();
        }

        @Override
        public double getMoInertia() {
            return 0D;
        }

        @Override
        public double getRotationRatio() {
            return rotationRatio;
        }

        @Override
        public float getAngle(float partialTicks) {
            return axis == null ? 0F : axis.getAngle(rotationRatio, partialTicks, renderOffset, 22.5F);
        }

        @Override
        public void propagate(IAxisHandler axis, byte key, double rotationRatio, double lastRadius, boolean renderOffset) {
            if (key == updateKey || axis.addToList(this)) {
                return;
            }

            this.rotationRatio = rotationRatio == 0D ? 1D : rotationRatio;
            this.renderOffset = renderOffset;
            this.updateKey = key;
            this.axis = axis;

            for (RotaryHatchProvider provider : getLinkedHatches()) {
                if (provider != RotaryHatchProvider.this) {
                    provider.axleHandler.propagate(axis, key, this.rotationRatio, lastRadius, renderOffset);
                }
            }

            BlockEntity neighbor = getFrontNeighbor();
            if (neighbor != null) {
                RotaryUtil.propagateAxially(neighbor, getFrontFacing().getOpposite(), this, axis, key, renderOffset);
            }
        }

        @Override
        public void disconnect() {
            axis = null;
        }
    }

    private final class HatchCogHandler implements ICogHandler {
        @Override
        public void connect(IAxisHandler axis, byte key, double rotationRatio, double lastRadius, Direction side, boolean renderOffset) {
            axleHandler.propagate(axis, key, rotationRatio, lastRadius, !renderOffset);
        }

        @Override
        public IAxleHandler getAxle() {
            return axleHandler;
        }
    }

    private static final class SharedRotaryState {
        private double energy;

        private void seedFrom(double energy) {
            if (Math.abs(energy) > Math.abs(this.energy)) {
                this.energy = energy;
            }
        }
    }
}
