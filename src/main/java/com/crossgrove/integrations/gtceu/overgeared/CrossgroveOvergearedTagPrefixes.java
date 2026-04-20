package com.crossgrove.integrations.gtceu.overgeared;

import java.util.function.Predicate;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconType;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.common.data.GTMaterialItems;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.Conditions.hasToolProperty;

public final class CrossgroveOvergearedTagPrefixes {

	private static final Predicate<Material> LOW_TECH_CASTING_MATERIAL =
			mat -> mat == GTMaterials.Copper || mat == GTMaterials.Tin || mat == GTMaterials.Bronze;
	private static final Predicate<Material> PART_CONDITION =
			hasToolProperty.and(mat -> mat.hasFlag(MaterialFlags.GENERATE_PLATE) || mat.hasProperty(PropertyKey.GEM));

	public static final TagPrefix TOOL_HEAD_PICKAXE = new TagPrefix("pickaxeHead")
			.idPattern("tool_head_pickaxe_%s")
			.itemTable(() -> GTMaterialItems.MATERIAL_ITEMS)
			.langValue("%s Pickaxe Head")
			.materialAmount(GTValues.M * 3)
			.maxStackSize(16)
			.materialIconType(MaterialIconType.toolHeadPickaxe)
			.unificationEnabled(true)
			.enableRecycling()
			.generateItem(true)
			.generationCondition(canGenerateToolPart(GTToolType.PICKAXE));

	public static final TagPrefix TOOL_HEAD_AXE = new TagPrefix("axeHead")
			.idPattern("tool_head_axe_%s")
			.itemTable(() -> GTMaterialItems.MATERIAL_ITEMS)
			.langValue("%s Axe Head")
			.materialAmount(GTValues.M * 3)
			.maxStackSize(16)
			.materialIconType(MaterialIconType.toolHeadAxe)
			.unificationEnabled(true)
			.enableRecycling()
			.generateItem(true)
			.generationCondition(canGenerateToolPart(GTToolType.AXE));

	public static final TagPrefix TOOL_HEAD_SHOVEL = new TagPrefix("shovelHead")
			.idPattern("tool_head_shovel_%s")
			.itemTable(() -> GTMaterialItems.MATERIAL_ITEMS)
			.langValue("%s Shovel Head")
			.materialAmount(GTValues.M)
			.maxStackSize(16)
			.materialIconType(MaterialIconType.toolHeadShovel)
			.unificationEnabled(true)
			.enableRecycling()
			.generateItem(true)
			.generationCondition(canGenerateToolPart(GTToolType.SHOVEL));

	public static final TagPrefix TOOL_HEAD_HOE = new TagPrefix("hoeHead")
			.idPattern("tool_head_hoe_%s")
			.itemTable(() -> GTMaterialItems.MATERIAL_ITEMS)
			.langValue("%s Hoe Head")
			.materialAmount(GTValues.M * 2)
			.maxStackSize(16)
			.materialIconType(MaterialIconType.toolHeadHoe)
			.unificationEnabled(true)
			.enableRecycling()
			.generateItem(true)
			.generationCondition(canGenerateToolPart(GTToolType.HOE));

	public static final TagPrefix TOOL_BLADE_SWORD_PREFIX = new TagPrefix("swordBlade")
			.idPattern("tool_blade_sword_%s")
			.itemTable(() -> GTMaterialItems.MATERIAL_ITEMS)
			.langValue("%s Sword Blade")
			.materialAmount(GTValues.M * 2)
			.maxStackSize(16)
			.materialIconType(MaterialIconType.toolHeadSword)
			.unificationEnabled(true)
			.enableRecycling()
			.generateItem(true)
			.generationCondition(canGenerateToolPart(GTToolType.SWORD));

	public static final TagPrefix TOOL_HEAD_HAMMER = new TagPrefix("hammerHead")
			.idPattern("tool_head_hammer_%s")
			.itemTable(() -> GTMaterialItems.MATERIAL_ITEMS)
			.langValue("%s Hammer Head")
			.materialAmount(GTValues.M * 2)
			.maxStackSize(16)
			.materialIconType(MaterialIconType.toolHeadHammer)
			.unificationEnabled(true)
			.enableRecycling()
			.generateItem(true)
			.generationCondition(canGenerateToolPart(GTToolType.HARD_HAMMER));

	public static final TagPrefix TOOL_HEAD_FILE = new TagPrefix("fileHead")
			.idPattern("tool_head_file_%s")
			.itemTable(() -> GTMaterialItems.MATERIAL_ITEMS)
			.langValue("%s File Head")
			.materialAmount(GTValues.M * 2)
			.maxStackSize(16)
			.materialIconType(MaterialIconType.toolHeadFile)
			.unificationEnabled(true)
			.enableRecycling()
			.generateItem(true)
			.generationCondition(canGenerateToolPart(GTToolType.FILE));

	public static final TagPrefix TOOL_BLADE_SAW = new TagPrefix("sawBlade")
			.idPattern("tool_blade_saw_%s")
			.itemTable(() -> GTMaterialItems.MATERIAL_ITEMS)
			.langValue("%s Saw Blade")
			.materialAmount(GTValues.M * 2)
			.maxStackSize(16)
			.materialIconType(MaterialIconType.toolHeadSaw)
			.unificationEnabled(true)
			.enableRecycling()
			.generateItem(true)
			.generationCondition(canGenerateToolPart(GTToolType.SAW));

	public static final TagPrefix TOOL_BLADE_SCYTHE = new TagPrefix("scytheBlade")
			.idPattern("tool_blade_scythe_%s")
			.itemTable(() -> GTMaterialItems.MATERIAL_ITEMS)
			.langValue("%s Scythe Blade")
			.materialAmount(GTValues.M * 2)
			.maxStackSize(16)
			.materialIconType(MaterialIconType.toolHeadScythe)
			.unificationEnabled(true)
			.enableRecycling()
			.generateItem(true)
			.generationCondition(canGenerateToolPart(GTToolType.SCYTHE));

	private CrossgroveOvergearedTagPrefixes() {
	}

	public static void init() {
	}

	private static Predicate<Material> canGenerateToolPart(GTToolType toolType) {
		return mat -> LOW_TECH_CASTING_MATERIAL.test(mat)
				|| (PART_CONDITION.test(mat) && mat.getProperty(PropertyKey.TOOL).hasType(toolType));
	}
}
