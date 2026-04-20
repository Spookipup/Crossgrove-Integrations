package com.crossgrove.integrations.gtceu;

import net.minecraft.resources.ResourceLocation;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;

import com.crossgrove.integrations.CrossgroveIntegrations;

public final class CrossgroveMaterials {
	public static Material IronSlag;

	private CrossgroveMaterials() {
	}

	public static void register() {
		IronSlag = new Material.Builder(ResourceLocation.fromNamespaceAndPath(CrossgroveIntegrations.MOD_ID, "iron_slag"))
				.dust()
				.color(0x5A5146)
				.secondaryColor(0x934F2B)
				.iconSet(MaterialIconSet.SAND)
				.buildAndRegister();
	}
}
