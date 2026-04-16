package com.crossgrove.integrations;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

@Mod(CrossgroveIntegrations.MOD_ID)
public final class CrossgroveIntegrations {
    public static final String MOD_ID = "crossgrove_integrations";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CrossgroveIntegrations(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, CrossgroveConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(GtceuHeatBridge.class);
        MinecraftForge.EVENT_BUS.register(CropHeatBridge.class);
        if (ModList.get().isLoaded("agricraft")) {
            AgriCraftHeatBridge.register();
        }
    }
}
