package com.crossgrove.integrations.mixin;

import com.crossgrove.integrations.gtceu.GtceuCrossroadsPower;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

@Mixin(targets = "com.gregtechceu.gtceu.integration.jade.provider.ElectricContainerBlockProvider", remap = false)
public abstract class GtceuElectricContainerBlockProviderMixin {
    @Inject(method = "addTooltip", at = @At("HEAD"), cancellable = true, remap = false)
    private void crossgrove$hideEuTooltipForHeatPoweredMachines(CompoundTag data, ITooltip tooltip, Player player,
                                                               BlockAccessor accessor, BlockEntity blockEntity,
                                                               IPluginConfig config, CallbackInfo callbackInfo) {
        if (blockEntity == null) {
            return;
        }
        if (GtceuCrossroadsPower.isHeatPoweredBlock(ForgeRegistries.BLOCKS.getKey(blockEntity.getBlockState().getBlock()))) {
            callbackInfo.cancel();
        }
    }
}
