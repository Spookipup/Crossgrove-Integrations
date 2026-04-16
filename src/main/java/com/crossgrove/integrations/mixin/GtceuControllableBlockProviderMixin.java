package com.crossgrove.integrations.mixin;

import com.crossgrove.integrations.GtceuHeatTooltipData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

// keeps jade from showing two different "machine is stopped" reasons
@Mixin(targets = "com.gregtechceu.gtceu.integration.jade.provider.ControllableBlockProvider", remap = false)
public abstract class GtceuControllableBlockProviderMixin {
    @Inject(method = "addTooltip", at = @At("HEAD"), cancellable = true, remap = false)
    private void crossgrove$replaceHeatSuspensionTooltip(CompoundTag data, ITooltip tooltip, Player player,
                                                        BlockAccessor accessor, BlockEntity blockEntity,
                                                        IPluginConfig config, CallbackInfo callbackInfo) {
        if (data.getBoolean("SuspendAfter") || !data.contains("WorkingEnabled") || data.getBoolean("WorkingEnabled")) {
            return;
        }

        CompoundTag serverData = accessor.getServerData();
        if (!GtceuHeatTooltipData.isHeatSuspended(serverData)) {
            return;
        }

        Component reason = GtceuHeatTooltipData.hasSafeTemperature(serverData)
                ? Component.literal("cooling below " + GtceuHeatTooltipData.formatTemperature(GtceuHeatTooltipData.getSafeTemperature(serverData)) + " C")
                : Component.literal("cooling before restart");
        tooltip.add(Component.literal("Overheated: ")
                .withStyle(ChatFormatting.RED)
                .append(reason.copy().withStyle(ChatFormatting.YELLOW)));
        callbackInfo.cancel();
    }
}
