package com.crossgrove.integrations.mixin;

import com.Da_Technomancer.crossroads.api.Capabilities;
import com.Da_Technomancer.crossroads.api.heat.HeatUtil;
import com.Da_Technomancer.crossroads.api.heat.IHeatHandler;
import com.Da_Technomancer.crossroads.api.templates.IInfoTE;
import com.Da_Technomancer.crossroads.items.OmniMeter;
import com.crossgrove.integrations.CrossgroveIntegrations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(value = OmniMeter.class, remap = false)
public abstract class OmniMeterMixin {
    @Inject(method = "measure", at = @At("TAIL"), require = 1)
    private static void crossgrove$measureHeatCapability(ArrayList<Component> chat, Player player, Level level,
                                                         BlockPos pos, Direction side, BlockHitResult hit,
                                                         CallbackInfo callbackInfo) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null || blockEntity instanceof IInfoTE) {
            return;
        }

        LazyOptional<IHeatHandler> heatCapability = blockEntity.getCapability(Capabilities.HEAT_CAPABILITY, null);
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(blockEntity.getBlockState().getBlock());
        if (!heatCapability.isPresent()) {
            CrossgroveIntegrations.LOGGER.debug("Omnimeter found no Crossroads heat capability on {}", blockId);
            return;
        }

        IHeatHandler heatHandler = heatCapability.orElseThrow(IllegalStateException::new);
        HeatUtil.addHeatInfo(chat, heatHandler.getTemp(), HeatUtil.convertBiomeTemp(level, pos));
        CrossgroveIntegrations.LOGGER.debug("Omnimeter added Crossroads heat info for {}", blockId);
    }
}
