package com.crossgrove.integrations.gtceu;

import java.util.function.Predicate;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlag;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconType;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.ToolProperty;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.common.data.GTMaterialItems;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.Conditions.hasIngotProperty;

public final class CrossgroveMetalStockTagPrefixes {

	private static final Predicate<Material> INGOT_MATERIAL = hasIngotProperty;
	private static final Predicate<Material> ORE_PROCESSING_MATERIAL = material ->
			material == GTMaterials.Copper
					|| material == GTMaterials.Tin
					|| material == GTMaterials.Iron
					|| material == GTMaterials.Zinc
					|| material == GTMaterials.Lead
					|| material == GTMaterials.Nickel
					|| material == GTMaterials.Gold;
	private static final MaterialFlag[] NATIVE_PART_FLAGS = {
			MaterialFlags.GENERATE_PLATE,
			MaterialFlags.GENERATE_ROD,
			MaterialFlags.GENERATE_BOLT_SCREW,
			MaterialFlags.GENERATE_FINE_WIRE
	};
	private static final GTToolType[] LOW_TECH_TOOL_TYPES = {
			GTToolType.PICKAXE,
			GTToolType.AXE,
			GTToolType.SHOVEL,
			GTToolType.HOE,
			GTToolType.SWORD,
			GTToolType.HARD_HAMMER,
			GTToolType.FILE,
			GTToolType.SAW,
			GTToolType.SCYTHE
	};

	public static final TagPrefix METAL_SPONGE = materialItem("metalSponge", "%s_metal_sponge",
			"%s Metal Sponge", GTValues.M, MaterialIconType.crushed);

	public static final TagPrefix LIGHT_PLATE = materialItem("lightPlate", "light_%s_plate",
			"Light %s Plate", GTValues.M / 2, MaterialIconType.foil);

	public static final TagPrefix LIGHT_ROD = materialItem("lightRod", "light_%s_rod",
			"Light %s Rod", GTValues.M / 4, MaterialIconType.rod);
	public static final TagPrefix THREADED_ROD = materialItem("threadedRod", "threaded_%s_rod",
			"Threaded %s Rod", GTValues.M / 4, MaterialIconType.rod);

	public static final TagPrefix HEAVY_WIRE = materialItem("heavyWire", "heavy_%s_wire",
			"Heavy %s Wire", GTValues.M / 8, MaterialIconType.wireFine);
	public static final TagPrefix SIEVE_MESH = materialItem("sieveMesh", "%s_mesh",
			"%s Mesh", GTValues.M / 2, MaterialIconType.wireFine);
	public static final TagPrefix CONCENTRATE = materialItem("concentrate", "%s_concentrate",
			"%s Concentrate", GTValues.M, MaterialIconType.crushedPurified, ORE_PROCESSING_MATERIAL);
	public static final TagPrefix FINES = materialItem("fines", "%s_fines",
			"%s Fines", GTValues.M / 4, MaterialIconType.dustSmall, ORE_PROCESSING_MATERIAL);
	public static final TagPrefix TAILINGS = materialItem("tailings", "%s_tailings",
			"%s Tailings", GTValues.M / 9, MaterialIconType.dustImpure, ORE_PROCESSING_MATERIAL);

	public static final TagPrefix SMALL_RAW_BILLET = materialItem("smallRawBillet", "small_raw_%s_billet",
			"Small Raw %s Billet", GTValues.M * 2, MaterialIconType.ingotDouble);
	public static final TagPrefix RAW_BILLET = materialItem("rawBillet", "raw_%s_billet",
			"Raw %s Billet", GTValues.M * 4, MaterialIconType.ingotDouble);
	public static final TagPrefix LARGE_RAW_BILLET = materialItem("largeRawBillet", "large_raw_%s_billet",
			"Large Raw %s Billet", GTValues.M * 8, MaterialIconType.ingotDouble);

	public static final TagPrefix SMALL_BILLET = materialItem("smallBillet", "small_%s_billet",
			"Small %s Billet", GTValues.M * 2, MaterialIconType.plateDouble);
	public static final TagPrefix BILLET = materialItem("billet", "%s_billet",
			"%s Billet", GTValues.M * 4, MaterialIconType.plateDense);
	public static final TagPrefix LARGE_BILLET = materialItem("largeBillet", "large_%s_billet",
			"Large %s Billet", GTValues.M * 8, MaterialIconType.plateDense);

