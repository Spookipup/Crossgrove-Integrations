package com.crossgrove.integrations.gtceu;

import com.crossgrove.integrations.CrossgroveIntegrations;
import com.crossgrove.integrations.gtceu.overgeared.CrossgroveOvergearedTagPrefixes;
import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import net.minecraftforge.fml.loading.FMLLoader;

@GTAddon
public final class CrossgroveGtceuAddon implements IGTAddon {
    public CrossgroveGtceuAddon() {
        CrossgroveGtceuMachines.queueMachineBuilders();
    }

    @Override
    public GTRegistrate getRegistrate() {
        return CrossgroveGtceuMachines.REGISTRATE;
    }

    @Override
    public void initializeAddon() {
        CrossgroveGtceuMachines.registerRegistrate();
    }

    @Override
    public void registerTagPrefixes() {
        CrossgroveMetalStockTagPrefixes.init();
        if (FMLLoader.getLoadingModList().getModFileById("overgeared") != null) {
            CrossgroveOvergearedTagPrefixes.init();
        }
    }

    @Override
    public void registerMaterials() {
        CrossgroveMetalStockTagPrefixes.registerNativeMaterialFlags();
    }

    @Override
    public String addonModId() {
        return CrossgroveIntegrations.MOD_ID;
    }
}
