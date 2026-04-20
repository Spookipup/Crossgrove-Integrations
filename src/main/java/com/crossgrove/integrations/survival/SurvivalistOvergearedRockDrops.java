package com.crossgrove.integrations.survival;

import java.util.Set;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class SurvivalistOvergearedRockDrops {
	private static final String SURVIVALIST_ESSENTIALS = "survivalistessentials";
	private static final String BLOCK_LOOT_PREFIX = "blocks/";
	private static final ResourceLocation KNAPPABLE_ROCK =
			ResourceLocation.fromNamespaceAndPath("overgeared", "knappable_rock");
	private static final Set<String> LOOSE_ROCK_TABLES = Set.of(
			"andesite_loose_rock",
			"diorite_loose_rock",
			"granite_loose_rock",
			"red_sandstone_loose_rock",
			"sandstone_loose_rock",
			"stone_loose_rock"
	);

	private SurvivalistOvergearedRockDrops() {
	}

	@SubscribeEvent
	public static void onLootTableLoad(LootTableLoadEvent event) {
		ResourceLocation tableId = event.getName();
		if (!SURVIVALIST_ESSENTIALS.equals(tableId.getNamespace())) {
			return;
		}

		String path = tableId.getPath();
		if (!path.startsWith(BLOCK_LOOT_PREFIX)
				|| !LOOSE_ROCK_TABLES.contains(path.substring(BLOCK_LOOT_PREFIX.length()))) {
			return;
		}
		if (!ForgeRegistries.ITEMS.containsKey(KNAPPABLE_ROCK)) {
			return;
		}

		Item rock = ForgeRegistries.ITEMS.getValue(KNAPPABLE_ROCK);
		if (rock == null) {
			return;
		}

		event.setTable(LootTable.lootTable()
				.setParamSet(LootContextParamSets.BLOCK)
				.setRandomSequence(tableId)
				.withPool(LootPool.lootPool()
						.setRolls(ConstantValue.exactly(1.0F))
						.add(LootItem.lootTableItem(rock)))
				.build());
	}
}
