package com.crossgrove.integrations.gtceu;

import com.crossgrove.integrations.CrossgroveIntegrations;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;

public final class CrossgroveGtceuPartAbilities {
    public static final PartAbility THERMAL_EXCHANGER = new PartAbility(
            CrossgroveIntegrations.MOD_ID + ":thermal_exchanger"
    );

    private CrossgroveGtceuPartAbilities() {
    }
}
