package com.crossgrove.integrations.gtceu.overgeared;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.item.tool.ToolHelper;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import net.stirdrem.overgeared.recipe.ForgingRecipe;
import net.stirdrem.overgeared.recipe.OvergearedShapelessRecipe;

import com.crossgrove.integrations.CrossgroveIntegrations;
import com.crossgrove.integrations.casting.CastingMoldItems;

public final class CrossgroveOvergearedRecipes {

	public record RecipeMaps(Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipes,
							 Map<ResourceLocation, Recipe<?>> byName) {
	}

	private record ToolEntry(GTToolType toolType, TagPrefix prefix, String blueprint, int hammering,
							 boolean hasPolishing, List<String> pattern) {
	}

	private record ToolSpecificEntry(GTToolType finishingTool, ToolEntry baseEntry, ToolEntry resultEntry) {
	}

	private record BlankEntry(RegistryObject<Item> output, String id, int hammering,
							  List<String> pattern) {
	}

	private static volatile Map<ResourceLocation, Map<GTToolType, ItemStack>> toolSpecificResults = Map.of();

	private static final ToolEntry PICKAXE_ENTRY = new ToolEntry(GTToolType.PICKAXE,
			CrossgroveOvergearedTagPrefixes.TOOL_HEAD_PICKAXE, "pickaxe", 3, true, List.of("###"));
	private static final ToolEntry AXE_ENTRY = new ToolEntry(GTToolType.AXE,
			CrossgroveOvergearedTagPrefixes.TOOL_HEAD_AXE, "axe", 3, true, List.of("##", "# "));
	private static final ToolEntry SHOVEL_ENTRY = new ToolEntry(GTToolType.SHOVEL,
			CrossgroveOvergearedTagPrefixes.TOOL_HEAD_SHOVEL, "shovel", 3, true, List.of("#"));
	private static final ToolEntry HOE_ENTRY = new ToolEntry(GTToolType.HOE,
			CrossgroveOvergearedTagPrefixes.TOOL_HEAD_HOE, "hoe", 3, true, List.of("##"));
	private static final ToolEntry SWORD_ENTRY = new ToolEntry(GTToolType.SWORD,
			CrossgroveOvergearedTagPrefixes.TOOL_BLADE_SWORD_PREFIX, "sword", 3, true, List.of("#", "#"));
	private static final ToolEntry HAMMER_ENTRY = new ToolEntry(GTToolType.HARD_HAMMER,
			CrossgroveOvergearedTagPrefixes.TOOL_HEAD_HAMMER, null, 3, false, List.of("# ", " #"));
	private static final ToolEntry FILE_ENTRY = new ToolEntry(GTToolType.FILE,
			CrossgroveOvergearedTagPrefixes.TOOL_HEAD_FILE, null, 3, true, List.of("#", "#"));
	private static final ToolEntry SAW_ENTRY = new ToolEntry(GTToolType.SAW,
			CrossgroveOvergearedTagPrefixes.TOOL_BLADE_SAW, null, 3, true, List.of("##"));
	private static final ToolEntry SCYTHE_ENTRY = new ToolEntry(GTToolType.SCYTHE,
			CrossgroveOvergearedTagPrefixes.TOOL_BLADE_SCYTHE, null, 3, true, List.of("# ", " #"));

	private static final List<ToolEntry> TOOL_ENTRIES = List.of(
			PICKAXE_ENTRY,
			AXE_ENTRY,
			SHOVEL_ENTRY,
			HOE_ENTRY,
			SWORD_ENTRY,
			HAMMER_ENTRY
	);

	private static final List<ToolSpecificEntry> TOOL_SPECIFIC_ENTRIES = List.of(
			new ToolSpecificEntry(GTToolType.FILE, SWORD_ENTRY, FILE_ENTRY),
			new ToolSpecificEntry(GTToolType.SAW, HOE_ENTRY, SAW_ENTRY),
			new ToolSpecificEntry(GTToolType.SCYTHE, HAMMER_ENTRY, SCYTHE_ENTRY)
	);

