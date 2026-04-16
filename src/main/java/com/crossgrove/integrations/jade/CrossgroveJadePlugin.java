package com.crossgrove.integrations.jade;

import com.crossgrove.integrations.CrossgroveIntegrations;
import com.crossgrove.integrations.GtceuHeatProvider;
import com.crossgrove.integrations.GtceuHeatTooltipData;
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
        registration.registerBlockComponent(HEAT_PROVIDER, Block.class);
    }

    private static final class GtceuHeatTooltipProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        private static final ResourceLocation UID = GtceuHeatTooltipData.UID;

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
            tooltip.add(Component.literal("Heat: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(GtceuHeatTooltipData.formatTemperature(temperature) + " C")
                            .withStyle(colorForTemperature(temperature))));
        }

        private static ChatFormatting colorForTemperature(double temperature) {
            if (temperature >= 1000D) {
                return ChatFormatting.RED;
            }
            if (temperature >= 500D) {
                return ChatFormatting.GOLD;
            }
            if (temperature >= 100D) {
                return ChatFormatting.YELLOW;
            }
            return ChatFormatting.AQUA;
        }
    }
}
