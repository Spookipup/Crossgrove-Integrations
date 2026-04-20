package com.crossgrove.integrations.gtceu.exdeorum;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import com.Da_Technomancer.crossroads.api.Capabilities;
import thedarkcolour.exdeorum.blockentity.ETankBlockEntity;

import com.crossgrove.integrations.CrossgroveIntegrations;

public final class ExDeorumHeatBridge {
	private static final ResourceLocation CAPABILITY_ID = ResourceLocation.fromNamespaceAndPath(
			CrossgroveIntegrations.MOD_ID,
			"ex_deorum_tank_heat"
	);

	private ExDeorumHeatBridge() {
	}

	@SubscribeEvent
	public static void attachHeatCapability(AttachCapabilitiesEvent<BlockEntity> event) {
		if (Capabilities.HEAT_CAPABILITY == null || !(event.getObject() instanceof ETankBlockEntity tankBlock)) {
			return;
		}

		ExDeorumHeatProvider provider = new ExDeorumHeatProvider(tankBlock);
		event.addCapability(CAPABILITY_ID, provider);
		event.addListener(provider::invalidate);
	}

	@SubscribeEvent
	public static void tickHeatProviders(TickEvent.LevelTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) {
			return;
		}
		ExDeorumHeatProvider.tickAll(event.level);
	}
}