	// Bronze-only mechanical parts previously produced via casting molds.
	// Now forged directly from bronze stock in the Overgeared forge.
	private static final List<BlankEntry> BLANK_ENTRIES = List.of(
			new BlankEntry(CastingMoldItems.ROUGH_BRONZE_GEAR_BLANK, "bronze_gear_blank", 3,
					List.of("# #", " # ", "# #")),
			new BlankEntry(CastingMoldItems.ROUGH_BRONZE_AXLE_BLANK, "bronze_axle_blank", 3,
					List.of(" # ", " # ", " # ")),
			new BlankEntry(CastingMoldItems.ROUGH_BRONZE_BUSHING_BLANK, "bronze_bushing_blank", 3,
					List.of(" # ", "# #", " # "))
	);

	private CrossgroveOvergearedRecipes() {
	}

	public static RecipeMaps inject(Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipes,
									Map<ResourceLocation, Recipe<?>> byName) {
		Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> mutableRecipes = new HashMap<>();
		for (Map.Entry<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> entry : recipes.entrySet()) {
			mutableRecipes.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
		}
		Map<ResourceLocation, Recipe<?>> mutableByName = new LinkedHashMap<>(byName);

		int added = 0;
		Map<ResourceLocation, Map<GTToolType, ItemStack>> builtToolSpecificResults = new HashMap<>();
		for (Material material : GTCEuAPI.materialManager.getRegisteredMaterials()) {
			if (!material.hasProperty(PropertyKey.TOOL)) {
				continue;
			}
			if (!material.hasFlag(MaterialFlags.GENERATE_PLATE) && !material.hasProperty(PropertyKey.GEM)) {
				continue;
			}

			for (ToolEntry entry : TOOL_ENTRIES) {
				if (!material.getProperty(PropertyKey.TOOL).hasType(entry.toolType())) {
					continue;
				}

				ItemStack partStack = ChemicalHelper.get(entry.prefix(), material);
				if (partStack.isEmpty()) {
					continue;
				}

				ItemStack inputStack = resolveInputStack(material);
				if (inputStack.isEmpty()) {
					continue;
				}

				ResourceLocation forgingId = recipeId("forge_" + entry.prefix().name + "_" + material.getName());
				if (mutableByName.containsKey(forgingId)) {
					continue;
				}

				try {
					ForgingRecipe forgingRecipe = ForgingRecipe.Serializer.INSTANCE.fromJson(
							forgingId, buildForgingJson(entry, inputStack, partStack));
					mutableRecipes
							.computeIfAbsent(forgingRecipe.getType(), t -> new LinkedHashMap<>())
							.put(forgingId, forgingRecipe);
					mutableByName.put(forgingId, forgingRecipe);
					added++;
				} catch (Exception e) {
					CrossgroveIntegrations.LOGGER.warn("Failed to build Overgeared forging recipe for {} {}",
							entry.prefix().name, material.getName(), e);
					continue;
				}

				added += injectToolAssemblyRecipe(mutableRecipes, mutableByName, material, entry, partStack);
			}

			for (ToolSpecificEntry entry : TOOL_SPECIFIC_ENTRIES) {
				if (!material.getProperty(PropertyKey.TOOL).hasType(entry.baseEntry().toolType())
						|| !material.getProperty(PropertyKey.TOOL).hasType(entry.resultEntry().toolType())) {
					continue;
				}

				ItemStack resultStack = ChemicalHelper.get(entry.resultEntry().prefix(), material);
				if (resultStack.isEmpty()) {
					continue;
				}

				ResourceLocation baseRecipeId = recipeId(
						"forge_" + entry.baseEntry().prefix().name + "_" + material.getName());
				builtToolSpecificResults
						.computeIfAbsent(baseRecipeId, id -> new HashMap<>())
						.put(entry.finishingTool(), resultStack.copy());

				added += injectToolAssemblyRecipe(
						mutableRecipes, mutableByName, material, entry.resultEntry(), resultStack);
			}
		}

		toolSpecificResults = freezeToolSpecificResults(builtToolSpecificResults);
		added += injectBlankRecipes(mutableRecipes, mutableByName);

		CrossgroveIntegrations.LOGGER.info("Injected {} GTCEu/Overgeared recipes", added);
		return new RecipeMaps(mutableRecipes, mutableByName);
	}

