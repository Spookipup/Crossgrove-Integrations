package com.crossgrove.integrations.gtceu;

import com.Da_Technomancer.crossroads.api.Capabilities;
import com.Da_Technomancer.crossroads.api.rotary.IAxleHandler;
import com.Da_Technomancer.crossroads.api.rotary.ICogHandler;
import com.crossgrove.integrations.CrossgroveIntegrations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Objects;

public final class GtceuCrossroadsPower {
    public static final RotaryState NO_ROTARY = new RotaryState(null, 0D, 0D);

    private GtceuCrossroadsPower() {
    }

    public static boolean isHeatPoweredMachine(@Nullable Object machine) {
        ResourceLocation blockId = getMachineBlockId(machine);
        return blockId != null && GtceuHeatProfiles.get(blockId).heatPowersMachine();
    }

    public static boolean isHeatPoweredBlock(ResourceLocation blockId) {
        return blockId != null && GtceuHeatProfiles.get(blockId).heatPowersMachine();
    }

    public static @Nullable ResourceLocation getMachineBlockId(@Nullable Object machine) {
        if (machine == null) {
            return null;
        }
        Object state = invokeNoArg(machine, "getBlockState");
        if (state instanceof BlockState blockState) {
            return ForgeRegistries.BLOCKS.getKey(blockState.getBlock());
        }
        return null;
    }

    public static RotaryState findBestRotary(Level level, BlockPos pos) {
        if (Capabilities.AXLE_CAPABILITY == null || Capabilities.COG_CAPABILITY == null) {
            return NO_ROTARY;
        }
        RotaryState best = NO_ROTARY;
        for (Direction direction : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(direction));
            if (neighbor == null) {
                continue;
            }

            Direction side = direction.getOpposite();
            RotaryState axleState = readAxle(neighbor.getCapability(Capabilities.AXLE_CAPABILITY, side));
            best = pickBetter(best, axleState);

            RotaryState cogState = readCog(neighbor.getCapability(Capabilities.COG_CAPABILITY, side));
            best = pickBetter(best, cogState);
        }
        return best;
    }

    public static RotaryState findBestMultiblockRotary(Level level, @Nullable Object machine, RotaryState currentBest) {
        if (machine == null || !booleanCall(machine, "isFormed", false) || !isMultiController(machine)) {
            return currentBest;
        }

        RotaryState best = currentBest;
        Object parts = invokeNoArg(machine, "getParts");
        if (!(parts instanceof Iterable<?> iterable)) {
            return best;
        }

        for (Object part : iterable) {
            Object pos = invokeNoArg(part, "getPos");
            if (!(pos instanceof BlockPos blockPos)) {
                continue;
            }
            RotaryHatchProvider provider = RotaryHatchProvider.find(level.getBlockEntity(blockPos));
            if (provider == null || !provider.isInput()) {
                continue;
            }
            best = pickBetter(best, provider.readState());
        }
        return best;
    }

    public static boolean consumeRotaryEnergy(RotaryState state, double energy) {
        if (energy <= 0D) {
            return true;
        }
        IAxleHandler axle = state.axle();
        if (axle == null || axle.getEnergy() < energy) {
            return false;
        }
        axle.addEnergy(-energy, false);
        return true;
    }

    private static RotaryState readAxle(LazyOptional<IAxleHandler> optional) {
        return optional.resolve()
                .map(GtceuCrossroadsPower::rotaryStateForAxle)
                .orElse(NO_ROTARY);
    }

    private static RotaryState readCog(LazyOptional<ICogHandler> optional) {
        return optional.resolve()
                .map(ICogHandler::getAxle)
                .filter(Objects::nonNull)
                .map(GtceuCrossroadsPower::rotaryStateForAxle)
                .orElse(NO_ROTARY);
    }

    public static RotaryState rotaryStateForAxle(IAxleHandler axle) {
        return new RotaryState(axle, Math.abs(axle.getSpeed()), Math.max(0D, axle.getEnergy()));
    }

    private static RotaryState pickBetter(RotaryState current, RotaryState candidate) {
        return Comparator.comparingDouble(RotaryState::speed)
                .thenComparingDouble(RotaryState::energy)
                .compare(candidate, current) > 0 ? candidate : current;
    }

    private static boolean isMultiController(Object machine) {
        return implementsNamedInterface(machine.getClass(), "com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController");
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

    private static boolean booleanCall(Object target, String methodName, boolean fallback) {
        Object value = invokeNoArg(target, methodName);
        return value instanceof Boolean booleanValue ? booleanValue : fallback;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            CrossgroveIntegrations.LOGGER.debug("Failed reflective GTCEu power call {} on {}", methodName, target.getClass().getName(), exception);
            return null;
        }
    }

    public record RotaryState(@Nullable IAxleHandler axle, double speed, double energy) {
    }
}
