package com.crossgrove.integrations;

import com.Da_Technomancer.crossroads.api.Capabilities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;

public final class GtceuHeatBridge {
    private static final ResourceLocation CAPABILITY_ID = Objects.requireNonNull(
            ResourceLocation.tryParse(CrossgroveIntegrations.MOD_ID + ":gtceu_heat")
    );
    private static final String GTCEU_NAMESPACE = "gtceu";

    private GtceuHeatBridge() {
    }

    @SubscribeEvent
    public static void attachHeatCapability(AttachCapabilitiesEvent<BlockEntity> event) {
        if (!CrossgroveConfig.enableGtceuHeatBridge || Capabilities.HEAT_CAPABILITY == null) {
            return;
        }

        BlockEntity blockEntity = event.getObject();
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(blockEntity.getBlockState().getBlock());
        if (!isSelectedGtceuMachine(blockId)) {
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
    }

    private static boolean isSelectedGtceuMachine(ResourceLocation blockId) {
        if (blockId == null || !GTCEU_NAMESPACE.equals(blockId.getNamespace())) {
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

        String path = blockId.getPath();
        return CrossgroveConfig.enabledGtceuPathSuffixes.stream().anyMatch(path::endsWith);
    }
}