	private static int injectToolAssemblyRecipe(Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> mutableRecipes,
												Map<ResourceLocation, Recipe<?>> mutableByName,
												Material material, ToolEntry entry, ItemStack partStack) {
		ItemStack toolStack = ToolHelper.get(entry.toolType(), material);
		if (toolStack.isEmpty()) {
			return 0;
		}

		ResourceLocation shapelessId = recipeId(
				entry.toolType().name + "_" + material.getName() + "_from_overgeared_part");
		if (mutableByName.containsKey(shapelessId)) {
			return 0;
		}

		try {
			OvergearedShapelessRecipe shapeless = OvergearedShapelessRecipe.Serializer.INSTANCE.fromJson(
					shapelessId, buildShapelessJson(partStack, toolStack));
			mutableRecipes
					.computeIfAbsent(shapeless.getType(), t -> new LinkedHashMap<>())
					.put(shapelessId, shapeless);
			mutableByName.put(shapelessId, shapeless);
			return 1;
		} catch (Exception e) {
			CrossgroveIntegrations.LOGGER.warn("Failed to build Overgeared shapeless recipe for {} {}",
					entry.toolType().name, material.getName(), e);
			return 0;
		}
	}

	private static Map<ResourceLocation, Map<GTToolType, ItemStack>> freezeToolSpecificResults(
			Map<ResourceLocation, Map<GTToolType, ItemStack>> builtResults) {
		Map<ResourceLocation, Map<GTToolType, ItemStack>> frozen = new HashMap<>();
		for (Map.Entry<ResourceLocation, Map<GTToolType, ItemStack>> entry : builtResults.entrySet()) {
			Map<GTToolType, ItemStack> byTool = new HashMap<>();
			for (Map.Entry<GTToolType, ItemStack> toolEntry : entry.getValue().entrySet()) {
				byTool.put(toolEntry.getKey(), toolEntry.getValue().copy());
			}
			frozen.put(entry.getKey(), Map.copyOf(byTool));
		}
		return Map.copyOf(frozen);
	}

	public static ItemStack toolSpecificResult(ResourceLocation recipeId, GTToolType toolType) {
		if (recipeId == null || toolType == null) {
			return ItemStack.EMPTY;
		}
		Map<GTToolType, ItemStack> byTool = toolSpecificResults.get(recipeId);
		if (byTool == null) {
			return ItemStack.EMPTY;
		}
		ItemStack result = byTool.get(toolType);
		return result == null ? ItemStack.EMPTY : result.copy();
	}

	private static int injectBlankRecipes(Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> mutableRecipes,
										  Map<ResourceLocation, Recipe<?>> mutableByName) {
		ItemStack bronze = resolveInputStack(GTMaterials.Bronze);
		if (bronze.isEmpty()) {
			return 0;
		}
		int added = 0;
		for (BlankEntry blank : BLANK_ENTRIES) {
			ItemStack result = new ItemStack(blank.output().get());
			if (result.isEmpty()) {
				continue;
			}
			ResourceLocation id = recipeId("forge_" + blank.id());
			if (mutableByName.containsKey(id)) {
				continue;
			}
			try {
				ForgingRecipe recipe = ForgingRecipe.Serializer.INSTANCE.fromJson(
						id, buildBlankForgingJson(blank, bronze, result));
				mutableRecipes
						.computeIfAbsent(recipe.getType(), t -> new LinkedHashMap<>())
						.put(id, recipe);
				mutableByName.put(id, recipe);
				added++;
			} catch (Exception e) {
				CrossgroveIntegrations.LOGGER.warn("Failed to build Overgeared forging recipe for {}",
						blank.id(), e);
			}
		}
		return added;
	}

