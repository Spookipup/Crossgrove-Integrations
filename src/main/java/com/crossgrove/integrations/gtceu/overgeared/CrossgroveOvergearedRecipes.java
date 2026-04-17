package com.crossgrove.integrations.gtceu.overgeared;

import com.crossgrove.integrations.CrossgroveIntegrations;
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

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.ForgeRegistries;
import net.stirdrem.overgeared.recipe.ForgingRecipe;
import net.stirdrem.overgeared.recipe.OvergearedShapelessRecipe;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CrossgroveOvergearedRecipes {

    public record RecipeMaps(Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipes,
                             Map<ResourceLocation, Recipe<?>> byName) {
    }

    private record ToolEntry(GTToolType toolType, TagPrefix prefix, String blueprint, int hammering,
                             boolean hasPolishing, List<String> pattern) {
    }

    private static final List<ToolEntry> TOOL_ENTRIES = List.of(
            new ToolEntry(GTToolType.PICKAXE, CrossgroveOvergearedTagPrefixes.TOOL_HEAD_PICKAXE,
                    "pickaxe", 3, true, List.of("###")),
            new ToolEntry(GTToolType.AXE, CrossgroveOvergearedTagPrefixes.TOOL_HEAD_AXE,
                    "axe", 3, true, List.of("##", "# ")),
            new ToolEntry(GTToolType.SHOVEL, CrossgroveOvergearedTagPrefixes.TOOL_HEAD_SHOVEL,
                    "shovel", 3, true, List.of("#")),
            new ToolEntry(GTToolType.HOE, CrossgroveOvergearedTagPrefixes.TOOL_HEAD_HOE,
                    "hoe", 3, true, List.of("##")),
            new ToolEntry(GTToolType.SWORD, CrossgroveOvergearedTagPrefixes.TOOL_BLADE_SWORD_PREFIX,
                    "sword", 3, true, List.of("#", "#")),
            new ToolEntry(GTToolType.HARD_HAMMER, CrossgroveOvergearedTagPrefixes.TOOL_HEAD_HAMMER,
                    null, 3, false, List.of("# ", " #"))
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

                ItemStack toolStack = ToolHelper.get(entry.toolType(), material);
                if (toolStack.isEmpty()) {
                    continue;
                }

                ResourceLocation shapelessId = recipeId(
                        entry.toolType().name + "_" + material.getName() + "_from_overgeared_part");
                if (mutableByName.containsKey(shapelessId)) {
                    continue;
                }

                try {
                    OvergearedShapelessRecipe shapeless = OvergearedShapelessRecipe.Serializer.INSTANCE.fromJson(
                            shapelessId, buildShapelessJson(partStack, toolStack));
                    mutableRecipes
                            .computeIfAbsent(shapeless.getType(), t -> new LinkedHashMap<>())
                            .put(shapelessId, shapeless);
                    mutableByName.put(shapelessId, shapeless);
                    added++;
                } catch (Exception e) {
                    CrossgroveIntegrations.LOGGER.warn("Failed to build Overgeared shapeless recipe for {} {}",
                            entry.toolType().name, material.getName(), e);
                }
            }
        }

        CrossgroveIntegrations.LOGGER.info("Injected {} GTCEu/Overgeared recipes", added);
        return new RecipeMaps(mutableRecipes, mutableByName);
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
        return new ResourceLocation(CrossgroveIntegrations.MOD_ID, "overgeared/gtceu/" + path.toLowerCase());
    }
}
