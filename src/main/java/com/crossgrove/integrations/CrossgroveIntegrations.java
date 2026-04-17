package com.crossgrove.integrations;

import com.crossgrove.integrations.crop.AgriCraftHeatBridge;
import com.crossgrove.integrations.crop.CropHeatBridge;
import com.crossgrove.integrations.gtceu.GtceuHeatBridge;
import com.crossgrove.integrations.gtceu.overgeared.OvergearedRecipeReloadListener;
import com.crossgrove.integrations.portal.AetherPortalHandler;
import com.crossgrove.integrations.portal.EndPortalHandler;
import com.crossgrove.integrations.portal.NetherPortalHandler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(CrossgroveIntegrations.MOD_ID)
public final class CrossgroveIntegrations {
    public static final String MOD_ID = "crossgrove_integrations";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CrossgroveIntegrations(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, CrossgroveConfig.SPEC);
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
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new OvergearedRecipeReloadListener(event.getServerResources().getRecipeManager()));
    }
}
