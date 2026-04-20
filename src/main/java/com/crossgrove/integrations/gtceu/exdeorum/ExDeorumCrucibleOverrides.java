package com.crossgrove.integrations.gtceu.exdeorum;

import java.util.HashMap;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import thedarkcolour.exdeorum.blockentity.AbstractCrucibleBlockEntity;

import com.crossgrove.integrations.CrossgroveIntegrations;
import com.crossgrove.integrations.gtceu.CrossgroveMetalStockTagPrefixes;

public final class ExDeorumCrucibleOverrides {
	private static final Material[] ORE_METALS = {
			GTMaterials.Copper,
			GTMaterials.Tin,
			GTMaterials.Lead,
			GTMaterials.Gold,
			GTMaterials.Iron,
			GTMaterials.Zinc,
			GTMaterials.Nickel
	};

	private ExDeorumCrucibleOverrides() {
	}

	public static void onCommonSetup(FMLCommonSetupEvent event) {
		event.enqueueWork(ExDeorumCrucibleOverrides::install);
	}

	private static void install() {
		HashMap<Item, Block> overrides = AbstractCrucibleBlockEntity.MELT_OVERRIDES.get();
		TagPrefix concentrate = CrossgroveMetalStockTagPrefixes.CONCENTRATE;
		TagPrefix fines = CrossgroveMetalStockTagPrefixes.FINES;
		for (Material metal : ORE_METALS) {
			Block rawBlock = ChemicalHelper.getBlock(TagPrefix.rawOreBlock, metal);
			if (rawBlock == null) {
				continue;
			}
			register(overrides, concentrate, metal, rawBlock);
			register(overrides, fines, metal, rawBlock);
		}
		CrossgroveIntegrations.LOGGER.info("Registered {} Ex Deorum crucible melt overrides for GTCEU ore processing items",
				overrides.size());
	}

	private static void register(HashMap<Item, Block> overrides, TagPrefix prefix, Material metal, Block raw) {
		ItemStack stack = ChemicalHelper.get(prefix, metal);
		if (stack.isEmpty()) {
			return;
		}
		Item item = stack.getItem();
		overrides.putIfAbsent(item, raw);
	}
}
