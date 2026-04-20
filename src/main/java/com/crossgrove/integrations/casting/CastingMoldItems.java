package com.crossgrove.integrations.casting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;

import com.crossgrove.integrations.CrossgroveIntegrations;
import com.crossgrove.integrations.gtceu.overgeared.CrossgroveOvergearedTagPrefixes;

public final class CastingMoldItems {
	public static final DeferredRegister<Item> ITEMS =
			DeferredRegister.create(ForgeRegistries.ITEMS, CrossgroveIntegrations.MOD_ID);

	private static final List<MoldEntry> ENTRIES = new ArrayList<>();
	private static final List<RegistryObject<Item>> UNFIRED_ENTRIES = new ArrayList<>();
	private static final List<RegistryObject<Item>> CASTING_OUTPUTS = new ArrayList<>();

	public static final RegistryObject<Item> ROUGH_BRONZE_GEAR_BLANK = registerCastingOutput("rough_bronze_gear_blank");
	public static final RegistryObject<Item> ROUGH_BRONZE_AXLE_BLANK = registerCastingOutput("rough_bronze_axle_blank");
	public static final RegistryObject<Item> ROUGH_BRONZE_BUSHING_BLANK = registerCastingOutput("rough_bronze_bushing_blank");

	public static final RegistryObject<Item> UNFIRED_INGOT_CLAY_MOLD = registerUnfired("unfired_ingot_clay_mold");
	public static final RegistryObject<Item> INGOT_CASTING_MOLD = registerCustom(
			"ingot_casting_mold",
			() -> GTValues.M * 4,
			CastingMoldItems::ingotFor
	);
	public static final RegistryObject<Item> UNFIRED_PICKAXE_HEAD_CLAY_MOLD = registerUnfired("unfired_pickaxe_head_clay_mold");
	public static final RegistryObject<Item> PICKAXE_HEAD_CASTING_MOLD = register(
			"pickaxe_head_casting_mold",
			() -> CrossgroveOvergearedTagPrefixes.TOOL_HEAD_PICKAXE,
			() -> GTToolType.PICKAXE
	);
	public static final RegistryObject<Item> UNFIRED_AXE_HEAD_CLAY_MOLD = registerUnfired("unfired_axe_head_clay_mold");
	public static final RegistryObject<Item> AXE_HEAD_CASTING_MOLD = register(
			"axe_head_casting_mold",
			() -> CrossgroveOvergearedTagPrefixes.TOOL_HEAD_AXE,
			() -> GTToolType.AXE
	);
	public static final RegistryObject<Item> UNFIRED_SHOVEL_HEAD_CLAY_MOLD = registerUnfired("unfired_shovel_head_clay_mold");
	public static final RegistryObject<Item> SHOVEL_HEAD_CASTING_MOLD = register(
			"shovel_head_casting_mold",
			() -> CrossgroveOvergearedTagPrefixes.TOOL_HEAD_SHOVEL,
			() -> GTToolType.SHOVEL
	);
	public static final RegistryObject<Item> UNFIRED_HOE_HEAD_CLAY_MOLD = registerUnfired("unfired_hoe_head_clay_mold");
	public static final RegistryObject<Item> HOE_HEAD_CASTING_MOLD = register(
			"hoe_head_casting_mold",
			() -> CrossgroveOvergearedTagPrefixes.TOOL_HEAD_HOE,
			() -> GTToolType.HOE
	);
	public static final RegistryObject<Item> UNFIRED_SWORD_BLADE_CLAY_MOLD = registerUnfired("unfired_sword_blade_clay_mold");
	public static final RegistryObject<Item> SWORD_BLADE_CASTING_MOLD = register(
			"sword_blade_casting_mold",
			() -> CrossgroveOvergearedTagPrefixes.TOOL_BLADE_SWORD_PREFIX,
			() -> GTToolType.SWORD
	);
	public static final RegistryObject<Item> UNFIRED_HAMMER_HEAD_CLAY_MOLD = registerUnfired("unfired_hammer_head_clay_mold");
	public static final RegistryObject<Item> HAMMER_HEAD_CASTING_MOLD = register(
			"hammer_head_casting_mold",
			() -> CrossgroveOvergearedTagPrefixes.TOOL_HEAD_HAMMER,
			() -> GTToolType.HARD_HAMMER
	);
	public static final RegistryObject<Item> UNFIRED_FILE_HEAD_CLAY_MOLD = registerUnfired("unfired_file_head_clay_mold");
	public static final RegistryObject<Item> FILE_HEAD_CASTING_MOLD = register(
			"file_head_casting_mold",
			() -> CrossgroveOvergearedTagPrefixes.TOOL_HEAD_FILE,
			() -> GTToolType.FILE
	);
	public static final RegistryObject<Item> UNFIRED_SAW_BLADE_CLAY_MOLD = registerUnfired("unfired_saw_blade_clay_mold");
	public static final RegistryObject<Item> SAW_BLADE_CASTING_MOLD = register(
			"saw_blade_casting_mold",
			() -> CrossgroveOvergearedTagPrefixes.TOOL_BLADE_SAW,
			() -> GTToolType.SAW
	);
	public static final RegistryObject<Item> UNFIRED_SCYTHE_BLADE_CLAY_MOLD = registerUnfired("unfired_scythe_blade_clay_mold");
	public static final RegistryObject<Item> SCYTHE_BLADE_CASTING_MOLD = register(
			"scythe_blade_casting_mold",
			() -> CrossgroveOvergearedTagPrefixes.TOOL_BLADE_SCYTHE,
			() -> GTToolType.SCYTHE
	);

