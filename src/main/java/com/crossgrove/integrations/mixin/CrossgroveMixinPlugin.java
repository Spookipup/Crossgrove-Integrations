package com.crossgrove.integrations.mixin;

import java.util.List;
import java.util.Set;

import net.minecraftforge.fml.loading.FMLLoader;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class CrossgroveMixinPlugin implements IMixinConfigPlugin {
	private static final String MIXIN_PACKAGE = CrossgroveMixinPlugin.class.getPackageName();
	private static final String JADE_MIXIN = MIXIN_PACKAGE + ".GtceuControllableBlockProviderMixin";
	private static final String GTCEU_ELECTRIC_JADE_MIXIN = MIXIN_PACKAGE + ".GtceuElectricContainerBlockProviderMixin";
	private static final String GTCEU_ENERGY_MIXIN = MIXIN_PACKAGE + ".GtceuEnergyContainerMixin";
	private static final String GTCEU_PREDICATE_BLOCKS_MIXIN = MIXIN_PACKAGE + ".GtceuPredicateBlocksMixin";
	private static final String GTCEU_PREDICATES_MIXIN = MIXIN_PACKAGE + ".GtceuPredicatesMixin";
	private static final String EXDEORUM_CRUCIBLE_MIXIN = MIXIN_PACKAGE + ".ExDeorumCrucibleMixin";
	private static final String EXDEORUM_LAVA_CRUCIBLE_HEAT_MIXIN = MIXIN_PACKAGE + ".ExDeorumLavaCrucibleHeatMixin";
	private static final String EXDEORUM_BARREL_FLUID_MIXING_CATEGORY_MIXIN =
			MIXIN_PACKAGE + ".ExDeorumBarrelFluidMixingCategoryMixin";
	private static final String EXDEORUM_SIEVE_ITEM_CONTENTS_MIXIN =
			MIXIN_PACKAGE + ".ExDeorumSieveItemContentsMixin";
	private static final String LSO_MIXIN = MIXIN_PACKAGE + ".LsoBlockModifierMixin";
	private static final String OVERGEARED_ANVIL_HAMMER_SLOT_MIXIN =
			MIXIN_PACKAGE + ".OvergearedAnvilHammerSlotMixin";
	private static final String OVERGEARED_ANVIL_USE_HAMMER_MIXIN =
			MIXIN_PACKAGE + ".OvergearedAnvilUseHammerMixin";
	private static final String OVERGEARED_ANVIL_SLAG_MIXIN = MIXIN_PACKAGE + ".OvergearedAnvilSlagMixin";
	private static final String OVERGEARED_RECIPE_MIXIN = MIXIN_PACKAGE + ".RecipeManagerMixin";
	private static final String OVERGEARED_KNAPPING_INTERACTION_MIXIN =
			MIXIN_PACKAGE + ".OvergearedKnappingInteractionMixin";
	private static final String OVERGEARED_HOT_ITEM_DAMAGE_MIXIN =
			MIXIN_PACKAGE + ".OvergearedHotItemDamageMixin";
	private static final String OVERGEARED_ROCK_KNAPPING_MENU_ACCESSOR =
			MIXIN_PACKAGE + ".OvergearedRockKnappingMenuAccessor";
	private static final String OVERGEARED_ROCK_KNAPPING_SCREEN_MIXIN =
			MIXIN_PACKAGE + ".OvergearedRockKnappingScreenMixin";

	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (JADE_MIXIN.equals(mixinClassName)) {
			return isLoaded("jade") && isLoaded("gtceu");
		}
		if (GTCEU_ELECTRIC_JADE_MIXIN.equals(mixinClassName)) {
			return isLoaded("jade") && isLoaded("gtceu");
		}
		if (GTCEU_ENERGY_MIXIN.equals(mixinClassName)) {
			return isLoaded("gtceu");
		}
		if (GTCEU_PREDICATE_BLOCKS_MIXIN.equals(mixinClassName)) {
			return isLoaded("gtceu");
		}
		if (GTCEU_PREDICATES_MIXIN.equals(mixinClassName)) {
			return isLoaded("gtceu");
		}
		if (EXDEORUM_CRUCIBLE_MIXIN.equals(mixinClassName)) {
			return isLoaded("exdeorum");
		}
		if (EXDEORUM_LAVA_CRUCIBLE_HEAT_MIXIN.equals(mixinClassName)) {
			return isLoaded("exdeorum") && isLoaded("crossroads");
		}
		if (EXDEORUM_BARREL_FLUID_MIXING_CATEGORY_MIXIN.equals(mixinClassName)) {
			return isLoaded("exdeorum") && isLoaded("jei");
		}
		if (EXDEORUM_SIEVE_ITEM_CONTENTS_MIXIN.equals(mixinClassName)) {
			return isLoaded("exdeorum");
		}
		if (LSO_MIXIN.equals(mixinClassName)) {
			return isLoaded("legendarysurvivaloverhaul");
		}
		if (OVERGEARED_ANVIL_SLAG_MIXIN.equals(mixinClassName)) {
			return isLoaded("overgeared");
		}
		if (OVERGEARED_ANVIL_HAMMER_SLOT_MIXIN.equals(mixinClassName)
				|| OVERGEARED_ANVIL_USE_HAMMER_MIXIN.equals(mixinClassName)) {
			return isLoaded("gtceu") && isLoaded("overgeared");
		}
		if (OVERGEARED_RECIPE_MIXIN.equals(mixinClassName)) {
			return isLoaded("gtceu") && isLoaded("overgeared");
		}
		if (OVERGEARED_KNAPPING_INTERACTION_MIXIN.equals(mixinClassName)) {
			return isLoaded("overgeared");
		}
		if (OVERGEARED_HOT_ITEM_DAMAGE_MIXIN.equals(mixinClassName)) {
			return isLoaded("overgeared");
		}
		if (OVERGEARED_ROCK_KNAPPING_MENU_ACCESSOR.equals(mixinClassName)) {
			return isLoaded("overgeared");
		}
		if (OVERGEARED_ROCK_KNAPPING_SCREEN_MIXIN.equals(mixinClassName)) {
			return isLoaded("overgeared");
		}
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	private static boolean isLoaded(String modId) {
		return FMLLoader.getLoadingModList().getModFileById(modId) != null;
	}
}
