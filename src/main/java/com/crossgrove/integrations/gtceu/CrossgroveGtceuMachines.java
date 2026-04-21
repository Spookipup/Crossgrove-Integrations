package com.crossgrove.integrations.gtceu;

import com.crossgrove.integrations.CrossgroveIntegrations;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.kjs.GTRegistryInfo;
import net.minecraft.network.chat.Component;

public final class CrossgroveGtceuMachines {
    public static final GTRegistrate REGISTRATE = GTRegistrate.create(CrossgroveIntegrations.MOD_ID);
    private static boolean queued;

    static {
        ConfigHolder.init();
    }

    private CrossgroveGtceuMachines() {
    }

    public static void registerRegistrate() {
        CrossgroveIntegrations.LOGGER.info("Registering CrossGrove GTCEu machine registrate");
        REGISTRATE.registerRegistrate();
    }

    public static void queueMachineBuilders() {
        if (queued) {
            return;
        }
        queued = true;
        CrossgroveIntegrations.LOGGER.info("Queueing CrossGrove GTCEu machine definitions");
        GTRegistryInfo.MACHINE.addBuilder(REGISTRATE
                .machine("thermal_exchanger_hatch", ThermalExchangerPartMachine::new)
                .langValue("Thermal Exchanger Hatch")
                .rotationState(RotationState.ALL)
                .abilities(CrossgroveGtceuPartAbilities.THERMAL_EXCHANGER)
                .tier(GTValues.LV)
                .modelProperty(GTMachineModelProperties.IS_FORMED, false)
                .blockModel((context, provider) -> {
                })
                .model((context, provider, model) -> {
                })
                .tooltips(Component.literal("Transfers heat between the structure and connected thermal networks.")));
        GTRegistryInfo.MACHINE.addBuilder(REGISTRATE
                .machine("rotary_input_hatch", ThermalExchangerPartMachine::new)
                .langValue("Rotary Input Hatch")
                .rotationState(RotationState.ALL)
                .abilities(CrossgroveGtceuPartAbilities.ROTARY_INPUT)
                .tier(GTValues.LV)
                .modelProperty(GTMachineModelProperties.IS_FORMED, false)
                .blockModel((context, provider) -> {
                })
                .model((context, provider, model) -> {
                })
                .tooltips(Component.literal("Takes Crossroads rotary power through the front face."),
                        Component.literal("Shares that rotary shaft with other rotary hatches on the formed multiblock.")));
        GTRegistryInfo.MACHINE.addBuilder(REGISTRATE
                .machine("rotary_output_hatch", ThermalExchangerPartMachine::new)
                .langValue("Rotary Output Hatch")
                .rotationState(RotationState.ALL)
                .abilities(CrossgroveGtceuPartAbilities.ROTARY_OUTPUT)
                .tier(GTValues.LV)
                .modelProperty(GTMachineModelProperties.IS_FORMED, false)
                .blockModel((context, provider) -> {
                })
                .model((context, provider, model) -> {
                })
                .tooltips(Component.literal("Outputs the formed multiblock's shared Crossroads rotary shaft through the front face.")));
    }
}