	private CastingMoldItems() {
	}

	private static RegistryObject<Item> register(String id, Supplier<TagPrefix> prefix, Supplier<GTToolType> toolType) {
		RegistryObject<Item> item = ITEMS.register(id, () -> new Item(new Item.Properties().stacksTo(1)));
		ENTRIES.add(MoldEntry.forPrefix(item, prefix, toolType));
		return item;
	}

	private static RegistryObject<Item> registerCustom(String id, LongSupplier materialAmount, Function<Material, ItemStack> resultFactory) {
		RegistryObject<Item> item = ITEMS.register(id, () -> new Item(new Item.Properties().stacksTo(1)));
		ENTRIES.add(MoldEntry.forCustom(item, materialAmount, resultFactory));
		return item;
	}

	private static RegistryObject<Item> registerUnfired(String id) {
		RegistryObject<Item> item = ITEMS.register(id, () -> new Item(new Item.Properties().stacksTo(1)));
		UNFIRED_ENTRIES.add(item);
		return item;
	}

	private static RegistryObject<Item> registerCastingOutput(String id) {
		RegistryObject<Item> item = ITEMS.register(id, () -> new Item(new Item.Properties()));
		CASTING_OUTPUTS.add(item);
		return item;
	}

	private static ItemStack ingotFor(Material material) {
		return ChemicalHelper.get(TagPrefix.ingot, material);
	}

	public static void register(IEventBus modBus) {
		ITEMS.register(modBus);
		modBus.addListener(CastingMoldItems::onBuildCreativeTabs);
	}

	public static List<MoldEntry> entries() {
		return List.copyOf(ENTRIES);
	}

	private static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
			for (RegistryObject<Item> item : UNFIRED_ENTRIES) {
				event.accept(item);
			}
			for (MoldEntry entry : ENTRIES) {
				event.accept(entry.item());
			}
			for (RegistryObject<Item> item : CASTING_OUTPUTS) {
				event.accept(item);
			}
		}
	}

	public record MoldEntry(RegistryObject<Item> item, Supplier<TagPrefix> prefixSupplier,
							Supplier<GTToolType> toolTypeSupplier, LongSupplier materialAmountSupplier,
							Function<Material, ItemStack> resultFactory) {
		private static MoldEntry forPrefix(RegistryObject<Item> item, Supplier<TagPrefix> prefixSupplier,
										   Supplier<GTToolType> toolTypeSupplier) {
			return new MoldEntry(item, prefixSupplier, toolTypeSupplier, null, null);
		}

		private static MoldEntry forCustom(RegistryObject<Item> item, LongSupplier materialAmountSupplier,
										   Function<Material, ItemStack> resultFactory) {
			return new MoldEntry(item, null, () -> null, materialAmountSupplier, resultFactory);
		}

		public TagPrefix prefix() {
			return prefixSupplier == null ? null : prefixSupplier.get();
		}

		public long materialAmount(Material material) {
			TagPrefix prefix = prefix();
			return prefix == null ? materialAmountSupplier.getAsLong() : prefix.getMaterialAmount(material);
		}

		public ItemStack output(Material material) {
			if (resultFactory == null) {
				return ItemStack.EMPTY;
			}
			return resultFactory.apply(material);
		}

		public GTToolType toolType() {
			return toolTypeSupplier.get();
		}
	}
}