	private static ItemStack resolveInputStack(Material material) {
		ItemStack stack = ChemicalHelper.get(TagPrefix.ingotHot, material);
		if (!stack.isEmpty()) {
			return stack;
		}
		if (material.hasProperty(PropertyKey.GEM)) {
			stack = ChemicalHelper.get(TagPrefix.gem, material);
			if (!stack.isEmpty()) {
				return stack;
			}
		}
		return ChemicalHelper.get(TagPrefix.ingot, material);
	}

	private static JsonObject buildForgingJson(ToolEntry entry, ItemStack input, ItemStack result) {
		JsonObject json = new JsonObject();
		json.addProperty("type", "overgeared:forging");
		json.addProperty("category", "tool_heads");
		json.addProperty("group", "");
		json.addProperty("hammering", entry.hammering());
		json.addProperty("has_polishing", entry.hasPolishing());
		json.addProperty("has_quality", true);
		json.addProperty("minimum_quality", "poor");
		json.addProperty("need_quenching", true);
		json.addProperty("needs_minigame", false);
		json.addProperty("quality_difficulty", "none");
		json.addProperty("requires_blueprint", false);
		json.addProperty("show_notification", true);
		json.addProperty("tier", "stone");

		JsonArray pattern = new JsonArray();
		for (String row : entry.pattern()) {
			pattern.add(row);
		}
		json.add("pattern", pattern);

		JsonObject key = new JsonObject();
		key.add("#", itemObject(input));
		json.add("key", key);

		json.add("result", itemObject(result));

		if (entry.blueprint() != null) {
			JsonArray blueprint = new JsonArray();
			blueprint.add(entry.blueprint());
			json.add("blueprint", blueprint);
		}
		return json;
	}

	private static JsonObject buildBlankForgingJson(BlankEntry entry, ItemStack input, ItemStack result) {
		JsonObject json = new JsonObject();
		json.addProperty("type", "overgeared:forging");
		json.addProperty("category", "misc");
		json.addProperty("group", "");
		json.addProperty("hammering", entry.hammering());
		json.addProperty("has_polishing", false);
		json.addProperty("has_quality", false);
		json.addProperty("minimum_quality", "poor");
		json.addProperty("need_quenching", true);
		json.addProperty("needs_minigame", false);
		json.addProperty("quality_difficulty", "none");
		json.addProperty("requires_blueprint", false);
		json.addProperty("show_notification", true);
		json.addProperty("tier", "stone");

		JsonArray pattern = new JsonArray();
		for (String row : entry.pattern()) {
			pattern.add(row);
		}
		json.add("pattern", pattern);

		JsonObject key = new JsonObject();
		key.add("#", itemObject(input));
		json.add("key", key);

		json.add("result", itemObject(result));
		return json;
	}

	private static JsonObject buildShapelessJson(ItemStack part, ItemStack tool) {
		JsonObject json = new JsonObject();
		json.addProperty("type", "overgeared:crafting_shapeless");
		json.addProperty("category", "equipment");
		json.addProperty("group", "");

		JsonArray ingredients = new JsonArray();
		ingredients.add(itemObject(part));
		JsonObject rod = new JsonObject();
		rod.addProperty("tag", "forge:rods/wooden");
		ingredients.add(rod);
		json.add("ingredients", ingredients);

		json.add("result", itemObject(tool));
		return json;
	}

	private static JsonObject itemObject(ItemStack stack) {
		ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
		JsonObject obj = new JsonObject();
		obj.addProperty("item", id == null ? "minecraft:air" : id.toString());
		return obj;
	}

	private static ResourceLocation recipeId(String path) {
		return ResourceLocation.fromNamespaceAndPath(CrossgroveIntegrations.MOD_ID, "overgeared/gtceu/" + path.toLowerCase());
	}
}
