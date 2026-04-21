package com.crossgrove.integrations.gtceu;

import com.Da_Technomancer.crossroads.api.Capabilities;
import com.crossgrove.integrations.CrossgroveConfig;
import com.crossgrove.integrations.CrossgroveIntegrations;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public final class GtceuHeatBridge {
    private static final ResourceLocation CAPABILITY_ID = Objects.requireNonNull(
            ResourceLocation.tryParse(CrossgroveIntegrations.MOD_ID + ":gtceu_heat")
    );
    private static final ResourceLocation THERMAL_EXCHANGER_CAPABILITY_ID = Objects.requireNonNull(
            ResourceLocation.tryParse(CrossgroveIntegrations.MOD_ID + ":thermal_exchanger_heat")
    );
    private static final ResourceLocation ROTARY_INPUT_CAPABILITY_ID = Objects.requireNonNull(
            ResourceLocation.tryParse(CrossgroveIntegrations.MOD_ID + ":rotary_input")
    );
    private static final ResourceLocation ROTARY_OUTPUT_CAPABILITY_ID = Objects.requireNonNull(
            ResourceLocation.tryParse(CrossgroveIntegrations.MOD_ID + ":rotary_output")
    );
    private static final ResourceLocation THERMAL_EXCHANGER_HATCH_ID = Objects.requireNonNull(
            ResourceLocation.tryParse(CrossgroveIntegrations.MOD_ID + ":thermal_exchanger_hatch")
    );
    private static final ResourceLocation ROTARY_INPUT_HATCH_ID = Objects.requireNonNull(
            ResourceLocation.tryParse(CrossgroveIntegrations.MOD_ID + ":rotary_input_hatch")
    );
    private static final ResourceLocation ROTARY_OUTPUT_HATCH_ID = Objects.requireNonNull(
            ResourceLocation.tryParse(CrossgroveIntegrations.MOD_ID + ":rotary_output_hatch")
    );
    private static final String GTCEU_NAMESPACE = "gtceu";
    private static final String GTCEU_MULTIBLOCK_PART = "com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart";

    private GtceuHeatBridge() {
    }

    @SubscribeEvent
    public static void attachHeatCapability(AttachCapabilitiesEvent<BlockEntity> event) {
        if (!CrossgroveConfig.enableGtceuHeatBridge) {
            return;
        }

        BlockEntity blockEntity = event.getObject();
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(blockEntity.getBlockState().getBlock());
        if (ROTARY_INPUT_HATCH_ID.equals(blockId)) {
            if (Capabilities.AXLE_CAPABILITY != null || Capabilities.COG_CAPABILITY != null) {
                RotaryHatchProvider provider = new RotaryHatchProvider(blockEntity, RotaryHatchMode.INPUT);
                event.addCapability(ROTARY_INPUT_CAPABILITY_ID, provider);
                event.addListener(provider::invalidate);
                CrossgroveIntegrations.LOGGER.debug("Attached Crossroads rotary capability to rotary input hatch");
            }
            return;
        }
        if (ROTARY_OUTPUT_HATCH_ID.equals(blockId)) {
            if (Capabilities.AXLE_CAPABILITY != null || Capabilities.COG_CAPABILITY != null) {
                RotaryHatchProvider provider = new RotaryHatchProvider(blockEntity, RotaryHatchMode.OUTPUT);
                event.addCapability(ROTARY_OUTPUT_CAPABILITY_ID, provider);
                event.addListener(provider::invalidate);
                CrossgroveIntegrations.LOGGER.debug("Attached Crossroads rotary capability to rotary output hatch");
            }
            return;
        }
        if (Capabilities.HEAT_CAPABILITY == null) {
            return;
        }
        if (THERMAL_EXCHANGER_HATCH_ID.equals(blockId)) {
            ThermalExchangerHatchProvider provider = new ThermalExchangerHatchProvider(blockEntity);
            event.addCapability(THERMAL_EXCHANGER_CAPABILITY_ID, provider);
            event.addListener(provider::invalidate);
            CrossgroveIntegrations.LOGGER.debug("Attached Crossroads heat capability to thermal exchanger hatch");
            return;
        }

        if (!isSelectedGtceuMachine(blockId)
                && !(CrossgroveConfig.shareMultiblockHeat && isGtceuMultiblockPart(blockEntity))) {
            return;
        }

        GtceuHeatProvider provider = new GtceuHeatProvider(blockEntity);
        event.addCapability(CAPABILITY_ID, provider);
        event.addListener(provider::invalidate);
        CrossgroveIntegrations.LOGGER.debug("Attached Crossroads heat capability to GTCEu block {}", blockId);
    }

    @SubscribeEvent
    public static void tickHeatProviders(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) {
            return;
        }
        GtceuHeatProvider.tickAll(event.level);
        ThermalExchangerHatchProvider.tickAll(event.level);
    }

    static boolean isSelectedGtceuMachine(ResourceLocation blockId) {
        if (blockId == null) {
            return false;
        }
        if (CrossgroveConfig.disabledGtceuBlocks.contains(blockId)) {
            return false;
        }
        if (CrossgroveConfig.enabledGtceuBlocks.contains(blockId)) {
            return true;
        }
        if (CrossgroveConfig.highTemperatureGtceuBlocks.contains(blockId)) {
            return true;
        }
        if (GtceuHeatProfiles.isProfiled(blockId)) {
            return true;
        }
        if (!GTCEU_NAMESPACE.equals(blockId.getNamespace())) {
            return false;
        }

        String path = blockId.getPath();
        return CrossgroveConfig.enabledGtceuPathSuffixes.stream().anyMatch(path::endsWith);
    }

    private static boolean isGtceuMultiblockPart(BlockEntity blockEntity) {
        Object machine = invokeNoArg(blockEntity, "getMetaMachine");
        return machine != null && implementsNamedInterface(machine.getClass(), GTCEU_MULTIBLOCK_PART);
    }

    private static boolean implementsNamedInterface(Class<?> type, String interfaceName) {
        Class<?> cursor = type;
        while (cursor != null) {
            for (Class<?> candidate : cursor.getInterfaces()) {
                if (implementsNamedInterface(candidate, interfaceName)) {
                    return true;
                }
            }
            if (interfaceName.equals(cursor.getName())) {
                return true;
            }
            cursor = cursor.getSuperclass();
        }
        return false;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            return null;
        }
    }
}
