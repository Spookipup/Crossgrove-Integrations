package com.crossgrove.integrations.jade;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import com.Da_Technomancer.crossroads.CRConfig;
import com.Da_Technomancer.crossroads.api.heat.HeatUtil;
import snownee.jade.addon.harvest.HarvestToolProvider;
import snownee.jade.addon.harvest.ToolHandler;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

import com.crossgrove.integrations.CrossgroveIntegrations;
import com.crossgrove.integrations.gtceu.GtceuHeatProvider;
import com.crossgrove.integrations.gtceu.GtceuHeatTooltipData;

@WailaPlugin(CrossgroveIntegrations.MOD_ID)
public final class CrossgroveJadePlugin implements IWailaPlugin {
	private static final GtceuHeatTooltipProvider HEAT_PROVIDER = new GtceuHeatTooltipProvider();

	@Override
	public void register(IWailaCommonRegistration registration) {
		registration.registerBlockDataProvider(HEAT_PROVIDER, BlockEntity.class);
		if (isExDeorumLoaded()) {
			ExDeorumJadeCompat.registerCommon(registration);
		}
	}

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.addConfig(GtceuHeatTooltipProvider.UID, true);
		registration.addConfig(GtceuHeatTooltipProvider.POWER_LABEL_UID, false);
		HarvestToolProvider.registerHandler(new CrossgrovePickaxeToolHandler());
		HarvestToolProvider.resultCache.invalidateAll();
		registration.registerBlockComponent(HEAT_PROVIDER, Block.class);
		if (isExDeorumLoaded()) {
			ExDeorumJadeCompat.registerClient(registration);
		}
	}

	private static boolean isExDeorumLoaded() {
		return ModList.get().isLoaded("exdeorum");
	}

	private static final class CrossgrovePickaxeToolHandler implements ToolHandler {
		private static final TagKey<Block> NEEDS_BRONZE_TOOL = blockTag(
				CrossgroveIntegrations.MOD_ID,
				"needs_bronze_tool"
		);
		private static final TagKey<Block> NEEDS_WOOD_TOOL = blockTag("forge", "needs_wood_tool");
		private static final TagKey<Block> NEEDS_NETHERITE_TOOL = blockTag("forge", "needs_netherite_tool");
		private static final TagKey<Block> NEEDS_COPPER_TOOL = blockTag("overgeared", "needs_copper_tool");
		private static final TagKey<Block> NEEDS_STEEL_TOOL = blockTag("overgeared", "needs_steel_tool");
		private static final TagKey<Block> NEEDS_DURANIUM_TOOL = blockTag("gtceu", "needs_duranium_tool");
		private static final TagKey<Block> NEEDS_NEUTRONIUM_TOOL = blockTag("gtceu", "needs_neutronium_tool");
		private static final TierRule[] TIERS = {
				new TierRule(NEEDS_NEUTRONIUM_TOOL, item("gtceu", "neutronium_pickaxe"), item("minecraft", "netherite_pickaxe")),
				new TierRule(NEEDS_DURANIUM_TOOL, item("gtceu", "duranium_pickaxe"), item("minecraft", "netherite_pickaxe")),
				new TierRule(NEEDS_NETHERITE_TOOL, item("minecraft", "netherite_pickaxe")),
				new TierRule(NEEDS_STEEL_TOOL, item("gtceu", "steel_pickaxe"), item("overgeared", "steel_pickaxe"),
						item("minecraft", "diamond_pickaxe")),
				new TierRule(BlockTags.NEEDS_DIAMOND_TOOL, item("minecraft", "diamond_pickaxe")),
				new TierRule(NEEDS_BRONZE_TOOL, item("gtceu", "bronze_pickaxe"), item("minecraft", "iron_pickaxe")),
				new TierRule(BlockTags.NEEDS_IRON_TOOL, item("minecraft", "iron_pickaxe")),
				new TierRule(BlockTags.NEEDS_STONE_TOOL, item("gtceu", "copper_pickaxe"), item("overgeared", "copper_pickaxe"),
						item("minecraft", "stone_pickaxe")),
				new TierRule(NEEDS_COPPER_TOOL, item("gtceu", "copper_pickaxe"), item("overgeared", "copper_pickaxe"),
						item("minecraft", "stone_pickaxe")),
				new TierRule(NEEDS_WOOD_TOOL, item("survivalistessentials", "flint_shard"),
						item("survivalistessentials", "rock_stone"), item("overgeared", "knappable_rock"),
						item("minecraft", "wooden_pickaxe"))
		};
		private static final ItemRef[] DEFAULT_PRIMITIVE_REPRESENTATIVES = {
				item("survivalistessentials", "flint_shard"),
				item("survivalistessentials", "rock_stone"),
				item("overgeared", "knappable_rock"),
				item("minecraft", "wooden_pickaxe")
		};

		@Override
		public ItemStack test(BlockState state, Level level, BlockPos pos) {
			if (!state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
				return ItemStack.EMPTY;
			}

			for (TierRule tier : TIERS) {
				if (tier.matches(state)) {
					return firstExisting(tier.representatives());
				}
			}
			return firstExisting(DEFAULT_PRIMITIVE_REPRESENTATIVES);
		}

		@Override
		public List<ItemStack> getTools() {
			List<ItemStack> tools = new ArrayList<>();
			for (TierRule tier : TIERS) {
				addExisting(tools, tier.representatives());
			}
			addExisting(tools, DEFAULT_PRIMITIVE_REPRESENTATIVES);
			return tools;
		}

		@Override
		public String getName() {
			return "pickaxe";
		}

		private static ItemStack firstExisting(ItemRef... refs) {
			for (ItemRef ref : refs) {
				ItemStack stack = stack(ref.id());
				if (!stack.isEmpty()) {
					return stack;
				}
			}
			return ItemStack.EMPTY;
		}

		private static void addExisting(List<ItemStack> tools, ItemRef... refs) {
			for (ItemRef ref : refs) {
				ItemStack stack = stack(ref.id());
				if (!stack.isEmpty() && tools.stream().noneMatch(existing -> ItemStack.isSameItem(existing, stack))) {
					tools.add(stack);
				}
			}
		}

		private static ItemStack stack(ResourceLocation id) {
			Item item = ForgeRegistries.ITEMS.getValue(id);
			return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
		}

		private static ItemRef item(String namespace, String path) {
			return new ItemRef(id(namespace, path));
		}

		private static ResourceLocation id(String namespace, String path) {
			return ResourceLocation.fromNamespaceAndPath(namespace, path);
		}

		private static TagKey<Block> blockTag(String namespace, String path) {
			return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(namespace, path));
		}

		private record TierRule(TagKey<Block> tag, ItemRef... representatives) {
			private boolean matches(BlockState state) {
				return state.is(tag);
			}
		}

		private record ItemRef(ResourceLocation id) {
		}
	}

	private static final class GtceuHeatTooltipProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
		private static final ResourceLocation UID = GtceuHeatTooltipData.UID;
		private static final ResourceLocation POWER_LABEL_UID = ResourceLocation.fromNamespaceAndPath(
				CrossgroveIntegrations.MOD_ID,
				"gtceu_crossroads_power_label"
		);

		@Override
		public ResourceLocation getUid() {
			return UID;
		}

		@Override
		public void appendServerData(CompoundTag data, BlockAccessor accessor) {
			BlockEntity blockEntity = accessor.getBlockEntity();
			if (blockEntity == null) {
				return;
			}
			GtceuHeatProvider.writeTooltipData(blockEntity, data);
		}

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			CompoundTag serverData = accessor.getServerData();
			if (!GtceuHeatTooltipData.hasHeat(serverData)) {
				return;
			}

			double temperature = GtceuHeatTooltipData.getTemperature(serverData);
			addCrossroadsHeatInfo(tooltip, accessor, temperature);

			if (GtceuHeatTooltipData.hasSharedMultiblockHeat(serverData)) {
				tooltip.add(Component.literal("Shared heat body: ")
						.withStyle(ChatFormatting.GRAY)
						.append(Component.literal(GtceuHeatTooltipData.getMultiblockHeatParts(serverData)
								+ " ports / "
								+ GtceuHeatTooltipData.getMultiblockStructureBlocks(serverData)
								+ " blocks")
								.withStyle(ChatFormatting.GREEN)));
			}

			if (GtceuHeatTooltipData.heatPowersMachine(serverData) && config.get(POWER_LABEL_UID)) {
				tooltip.add(Component.literal("Power: ")
						.withStyle(ChatFormatting.GRAY)
						.append(Component.literal(crossroadsPowerText(serverData))
								.withStyle(ChatFormatting.GOLD)));
			}

			if (GtceuHeatTooltipData.hasMinimumWorkingTemperature(serverData)) {
				double minimum = GtceuHeatTooltipData.getMinimumWorkingTemperature(serverData);
				tooltip.add(Component.literal("Working heat: ")
						.withStyle(ChatFormatting.GRAY)
						.append(Component.literal(GtceuHeatTooltipData.formatTemperature(minimum) + " C min")
								.withStyle(temperature >= minimum ? ChatFormatting.GREEN : ChatFormatting.RED)));
			}

			if (GtceuHeatTooltipData.hasIdealTemperatureRange(serverData)) {
				tooltip.add(Component.literal("Ideal heat: ")
						.withStyle(ChatFormatting.GRAY)
						.append(Component.literal(GtceuHeatTooltipData.formatTemperature(GtceuHeatTooltipData.getIdealMinTemperature(serverData))
								+ "-" + GtceuHeatTooltipData.formatTemperature(GtceuHeatTooltipData.getIdealMaxTemperature(serverData)) + " C")
								.withStyle(colorForIdealRange(temperature, serverData))));
			}

			if (GtceuHeatTooltipData.hasSafeTemperature(serverData)) {
				tooltip.add(Component.literal("Safe below: ")
						.withStyle(ChatFormatting.GRAY)
						.append(Component.literal(GtceuHeatTooltipData.formatTemperature(GtceuHeatTooltipData.getSafeTemperature(serverData)) + " C")
								.withStyle(ChatFormatting.YELLOW)));
			}

			if (GtceuHeatTooltipData.hasMinimumRotarySpeed(serverData)) {
				double speed = GtceuHeatTooltipData.getRotarySpeed(serverData);
				double minimum = GtceuHeatTooltipData.getMinimumRotarySpeed(serverData);
				addCrossroadsRotaryInfo(tooltip, speed, GtceuHeatTooltipData.getRotaryEnergy(serverData));
				tooltip.add(Component.literal("Required speed: ")
						.withStyle(ChatFormatting.GRAY)
						.append(Component.literal(formatRotarySpeed(minimum) + " rad/s")
								.withStyle(speed >= minimum ? ChatFormatting.GREEN : ChatFormatting.RED)));
			}

			if (GtceuHeatTooltipData.hasIdealRotarySpeedRange(serverData)) {
				tooltip.add(Component.literal("Ideal rotary: ")
						.withStyle(ChatFormatting.GRAY)
						.append(Component.literal(formatRotarySpeed(GtceuHeatTooltipData.getIdealMinRotarySpeed(serverData))
								+ "-" + formatRotarySpeed(GtceuHeatTooltipData.getIdealMaxRotarySpeed(serverData)) + " rad/s")
								.withStyle(colorForIdealRotaryRange(GtceuHeatTooltipData.getRotarySpeed(serverData), serverData))));
			}

			if (GtceuHeatTooltipData.isHeatSuspended(serverData)) {
				tooltip.add(Component.literal(statusText(serverData))
						.withStyle(statusColor(serverData)));
			}
		}

		private static void addCrossroadsHeatInfo(ITooltip tooltip, BlockAccessor accessor, double temperature) {
			List<Component> heatInfo = new ArrayList<>();
			HeatUtil.addHeatInfo(heatInfo, temperature, HeatUtil.convertBiomeTemp(accessor.getLevel(), accessor.getPosition()));
			heatInfo.forEach(tooltip::add);
		}

		private static void addCrossroadsRotaryInfo(ITooltip tooltip, double speed, double energy) {
			tooltip.add(Component.translatable("tt.crossroads.boilerplate.rotary.speed",
					CRConfig.formatVal(speed),
					CRConfig.formatVal(speed * 60D / (2D * Math.PI))));
			tooltip.add(Component.translatable("tt.crossroads.boilerplate.rotary.energy", CRConfig.formatVal(energy)));
		}

		private static ChatFormatting colorForIdealRange(double temperature, CompoundTag serverData) {
			double idealMin = GtceuHeatTooltipData.getIdealMinTemperature(serverData);
			double idealMax = GtceuHeatTooltipData.getIdealMaxTemperature(serverData);
			return temperature >= idealMin && temperature <= idealMax ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
		}

		private static String statusText(CompoundTag serverData) {
			return switch (GtceuHeatTooltipData.getHeatSuspensionReason(serverData)) {
				case "too_cold" -> "Status: waiting for heat";
				case "too_hot" -> "Status: heat suspended";
				case "no_rotary" -> "Status: waiting for rotary power";
				default -> "Status: heat limited";
			};
		}

		private static ChatFormatting statusColor(CompoundTag serverData) {
			return switch (GtceuHeatTooltipData.getHeatSuspensionReason(serverData)) {
				case "too_cold" -> ChatFormatting.AQUA;
				case "too_hot" -> ChatFormatting.RED;
				case "no_rotary" -> ChatFormatting.LIGHT_PURPLE;
				default -> ChatFormatting.YELLOW;
			};
		}

		private static ChatFormatting colorForIdealRotaryRange(double speed, CompoundTag serverData) {
			double idealMin = GtceuHeatTooltipData.getIdealMinRotarySpeed(serverData);
			double idealMax = GtceuHeatTooltipData.getIdealMaxRotarySpeed(serverData);
			return speed >= idealMin && speed <= idealMax ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
		}

		private static String formatRotarySpeed(double speed) {
			return CRConfig.formatVal(speed);
		}

		private static String crossroadsPowerText(CompoundTag serverData) {
			boolean heat = GtceuHeatTooltipData.hasMinimumWorkingTemperature(serverData);
			boolean rotary = GtceuHeatTooltipData.hasMinimumRotarySpeed(serverData);
			if (heat && rotary) {
				return "heat + rotary";
			}
			if (rotary) {
				return "rotary";
			}
			return "heat";
		}
	}
}
