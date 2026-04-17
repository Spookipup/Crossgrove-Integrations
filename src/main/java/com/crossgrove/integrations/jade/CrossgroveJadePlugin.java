package com.crossgrove.integrations.jade;

import com.Da_Technomancer.crossroads.CRConfig;
import com.Da_Technomancer.crossroads.api.heat.HeatUtil;
import com.crossgrove.integrations.CrossgroveIntegrations;
import com.crossgrove.integrations.gtceu.GtceuHeatProvider;
import com.crossgrove.integrations.gtceu.GtceuHeatTooltipData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

import java.util.ArrayList;
import java.util.List;

@WailaPlugin(CrossgroveIntegrations.MOD_ID)
public final class CrossgroveJadePlugin implements IWailaPlugin {
    private static final GtceuHeatTooltipProvider HEAT_PROVIDER = new GtceuHeatTooltipProvider();

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(HEAT_PROVIDER, BlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addConfig(GtceuHeatTooltipProvider.UID, true);
        registration.addConfig(GtceuHeatTooltipProvider.POWER_LABEL_UID, false);
        registration.registerBlockComponent(HEAT_PROVIDER, Block.class);
    }

    private static final class GtceuHeatTooltipProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        private static final ResourceLocation UID = GtceuHeatTooltipData.UID;
        private static final ResourceLocation POWER_LABEL_UID = ResourceLocation.fromNamespaceAndPath(
                CrossgroveIntegrations.MOD_ID,
                "gtceu_crossroads_power_label"
        );

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            BlockEntity blockEntity = accessor.getBlockEntity();
            if (blockEntity == null) {
                return;
            }
            GtceuHeatProvider.writeTooltipData(blockEntity, data);
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag serverData = accessor.getServerData();
            if (!GtceuHeatTooltipData.hasHeat(serverData)) {
                return;
            }

            double temperature = GtceuHeatTooltipData.getTemperature(serverData);
            addCrossroadsHeatInfo(tooltip, accessor, temperature);

