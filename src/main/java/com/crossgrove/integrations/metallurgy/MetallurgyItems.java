package com.crossgrove.integrations.metallurgy;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.crossgrove.integrations.CrossgroveIntegrations;

public final class MetallurgyItems {
	public static final DeferredRegister<Item> ITEMS =
			DeferredRegister.create(ForgeRegistries.ITEMS, CrossgroveIntegrations.MOD_ID);

	public static final RegistryObject<Item> IRON_BLOOM = ITEMS.register(
			"iron_bloom",
			() -> new Item(new Item.Properties())
	);

	public static final RegistryObject<Item> HOT_IRON_BLOOM = ITEMS.register(
			"hot_iron_bloom",
			() -> new HotIronBloomItem(new Item.Properties())
	);

	private MetallurgyItems() {
	}

	public static void register(IEventBus modBus) {
		ITEMS.register(modBus);
		modBus.addListener(MetallurgyItems::onBuildCreativeTabs);
	}

	private static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
			event.accept(IRON_BLOOM);
			event.accept(HOT_IRON_BLOOM);
		}
	}
}
