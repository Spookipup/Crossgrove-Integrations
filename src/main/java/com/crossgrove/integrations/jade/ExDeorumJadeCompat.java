package com.crossgrove.integrations.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.fluids.FluidStack;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.config.IPluginConfig;
import thedarkcolour.exdeorum.blockentity.BarrelBlockEntity;

import com.crossgrove.integrations.CrossgroveIntegrations;
import com.crossgrove.integrations.casting.ExDeorumCastingHandler;

final class ExDeorumJadeCompat {
	private ExDeorumJadeCompat() {
	}

	static void registerCommon(IWailaCommonRegistration registration) {
		registration.registerBlockDataProvider(new BarrelMixtureTooltipProvider(), BarrelBlockEntity.class);
	}

	static void registerClient(IWailaClientRegistration registration) {
		registration.addConfig(BarrelMixtureTooltipProvider.UID, true);
		registration.registerBlockComponent(new BarrelMixtureTooltipProvider(), Block.class);
	}

	private static final class BarrelMixtureTooltipProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
		private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
				CrossgroveIntegrations.MOD_ID,
				"exdeorum_barrel_mixture"
		);
		private static final String SERVER_DATA_KEY = "crossgrove_barrel_mixture";
		private static final String BRONZE_AMOUNT_KEY = "bronze_amount";

		@Override
		public ResourceLocation getUid() {
			return UID;
		}

		@Override
		public void appendServerData(CompoundTag data, BlockAccessor accessor) {
			BlockEntity blockEntity = accessor.getBlockEntity();
			if (!(blockEntity instanceof BarrelBlockEntity barrel)) {
				return;
			}

			CompoundTag hiddenMixture = blockEntity.getPersistentData().getCompound(ExDeorumCastingHandler.MIXTURE_TAG);
			CompoundTag visibleMixture = visibleMixture(barrel, hiddenMixture);
			if (!hasVisibleMixture(visibleMixture) || !shouldShowMixture(hiddenMixture, visibleMixture)) {
				return;
			}

			data.put(SERVER_DATA_KEY, visibleMixture);
		}

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			if (!config.get(UID)) {
				return;
			}

			CompoundTag serverData = accessor.getServerData();
			if (!serverData.contains(SERVER_DATA_KEY, Tag.TAG_COMPOUND)) {
				return;
			}

			CompoundTag mixture = serverData.getCompound(SERVER_DATA_KEY);
			if (!hasVisibleMixture(mixture)) {
				return;
			}

			tooltip.add(Component.literal("Mixture:").withStyle(ChatFormatting.GRAY));
			addMixtureLine(tooltip, mixture, ExDeorumCastingHandler.COPPER_KEY, "Copper", ChatFormatting.GOLD);
			addMixtureLine(tooltip, mixture, ExDeorumCastingHandler.TIN_KEY, "Tin", ChatFormatting.AQUA);
			int bronzeAmount = mixture.getInt(BRONZE_AMOUNT_KEY);
			if (bronzeAmount > 0) {
				tooltip.add(Component.literal("  Bronze: ")
						.withStyle(ChatFormatting.GRAY)
						.append(Component.literal(bronzeAmount + " mB")
								.withStyle(ChatFormatting.YELLOW)));
			}
		}

		private static CompoundTag visibleMixture(BarrelBlockEntity barrel, CompoundTag hiddenMixture) {
			int copper = hiddenMixture.getInt(ExDeorumCastingHandler.COPPER_KEY);
			int tin = hiddenMixture.getInt(ExDeorumCastingHandler.TIN_KEY);
			int bronze = hiddenMixture.getInt(ExDeorumCastingHandler.BRONZE_KEY);

			FluidStack stored = barrel.getTank().getFluid();
			Material tankMaterial = material(stored);
			if (isMaterial(tankMaterial, GTMaterials.Copper)) {
				copper += stored.getAmount();
			} else if (isMaterial(tankMaterial, GTMaterials.Tin)) {
				tin += stored.getAmount();
			} else if (isMaterial(tankMaterial, GTMaterials.Bronze)) {
				bronze += stored.getAmount();
			}

			CompoundTag visibleMixture = new CompoundTag();
			if (bronze > 0) {
				int bronzeTin = bronzeTinAmount(bronze);
				copper += bronze - bronzeTin;
				tin += bronzeTin;
				visibleMixture.putInt(BRONZE_AMOUNT_KEY, bronze);
			}
			putPositive(visibleMixture, ExDeorumCastingHandler.COPPER_KEY, copper);
			putPositive(visibleMixture, ExDeorumCastingHandler.TIN_KEY, tin);
			return visibleMixture;
		}

		private static boolean shouldShowMixture(CompoundTag hiddenMixture, CompoundTag visibleMixture) {
			return hasVisibleMixture(hiddenMixture)
					|| visibleMixture.getInt(BRONZE_AMOUNT_KEY) > 0
					|| (visibleMixture.getInt(ExDeorumCastingHandler.COPPER_KEY) > 0
					&& visibleMixture.getInt(ExDeorumCastingHandler.TIN_KEY) > 0);
		}

		private static boolean hasVisibleMixture(CompoundTag mixture) {
			return mixture.getInt(ExDeorumCastingHandler.COPPER_KEY) > 0
					|| mixture.getInt(ExDeorumCastingHandler.TIN_KEY) > 0
					|| mixture.getInt(ExDeorumCastingHandler.BRONZE_KEY) > 0;
		}

		private static void addMixtureLine(ITooltip tooltip, CompoundTag mixture, String key, String label,
				ChatFormatting color) {
			int amount = mixture.getInt(key);
			if (amount > 0) {
				tooltip.add(Component.literal("  " + label + ": ")
						.withStyle(ChatFormatting.GRAY)
						.append(Component.literal(amount + " mB")
								.withStyle(color)));
			}
		}

		private static Material material(FluidStack stack) {
			if (stack.isEmpty()) {
				return null;
			}
			return ChemicalHelper.getMaterial(stack.getFluid());
		}

		private static boolean isMaterial(Material material, Material expected) {
			return material != null && (material == expected || material.equals(expected));
		}

		private static int bronzeTinAmount(int bronzeAmount) {
			if (bronzeAmount <= 0) {
				return 0;
			}
			return (bronzeAmount + 3) / 4;
		}

		private static void putPositive(CompoundTag tag, String key, int amount) {
			if (amount > 0) {
				tag.putInt(key, amount);
			}
		}
	}
}
