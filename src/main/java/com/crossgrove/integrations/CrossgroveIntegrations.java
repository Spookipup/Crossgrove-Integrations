package com.crossgrove.integrations;

import com.mojang.logging.LogUtils;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.slf4j.Logger;

import com.crossgrove.integrations.casting.CastingMoldItems;
import com.crossgrove.integrations.casting.ExDeorumCastingHandler;
import com.crossgrove.integrations.ceramic.CeramicItems;
import com.crossgrove.integrations.crop.AgriCraftHeatBridge;
import com.crossgrove.integrations.crop.CropHeatBridge;
import com.crossgrove.integrations.gtceu.GtceuHeatBridge;
import com.crossgrove.integrations.gtceu.exdeorum.ExDeorumCrucibleOverrides;
import com.crossgrove.integrations.gtceu.exdeorum.ExDeorumHeatBridge;
import com.crossgrove.integrations.gtceu.overgeared.OvergearedRecipeReloadListener;
import com.crossgrove.integrations.metallurgy.MetallurgyItems;
import com.crossgrove.integrations.portal.AetherPortalHandler;
import com.crossgrove.integrations.portal.EndPortalHandler;
import com.crossgrove.integrations.portal.NetherPortalHandler;
import com.crossgrove.integrations.survival.SurvivalistOvergearedRockDrops;

@Mod(CrossgroveIntegrations.MOD_ID)
public final class CrossgroveIntegrations {
	public static final String MOD_ID = "crossgrove_integrations";
	public static final Logger LOGGER = LogUtils.getLogger();

	public CrossgroveIntegrations(FMLJavaModLoadingContext context) {
		context.registerConfig(ModConfig.Type.COMMON, CrossgroveConfig.SPEC);
		CastingMoldItems.register(context.getModEventBus());
		CeramicItems.register(context.getModEventBus());
		MetallurgyItems.register(context.getModEventBus());
		MinecraftForge.EVENT_BUS.register(GtceuHeatBridge.class);
		MinecraftForge.EVENT_BUS.register(CropHeatBridge.class);
		MinecraftForge.EVENT_BUS.register(EndPortalHandler.class);
		MinecraftForge.EVENT_BUS.register(NetherPortalHandler.class);
		if (ModList.get().isLoaded("agricraft")) {
			AgriCraftHeatBridge.register();
		}
		if (ModList.get().isLoaded("aether")) {
			MinecraftForge.EVENT_BUS.register(AetherPortalHandler.class);
		}
		if (ModList.get().isLoaded("gtceu") && ModList.get().isLoaded("overgeared")) {
			MinecraftForge.EVENT_BUS.register(CrossgroveIntegrations.class);
		}
		if (ModList.get().isLoaded("survivalistessentials") && ModList.get().isLoaded("overgeared")) {
			MinecraftForge.EVENT_BUS.register(SurvivalistOvergearedRockDrops.class);
		}
		if (ModList.get().isLoaded("gtceu") && ModList.get().isLoaded("exdeorum")) {
			context.getModEventBus().addListener(ExDeorumCrucibleOverrides::onCommonSetup);
			MinecraftForge.EVENT_BUS.register(ExDeorumCastingHandler.class);
			MinecraftForge.EVENT_BUS.register(ExDeorumHeatBridge.class);
		}
	}

	@SubscribeEvent
	public static void onAddReloadListeners(AddReloadListenerEvent event) {
		event.addListener(new OvergearedRecipeReloadListener(event.getServerResources().getRecipeManager()));
	}
}
