package com.crossgrove.integrations.mechanical;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.crossgrove.integrations.CrossgroveIntegrations;

public final class MechanicalItems {
	public static final DeferredRegister<Item> ITEMS =
			DeferredRegister.create(ForgeRegistries.ITEMS, CrossgroveIntegrations.MOD_ID);

	public static final RegistryObject<Item> MECHANICAL_PISTON = ITEMS.register(
			"mechanical_piston",
			() -> new Item(new Item.Properties())
	);

	private MechanicalItems() {
	}

	public static void register(IEventBus modBus) {
		ITEMS.register(modBus);
		modBus.addListener(MechanicalItems::onBuildCreativeTabs);
	}

	private static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
			event.accept(MECHANICAL_PISTON);
		}
	}
}