	public static final TagPrefix SMALL_PRECISION_BILLET = materialItem("smallPrecisionBillet",
			"small_precision_%s_billet", "Small Precision %s Billet", GTValues.M * 2, MaterialIconType.plateDouble);
	public static final TagPrefix PRECISION_BILLET = materialItem("precisionBillet", "precision_%s_billet",
			"Precision %s Billet", GTValues.M * 4, MaterialIconType.plateDense);
	public static final TagPrefix LARGE_PRECISION_BILLET = materialItem("largePrecisionBillet",
			"large_precision_%s_billet", "Large Precision %s Billet", GTValues.M * 8, MaterialIconType.plateDense);

	private CrossgroveMetalStockTagPrefixes() {
	}

	public static void init() {
		TagPrefix.ingotHot.generationCondition(INGOT_MATERIAL);
		TagPrefix.wireFine.materialAmount(GTValues.M / 16);
	}

	public static void registerNativeMaterialFlags() {
		registerLowTechToolProperties();
		addNativePartFlags(
				GTMaterials.Copper,
				GTMaterials.Tin,
				GTMaterials.Iron,
				GTMaterials.Zinc,
				GTMaterials.Lead,
				GTMaterials.Nickel,
				GTMaterials.Gold,
				GTMaterials.Bronze,
				GTMaterials.Brass,
				GTMaterials.Steel,
				GTMaterials.Cupronickel
		);
	}

	private static void registerLowTechToolProperties() {
		registerLowTechToolProperty(GTMaterials.Copper, 2.0F, 0.0F, 48, 1, 1);
		registerLowTechToolProperty(GTMaterials.Tin, 1.5F, 0.0F, 24, 1, 1);
		registerLowTechToolProperty(GTMaterials.Lead, 1.25F, 0.0F, 20, 1, 1);
		registerLowTechToolProperty(GTMaterials.Gold, 6.0F, 0.0F, 32, 1, 8);
		registerLowTechToolProperty(GTMaterials.Bronze, 4.0F, 1.0F, 131, 2, 5);
	}

	private static void registerLowTechToolProperty(Material material, float harvestSpeed, float attackDamage,
													int durability, int harvestLevel, int enchantability) {
		if (material.hasProperty(PropertyKey.TOOL)) {
			ToolProperty property = material.getProperty(PropertyKey.TOOL);
			property.addTypes(LOW_TECH_TOOL_TYPES);
			property.setHarvestSpeed(harvestSpeed);
			property.setAttackDamage(attackDamage);
			property.setDurability(durability);
			property.setHarvestLevel(harvestLevel);
			property.setEnchantability(enchantability);
			property.setDurabilityMultiplier(1);
			return;
		}

		material.setProperty(
				PropertyKey.TOOL,
				ToolProperty.Builder.of(harvestSpeed, attackDamage, durability, harvestLevel, LOW_TECH_TOOL_TYPES)
						.enchantability(enchantability)
						.build()
		);
	}

	private static void addNativePartFlags(Material... materials) {
		for (Material material : materials) {
			material.addFlags(NATIVE_PART_FLAGS);
		}
	}

	private static TagPrefix materialItem(String name, String idPattern, String langValue, long materialAmount,
										  MaterialIconType iconType) {
		return materialItem(name, idPattern, langValue, materialAmount, iconType, INGOT_MATERIAL);
	}

	private static TagPrefix materialItem(String name, String idPattern, String langValue, long materialAmount,
										  MaterialIconType iconType, Predicate<Material> generationCondition) {
		return new TagPrefix(name)
				.idPattern(idPattern)
				.itemTable(() -> GTMaterialItems.MATERIAL_ITEMS)
				.langValue(langValue)
				.materialAmount(materialAmount)
				.materialIconType(iconType)
				.unificationEnabled(true)
				.enableRecycling()
				.generateItem(true)
				.generationCondition(generationCondition);
	}
}
