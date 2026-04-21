package com.crossgrove.integrations.gtceu;

import com.crossgrove.integrations.CrossgroveIntegrations;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ThermalExchangerPatternSupport {
    private static final ResourceLocation THERMAL_EXCHANGER_HATCH_ID = Objects.requireNonNull(
            ResourceLocation.tryParse(CrossgroveIntegrations.MOD_ID + ":thermal_exchanger_hatch")
    );
    private static final ResourceLocation ROTARY_INPUT_HATCH_ID = Objects.requireNonNull(
            ResourceLocation.tryParse(CrossgroveIntegrations.MOD_ID + ":rotary_input_hatch")
    );
    private static final ResourceLocation ROTARY_OUTPUT_HATCH_ID = Objects.requireNonNull(
            ResourceLocation.tryParse(CrossgroveIntegrations.MOD_ID + ":rotary_output_hatch")
    );
    private static final Set<ResourceLocation> CROSSGROVE_MULTIBLOCK_PART_IDS = Set.of(
            THERMAL_EXCHANGER_HATCH_ID,
            ROTARY_INPUT_HATCH_ID,
            ROTARY_OUTPUT_HATCH_ID
    );
    private static final AtomicBoolean LOGGED_AUTO_ABILITY_HOOK = new AtomicBoolean();
    private static final AtomicBoolean LOGGED_CASING_HOOK = new AtomicBoolean();

    private ThermalExchangerPatternSupport() {
    }

    public static TraceabilityPredicate withCrossgroveParts(TraceabilityPredicate original) {
        if (original == null) {
            return null;
        }
        if (LOGGED_AUTO_ABILITY_HOOK.compareAndSet(false, true)) {
            CrossgroveIntegrations.LOGGER.info("Allowing CrossGrove multiblock hatches in GTCEu auto-ability multiblock slots");
        }
        return original.or(crossgrovePartPredicate());
    }

    private static TraceabilityPredicate crossgrovePartPredicate() {
        return new TraceabilityPredicate(
                ThermalExchangerPatternSupport::isCrossgroveMultiblockPart,
                () -> new BlockInfo[0]
        );
    }

    public static void logCasingHook() {
        if (LOGGED_CASING_HOOK.compareAndSet(false, true)) {
            CrossgroveIntegrations.LOGGER.info("Allowing CrossGrove multiblock hatches to count as GTCEu casing multiblock blocks");
        }
    }

    public static boolean isCrossgroveMultiblockPart(MultiblockState state) {
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlockState().getBlock());
        return CROSSGROVE_MULTIBLOCK_PART_IDS.contains(blockId);
    }

    public static boolean containsGtceuCasing(Block[] blocks) {
        if (blocks == null) {
            return false;
        }
        for (Block block : blocks) {
            if (isGtceuCasing(block)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGtceuCasing(Block block) {
        if (block == null) {
            return false;
        }
        ResourceLocation forgeId = ForgeRegistries.BLOCKS.getKey(block);
        if (isGtceuCasingId(forgeId)) {
            return true;
        }
        ResourceLocation builtInId = BuiltInRegistries.BLOCK.getKey(block);
        if (isGtceuCasingId(builtInId)) {
            return true;
        }
        String descriptionId = block.getDescriptionId();
        return descriptionId != null
                && descriptionId.startsWith("block.gtceu.")
                && descriptionId.contains("casing");
    }

    private static boolean isGtceuCasingId(ResourceLocation blockId) {
        return blockId != null
                && "gtceu".equals(blockId.getNamespace())
                && blockId.getPath().contains("casing");
    }
}
