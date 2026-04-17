package com.crossgrove.integrations.gtceu.overgeared;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.item.tool.ToolHelper;

import net.minecraft.world.item.ItemStack;

public final class CrossgroveOvergearedKubeJs {

    private CrossgroveOvergearedKubeJs() {
    }

    public static ItemStack materialItem(TagPrefix prefix, Material material) {
        if (prefix == null || material == null) {
            return ItemStack.EMPTY;
        }
        return ChemicalHelper.get(prefix, material);
    }

    public static ItemStack toolItem(GTToolType toolType, Material material) {
        if (toolType == null || material == null) {
            return ItemStack.EMPTY;
        }
        return ToolHelper.get(toolType, material);
    }
}
