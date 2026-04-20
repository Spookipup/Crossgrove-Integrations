package com.crossgrove.integrations.ceramic;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import org.jetbrains.annotations.Nullable;

import com.crossgrove.integrations.CrossgroveIntegrations;

public final class CeramicItems {
	public static final DeferredRegister<Item> ITEMS =
			DeferredRegister.create(ForgeRegistries.ITEMS, CrossgroveIntegrations.MOD_ID);

	public static final RegistryObject<Item> UNFIRED_CERAMIC_BUCKET = ITEMS.register(
			"unfired_ceramic_bucket",
			() -> new Item(new Item.Properties().stacksTo(16))
	);

	public static final RegistryObject<Item> CERAMIC_BUCKET = ITEMS.register(
			"ceramic_bucket",
			() -> new CeramicBucketItem(new Item.Properties())
	);

	public static final RegistryObject<Item> CERAMIC_COPPER_BUCKET = registerFilled("copper");
	public static final RegistryObject<Item> CERAMIC_TIN_BUCKET = registerFilled("tin");
	public static final RegistryObject<Item> CERAMIC_LEAD_BUCKET = registerFilled("lead");
	public static final RegistryObject<Item> CERAMIC_GOLD_BUCKET = registerFilled("gold");
	public static final RegistryObject<Item> CERAMIC_BRONZE_BUCKET = registerFilled("bronze");

	private static final List<RegistryObject<Item>> FILLED_VARIANTS = List.of(
			CERAMIC_COPPER_BUCKET,
			CERAMIC_TIN_BUCKET,
			CERAMIC_LEAD_BUCKET,
			CERAMIC_GOLD_BUCKET,
			CERAMIC_BRONZE_BUCKET
	);

	@Nullable
	private static volatile Map<Fluid, Item> fluidToBucket;

	private CeramicItems() {
	}

	private static RegistryObject<Item> registerFilled(String gtceuFluidName) {
		ResourceLocation fluidId = ResourceLocation.fromNamespaceAndPath("gtceu", gtceuFluidName);
		Supplier<Fluid> fluidSupplier = () -> ForgeRegistries.FLUIDS.getValue(fluidId);
		return ITEMS.register(
				"ceramic_" + gtceuFluidName + "_bucket",
				() -> new FilledCeramicBucketItem(new Item.Properties(), fluidSupplier)
		);
	}

	@Nullable
	public static Item getFilledBucketFor(Fluid fluid) {
		if (fluid == null || fluid == Fluids.EMPTY) return null;
		Map<Fluid, Item> map = fluidToBucket;
		if (map == null) map = buildMap();
		return map.get(fluid);
	}

	private static synchronized Map<Fluid, Item> buildMap() {
		Map<Fluid, Item> existing = fluidToBucket;
		if (existing != null) return existing;
		Map<Fluid, Item> built = new IdentityHashMap<>();
		for (RegistryObject<Item> ro : FILLED_VARIANTS) {
			Item item = ro.get();
			if (item instanceof FilledCeramicBucketItem filled) {
				Fluid f = filled.getFluid();
				if (f != Fluids.EMPTY) built.put(f, item);
			}
		}
		fluidToBucket = built;
		return built;
	}

	public static void register(IEventBus modBus) {
		ITEMS.register(modBus);
		modBus.addListener(CeramicItems::onBuildCreativeTabs);
	}

	private static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
			event.accept(UNFIRED_CERAMIC_BUCKET);
			event.accept(CERAMIC_BUCKET);
			for (RegistryObject<Item> ro : FILLED_VARIANTS) {
				event.accept(ro);
			}
		}
	}

	public static List<RegistryObject<Item>> filledVariants() {
		return FILLED_VARIANTS;
	}
}