            if (GtceuHeatTooltipData.hasSharedMultiblockHeat(serverData)) {
                tooltip.add(Component.literal("Shared heat body: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(GtceuHeatTooltipData.getMultiblockHeatParts(serverData)
                                + " ports / "
                                + GtceuHeatTooltipData.getMultiblockStructureBlocks(serverData)
                                + " blocks")
                                .withStyle(ChatFormatting.GREEN)));
            }

            if (GtceuHeatTooltipData.heatPowersMachine(serverData) && config.get(POWER_LABEL_UID)) {
                tooltip.add(Component.literal("Power: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(crossroadsPowerText(serverData))
                                .withStyle(ChatFormatting.GOLD)));
            }

            if (GtceuHeatTooltipData.hasMinimumWorkingTemperature(serverData)) {
                double minimum = GtceuHeatTooltipData.getMinimumWorkingTemperature(serverData);
                tooltip.add(Component.literal("Working heat: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(GtceuHeatTooltipData.formatTemperature(minimum) + " C min")
                                .withStyle(temperature >= minimum ? ChatFormatting.GREEN : ChatFormatting.RED)));
            }

            if (GtceuHeatTooltipData.hasIdealTemperatureRange(serverData)) {
                tooltip.add(Component.literal("Ideal heat: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(GtceuHeatTooltipData.formatTemperature(GtceuHeatTooltipData.getIdealMinTemperature(serverData))
                                + "-" + GtceuHeatTooltipData.formatTemperature(GtceuHeatTooltipData.getIdealMaxTemperature(serverData)) + " C")
                                .withStyle(colorForIdealRange(temperature, serverData))));
            }

            if (GtceuHeatTooltipData.hasSafeTemperature(serverData)) {
                tooltip.add(Component.literal("Safe below: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(GtceuHeatTooltipData.formatTemperature(GtceuHeatTooltipData.getSafeTemperature(serverData)) + " C")
                                .withStyle(ChatFormatting.YELLOW)));
            }

            if (GtceuHeatTooltipData.hasMinimumRotarySpeed(serverData)) {
                double speed = GtceuHeatTooltipData.getRotarySpeed(serverData);
                double minimum = GtceuHeatTooltipData.getMinimumRotarySpeed(serverData);
                addCrossroadsRotaryInfo(tooltip, speed, GtceuHeatTooltipData.getRotaryEnergy(serverData));
                tooltip.add(Component.literal("Required speed: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(formatRotarySpeed(minimum) + " rad/s")
                                .withStyle(speed >= minimum ? ChatFormatting.GREEN : ChatFormatting.RED)));
            }

            if (GtceuHeatTooltipData.hasIdealRotarySpeedRange(serverData)) {
                tooltip.add(Component.literal("Ideal rotary: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(formatRotarySpeed(GtceuHeatTooltipData.getIdealMinRotarySpeed(serverData))
                                + "-" + formatRotarySpeed(GtceuHeatTooltipData.getIdealMaxRotarySpeed(serverData)) + " rad/s")
                                .withStyle(colorForIdealRotaryRange(GtceuHeatTooltipData.getRotarySpeed(serverData), serverData))));
            }

            if (GtceuHeatTooltipData.isHeatSuspended(serverData)) {
                tooltip.add(Component.literal(statusText(serverData))
                        .withStyle(statusColor(serverData)));
            }
        }

        private static void addCrossroadsHeatInfo(ITooltip tooltip, BlockAccessor accessor, double temperature) {
            List<Component> heatInfo = new ArrayList<>();
            HeatUtil.addHeatInfo(heatInfo, temperature, HeatUtil.convertBiomeTemp(accessor.getLevel(), accessor.getPosition()));
            heatInfo.forEach(tooltip::add);
        }

        private static void addCrossroadsRotaryInfo(ITooltip tooltip, double speed, double energy) {
            tooltip.add(Component.translatable("tt.crossroads.boilerplate.rotary.speed",
                    CRConfig.formatVal(speed),
                    CRConfig.formatVal(speed * 60D / (2D * Math.PI))));
            tooltip.add(Component.translatable("tt.crossroads.boilerplate.rotary.energy", CRConfig.formatVal(energy)));
        }

        private static ChatFormatting colorForIdealRange(double temperature, CompoundTag serverData) {
            double idealMin = GtceuHeatTooltipData.getIdealMinTemperature(serverData);
            double idealMax = GtceuHeatTooltipData.getIdealMaxTemperature(serverData);
            return temperature >= idealMin && temperature <= idealMax ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
        }

        private static String statusText(CompoundTag serverData) {
            return switch (GtceuHeatTooltipData.getHeatSuspensionReason(serverData)) {
                case "too_cold" -> "Status: waiting for heat";
                case "too_hot" -> "Status: heat suspended";
                case "no_rotary" -> "Status: waiting for rotary power";
                default -> "Status: heat limited";
            };
        }

        private static ChatFormatting statusColor(CompoundTag serverData) {
            return switch (GtceuHeatTooltipData.getHeatSuspensionReason(serverData)) {
                case "too_cold" -> ChatFormatting.AQUA;
                case "too_hot" -> ChatFormatting.RED;
                case "no_rotary" -> ChatFormatting.LIGHT_PURPLE;
                default -> ChatFormatting.YELLOW;
            };
        }

        private static ChatFormatting colorForIdealRotaryRange(double speed, CompoundTag serverData) {
            double idealMin = GtceuHeatTooltipData.getIdealMinRotarySpeed(serverData);
            double idealMax = GtceuHeatTooltipData.getIdealMaxRotarySpeed(serverData);
            return speed >= idealMin && speed <= idealMax ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
        }

        private static String formatRotarySpeed(double speed) {
            return CRConfig.formatVal(speed);
        }

        private static String crossroadsPowerText(CompoundTag serverData) {
            boolean heat = GtceuHeatTooltipData.hasMinimumWorkingTemperature(serverData);
            boolean rotary = GtceuHeatTooltipData.hasMinimumRotarySpeed(serverData);
            if (heat && rotary) {
                return "heat + rotary";
            }
            if (rotary) {
                return "rotary";
            }
            return "heat";
        }
    }
}
