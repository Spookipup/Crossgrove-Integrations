package com.crossgrove.integrations.gtceu;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlag;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconType;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterialItems;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import java.util.function.Predicate;

import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.Conditions.hasIngotProperty;

public final class CrossgroveMetalStockTagPrefixes {

    private static final Predicate<Material> INGOT_MATERIAL = hasIngotProperty;
    private static final MaterialFlag[] NATIVE_PART_FLAGS = {
            MaterialFlags.GENERATE_PLATE,
            MaterialFlags.GENERATE_ROD,
            MaterialFlags.GENERATE_BOLT_SCREW,
            MaterialFlags.GENERATE_FINE_WIRE
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

    public static final TagPrefix SMALL_RAW_BILLET = materialItem("smallRawBillet", "small_raw_%s_billet",
            "Small Raw %s Billet", GTValues.M * 2, MaterialIconType.ingotDouble);
    public static final TagPrefix RAW_BILLET = materialItem("rawBillet", "raw_%s_billet",
            "Raw %s Billet", GTValues.M * 4, MaterialIconType.ingotDouble);
    public static final TagPrefix LARGE_RAW_BILLET = materialItem("largeRawBillet", "large_raw_%s_billet",
            "Large Raw %s Billet", GTValues.M * 8, MaterialIconType.ingotDouble);

    public static final TagPrefix SMALL_BILLET = materialItem("smallBillet", "small_%s_billet",
            "Small %s Billet", GTValues.M * 2, MaterialIconType.plateDense);
    public static final TagPrefix BILLET = materialItem("billet", "%s_billet",
            "%s Billet", GTValues.M * 4, MaterialIconType.plateDense);
    public static final TagPrefix LARGE_BILLET = materialItem("largeBillet", "large_%s_billet",
            "Large %s Billet", GTValues.M * 8, MaterialIconType.plateDense);

    public static final TagPrefix SMALL_PRECISION_BILLET = materialItem("smallPrecisionBillet",
            "small_precision_%s_billet", "Small Precision %s Billet", GTValues.M * 2, MaterialIconType.plateDense);
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

    private static void addNativePartFlags(Material... materials) {
        for (Material material : materials) {
            material.addFlags(NATIVE_PART_FLAGS);
        }
    }

    private static TagPrefix materialItem(String name, String idPattern, String langValue, long materialAmount,
                                          MaterialIconType iconType) {
        return new TagPrefix(name)
                .idPattern(idPattern)
                .itemTable(() -> GTMaterialItems.MATERIAL_ITEMS)
                .langValue(langValue)
                .materialAmount(materialAmount)
                .materialIconType(iconType)
                .unificationEnabled(true)
                .enableRecycling()
                .generateItem(true)
                .generationCondition(INGOT_MATERIAL);
    }
}
