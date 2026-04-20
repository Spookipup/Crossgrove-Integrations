package com.crossgrove.integrations.casting;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.common.SoundActions;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.registries.ForgeRegistries;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import thedarkcolour.exdeorum.blockentity.AbstractCrucibleBlockEntity;
import thedarkcolour.exdeorum.blockentity.BarrelBlockEntity;
import thedarkcolour.exdeorum.blockentity.ETankBlockEntity;

import com.crossgrove.integrations.casting.CastingMoldItems.MoldEntry;
import com.crossgrove.integrations.ceramic.CeramicBucketItem;
import com.crossgrove.integrations.ceramic.CeramicItems;

public final class ExDeorumCastingHandler {
	public static final String MIXTURE_TAG = "CrossgroveMetalMixture";
	public static final String COPPER_KEY = "copper";
	public static final String TIN_KEY = "tin";
	public static final String BRONZE_KEY = "bronze";
	private static final int CASTING_AMOUNT_DIVISOR = 4;
	private static final Map<ResourceLocation, MoldEntry> GTCEU_REUSABLE_MOLDS = Map.ofEntries(
			Map.entry(gtceuMold("ingot_casting_mold"), reusableMold(TagPrefix.ingot)),
			Map.entry(gtceuMold("nugget_casting_mold"), reusableMold(TagPrefix.nugget)),
			Map.entry(gtceuMold("block_casting_mold"), reusableMold(TagPrefix.block)),
			Map.entry(gtceuMold("plate_casting_mold"), reusableMold(TagPrefix.plate)),
			Map.entry(gtceuMold("gear_casting_mold"), reusableMold(TagPrefix.gear)),
			Map.entry(gtceuMold("small_gear_casting_mold"), reusableMold(TagPrefix.gearSmall)),
			Map.entry(gtceuMold("rotor_casting_mold"), reusableMold(TagPrefix.rotor)),
			Map.entry(gtceuMold("tiny_pipe_casting_mold"), reusableMold(TagPrefix.pipeTinyFluid)),
			Map.entry(gtceuMold("small_pipe_casting_mold"), reusableMold(TagPrefix.pipeSmallFluid)),
			Map.entry(gtceuMold("normal_pipe_casting_mold"), reusableMold(TagPrefix.pipeNormalFluid)),
			Map.entry(gtceuMold("large_pipe_casting_mold"), reusableMold(TagPrefix.pipeLargeFluid)),
			Map.entry(gtceuMold("huge_pipe_casting_mold"), reusableMold(TagPrefix.pipeHugeFluid))
	);

	private static Map<Item, MoldEntry> moldsByItem;

	private ExDeorumCastingHandler() {
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onTankUse(PlayerInteractEvent.RightClickBlock event) {
		if (handleStackedCeramicBucketFill(event)) {
			return;
		}

		MoldUse mold = getMoldUse(event.getItemStack());
		if (mold != null) {
			handleMoldUse(event, mold);
			return;
		}

		handleFluidContainerUse(event);
	}

	private static boolean handleStackedCeramicBucketFill(PlayerInteractEvent.RightClickBlock event) {
		ItemStack held = event.getEntity().getItemInHand(event.getHand());
		if (held.getCount() <= 1 || !held.is(CeramicItems.CERAMIC_BUCKET.get())) {
			return false;
		}

		Level level = event.getLevel();
		BlockPos pos = event.getPos();
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity == null || hasSolids(blockEntity)) {
			return false;
		}
		IFluidTank tank = tankFor(blockEntity);
		if (tank == null) {
			return false;
		}

		FluidStack stored = tank.getFluid();
		if (stored.isEmpty()) {
			return false;
		}

		Item filledBucket = CeramicItems.getFilledBucketFor(stored.getFluid());
		if (filledBucket == null) {
			return false;
		}

		FluidStack requested = new FluidStack(stored.getFluid(), CeramicBucketItem.CAPACITY);
		if (stored.hasTag()) {
			requested.setTag(stored.getTag().copy());
		}

		FluidStack simulated = tank.drain(requested, IFluidHandler.FluidAction.SIMULATE);
		if (!canFillCeramicBucket(simulated, requested)) {
			return false;
		}

		completeInteraction(event, level);
		if (level.isClientSide) {
			return true;
		}

		FluidStack drained = tank.drain(requested, IFluidHandler.FluidAction.EXECUTE);
		if (!canFillCeramicBucket(drained, requested)) {
			return true;
		}

		Player player = event.getEntity();
		if (!player.getAbilities().instabuild) {
			held.shrink(1);
		}
		giveOrDrop(player, new ItemStack(filledBucket));
		updateBlock(level, pos, blockEntity);
		playBucketFillSound(level, pos, drained);
		return true;
	}

	private static IFluidTank tankFor(BlockEntity blockEntity) {
		if (blockEntity instanceof ETankBlockEntity tankBlock) {
			return tankBlock.getTank();
		}
		if (blockEntity instanceof AbstractCrucibleBlockEntity crucible) {
			return crucible.getTank();
		}
		return null;
	}

	private static boolean canFillCeramicBucket(FluidStack drained, FluidStack requested) {
		return !drained.isEmpty()
				&& drained.getAmount() >= CeramicBucketItem.CAPACITY
				&& drained.isFluidEqual(requested);
	}

	private static void playBucketFillSound(Level level, BlockPos pos, FluidStack fluid) {
		SoundEvent sound = fluid.getFluid().getFluidType().getSound(fluid, SoundActions.BUCKET_FILL);
		if (sound == null) {
			sound = SoundEvents.BUCKET_FILL_LAVA;
		}
		level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
	}

	private static void handleMoldUse(PlayerInteractEvent.RightClickBlock event, MoldUse mold) {
		Level level = event.getLevel();
		BlockPos pos = event.getPos();
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (!(blockEntity instanceof ETankBlockEntity tankBlock) || hasSolids(blockEntity)) {
			return;
		}
		if (level.isClientSide) {
			return;
		}

		CastResult result = findResult(tankBlock.getTank(), mold);
		if (result != null) {
			completeInteraction(event, level);

			FluidStack drained = tankBlock.getTank().drain(result.toDrain().getAmount(), IFluidHandler.FluidAction.EXECUTE);
			if (drained.getAmount() != result.toDrain().getAmount() || drained.getFluid() != result.toDrain().getFluid()) {
				return;
			}

			if (mold.consume()) {
				consumeMold(event);
			}
			giveOrDrop(event.getEntity(), result.output());
			if (blockEntity instanceof BarrelBlockEntity barrel) {
				exposeBronzeInTank(barrel);
			}
			updateBlock(level, pos, blockEntity);
			level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.3F);
			return;
		}

		if (!(blockEntity instanceof BarrelBlockEntity barrel)) {
			event.getEntity().displayClientMessage(moldFailure(tankBlock.getTank(), mold), true);
			return;
		}

		MixtureCastResult mixtureResult = findMixtureResult(barrel, mold);
		if (mixtureResult == null) {
			event.getEntity().displayClientMessage(moldFailure(tankBlock.getTank(), mold), true);
			return;
		}

		completeInteraction(event, level);

		drainMixture(barrel, mixtureResult);
		exposeBronzeInTank(barrel);
		if (mold.consume()) {
			consumeMold(event);
		}
		giveOrDrop(event.getEntity(), mixtureResult.output());
		updateBlock(level, pos, blockEntity);
		level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.3F);
	}

	private static void handleFluidContainerUse(PlayerInteractEvent.RightClickBlock event) {
		Level level = event.getLevel();
		BlockPos pos = event.getPos();
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (!(blockEntity instanceof BarrelBlockEntity barrel) || hasSolids(blockEntity)) {
			return;
		}

		ItemStack held = event.getItemStack();
		IFluidHandlerItem handler = held.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
		if (handler == null) {
			return;
		}

		FluidStack simulated = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
		Material material = materialForMixture(simulated);
		if (material == null) {
			return;
		}

		Material tankMaterial = materialForMixture(barrel.getTank().getFluid());
		if (!barrel.getTank().getFluid().isEmpty() && tankMaterial == null) {
			return;
		}

		boolean hasExistingMixture = hasMixture(barrel);
		if (!hasExistingMixture && tankMaterial == null) {
			return;
		}
		if (!hasExistingMixture && isMaterial(tankMaterial, material) && !isMaterial(tankMaterial, GTMaterials.Bronze)) {
			return;
		}

		int room = mixtureRoom(barrel);
		FluidStack drainable = room <= 0
				? FluidStack.EMPTY
				: handler.drain(Math.min(simulated.getAmount(), room), IFluidHandler.FluidAction.SIMULATE);
		if (drainable.isEmpty()) {
			return;
		}

		completeInteraction(event, level);
		if (level.isClientSide) {
			return;
		}

		if (tankMaterial != null && !isMaterial(tankMaterial, GTMaterials.Bronze)) {
			absorbCompatibleTankFluid(barrel);
		}

		FluidStack drained = handler.drain(Math.min(drainable.getAmount(), mixtureRoom(barrel)), IFluidHandler.FluidAction.EXECUTE);
		if (drained.isEmpty()) {
			return;
		}
		addMixtureAmount(barrel, material, drained.getAmount());
		exposeBronzeInTank(barrel);
		InteractionHand hand = event.getHand();
		event.getEntity().setItemInHand(hand, handler.getContainer());
		updateBlock(level, pos, blockEntity);
		level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 0.5F, 1.0F);
	}

	private static boolean hasSolids(BlockEntity blockEntity) {
		return blockEntity instanceof BarrelBlockEntity barrel && !barrel.hasNoSolids();
	}

	private static MoldUse getMoldUse(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		MoldEntry mold = getConsumableMold(stack);
		if (mold != null) {
			return new MoldUse(mold, true, 1);
		}

		ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
		if (itemId == null) {
			return null;
		}
		mold = GTCEU_REUSABLE_MOLDS.get(itemId);
		return mold == null ? null : new MoldUse(mold, false, CASTING_AMOUNT_DIVISOR);
	}

	private static MoldEntry getConsumableMold(ItemStack stack) {
		Map<Item, MoldEntry> map = moldsByItem;
		if (map == null) {
			map = buildMoldMap();
		}
		return map.get(stack.getItem());
	}

	private static synchronized Map<Item, MoldEntry> buildMoldMap() {
		Map<Item, MoldEntry> existing = moldsByItem;
		if (existing != null) {
			return existing;
		}

		Map<Item, MoldEntry> built = new HashMap<>();
		for (MoldEntry entry : CastingMoldItems.entries()) {
			built.put(entry.item().get(), entry);
		}
		moldsByItem = built;
		return built;
	}

	private static CastResult findResult(IFluidTank tank, MoldUse mold) {
		FluidStack stored = tank.getFluid();
		if (stored.isEmpty()) {
			return null;
		}

		Material material = materialForFluid(stored);
		if (material == null) {
			return null;
		}

		long amount = castingAmount(mold, material);
		if (amount <= 0 || amount > Integer.MAX_VALUE || stored.getAmount() < amount) {
			return null;
		}

		ItemStack output = outputFor(mold.entry(), material);
		if (output.isEmpty()) {
			return null;
		}

		return new CastResult(output.copy(), new FluidStack(stored.getFluid(), (int) amount));
	}

	private static MixtureCastResult findMixtureResult(BarrelBlockEntity barrel, MoldUse mold) {
		CompoundTag mixture = getMixture(barrel);
		int copper = mixture.getInt(COPPER_KEY);
		int tin = mixture.getInt(TIN_KEY);
		int bronze = mixture.getInt(BRONZE_KEY);
		if (copper <= 0 && tin <= 0 && bronze <= 0) {
			return null;
		}

		MixtureCastResult bronzeResult = findBronzeMixtureResult(mold, copper, tin, bronze);
		if (bronzeResult != null) {
			return bronzeResult;
		}

		if (tin <= 0 && bronze <= 0) {
			return findSingleMaterialMixtureResult(mold, GTMaterials.Copper, copper, COPPER_KEY);
		}
		if (copper <= 0 && bronze <= 0) {
			return findSingleMaterialMixtureResult(mold, GTMaterials.Tin, tin, TIN_KEY);
		}

		return null;
	}

	private static MixtureCastResult findBronzeMixtureResult(MoldUse mold, int copper, int tin, int bronze) {
		long amount = castingAmount(mold, GTMaterials.Bronze);
		if (amount <= 0 || amount > Integer.MAX_VALUE) {
			return null;
		}
		ItemStack output = outputFor(mold.entry(), GTMaterials.Bronze);
		if (output.isEmpty()) {
			return null;
		}

		int required = (int) amount;
		int bronzeDrain = Math.min(bronze, required);
		int remaining = required - bronzeDrain;
		int tinDrain = bronzeTinAmount(remaining);
		int copperDrain = remaining - tinDrain;
		if (copper < copperDrain || tin < tinDrain) {
			return null;
		}

		return new MixtureCastResult(output.copy(), copperDrain, tinDrain, bronzeDrain);
	}

	private static MixtureCastResult findSingleMaterialMixtureResult(MoldUse mold, Material material, int stored, String key) {
		long amount = castingAmount(mold, material);
		if (amount <= 0 || amount > Integer.MAX_VALUE || stored < amount) {
			return null;
		}
		ItemStack output = outputFor(mold.entry(), material);
		if (output.isEmpty()) {
			return null;
		}

		int drain = (int) amount;
		return switch (key) {
			case COPPER_KEY -> new MixtureCastResult(output.copy(), drain, 0, 0);
			case TIN_KEY -> new MixtureCastResult(output.copy(), 0, drain, 0);
			case BRONZE_KEY -> new MixtureCastResult(output.copy(), 0, 0, drain);
			default -> null;
		};
	}

	private static ItemStack outputFor(MoldEntry mold, Material material) {
		if (material == null) {
			return ItemStack.EMPTY;
		}
		ItemStack output = mold.output(material);
		if (!output.isEmpty()) {
			return output;
		}

		if (mold.prefix() == null) {
			return ItemStack.EMPTY;
		}

		output = ChemicalHelper.get(mold.prefix(), material);
		if (!output.isEmpty()) {
			return output;
		}

		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
				"gtceu",
				mold.prefix().idPattern().formatted(material.getName())
		);
		Item item = ForgeRegistries.ITEMS.getValue(id);
		if (item == null || item == Items.AIR) {
			return ItemStack.EMPTY;
		}
		return new ItemStack(item);
	}

	private static Component moldFailure(IFluidTank tank, MoldUse mold) {
		FluidStack stored = tank.getFluid();
		if (stored.isEmpty()) {
			return Component.literal("This mold needs molten metal.");
		}

		Material material = materialForFluid(stored);
		if (material == null) {
			ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(stored.getFluid());
			return Component.literal("This mold cannot identify " + (fluidId == null ? "this fluid" : fluidId) + ".");
		}

		long amount = castingAmount(mold, material);
		if (amount <= 0 || amount > Integer.MAX_VALUE) {
			return Component.literal("This mold cannot measure " + material.getLocalizedName() + ".");
		}
		if (stored.getAmount() < amount) {
			return Component.literal("This mold needs " + amount + " mB of molten " + material.getLocalizedName() + ".");
		}

		ItemStack output = outputFor(mold.entry(), material);
		if (output.isEmpty()) {
			return Component.literal("No cast part exists for " + material.getLocalizedName() + " with this mold.");
		}

		return Component.literal("This mold needs more compatible molten metal.");
	}

	private static long castingAmount(MoldUse mold, Material material) {
		long materialAmount = mold.entry().materialAmount(material);
		if (materialAmount <= 0) {
			return materialAmount;
		}
		if (mold.materialAmountMultiplier() > 1) {
			if (materialAmount > Long.MAX_VALUE / mold.materialAmountMultiplier()) {
				return Long.MAX_VALUE;
			}
			materialAmount *= mold.materialAmountMultiplier();
		}

		long denominator = (long) GTValues.M * CASTING_AMOUNT_DIVISOR;
		long numerator = materialAmount * 144L;
		return Math.max(1, (numerator + denominator - 1) / denominator);
	}

	private static ResourceLocation gtceuMold(String path) {
		return ResourceLocation.fromNamespaceAndPath("gtceu", path);
	}

	private static MoldEntry reusableMold(TagPrefix prefix) {
		return new MoldEntry(null, () -> prefix, () -> null, null, null);
	}

	private static int bronzeTinAmount(int bronzeAmount) {
		if (bronzeAmount <= 0) {
			return 0;
		}
		return (bronzeAmount + 3) / 4;
	}

	private static boolean absorbCompatibleTankFluid(BarrelBlockEntity barrel) {
		IFluidTank tank = barrel.getTank();
		FluidStack stored = tank.getFluid();
		Material material = materialForMixture(stored);
		if (material == null) {
			return false;
		}

		FluidStack drained = tank.drain(stored.copy(), IFluidHandler.FluidAction.EXECUTE);
		if (drained.isEmpty()) {
			return false;
		}

		addMixtureAmount(barrel, material, drained.getAmount());
		return true;
	}

	private static void exposeBronzeInTank(BarrelBlockEntity barrel) {
		CompoundTag data = barrel.getPersistentData();
		CompoundTag mixture = data.getCompound(MIXTURE_TAG);

		int copper = mixture.getInt(COPPER_KEY);
		int tin = mixture.getInt(TIN_KEY);
		int bronze = mixture.getInt(BRONZE_KEY);
		int bronzeFromComponents = bronzeAmountFromComponents(copper, tin);
		if (bronzeFromComponents > 0) {
			int tinDrain = bronzeTinAmount(bronzeFromComponents);
			int copperDrain = bronzeFromComponents - tinDrain;
			copper -= copperDrain;
			tin -= tinDrain;
			bronze = cappedAdd(bronze, bronzeFromComponents);
		}

		IFluidTank tank = barrel.getTank();
		if (bronze > 0) {
			FluidStack stored = tank.getFluid();
			Material storedMaterial = materialForMixture(stored);
			if (stored.isEmpty() || isMaterial(storedMaterial, GTMaterials.Bronze)) {
				int filled = tank.fill(GTMaterials.Bronze.getFluid(bronze), IFluidHandler.FluidAction.EXECUTE);
				bronze -= filled;
			}
		}
		if (tank.getFluid().isEmpty() && bronze <= 0) {
			if (copper > 0 && tin <= 0) {
				int filled = tank.fill(GTMaterials.Copper.getFluid(copper), IFluidHandler.FluidAction.EXECUTE);
				copper -= filled;
			} else if (tin > 0 && copper <= 0) {
				int filled = tank.fill(GTMaterials.Tin.getFluid(tin), IFluidHandler.FluidAction.EXECUTE);
				tin -= filled;
			}
		}

		setAmountOrRemove(mixture, COPPER_KEY, copper);
		setAmountOrRemove(mixture, TIN_KEY, tin);
		setAmountOrRemove(mixture, BRONZE_KEY, bronze);
		saveMixture(barrel, data, mixture);
	}

	private static int bronzeAmountFromComponents(int copper, int tin) {
		if (copper <= 0 || tin <= 0) {
			return 0;
		}

		int amount = Math.min((copper / 3) * 4, tin * 4);
		while (amount > 0) {
			int tinDrain = bronzeTinAmount(amount);
			int copperDrain = amount - tinDrain;
			if (tinDrain <= tin && copperDrain <= copper) {
				return amount;
			}
			amount--;
		}
		return 0;
	}

	private static Material materialForMixture(FluidStack stack) {
		if (stack.isEmpty()) {
			return null;
		}

		Material material = materialForFluid(stack);
		if (isMaterial(material, GTMaterials.Copper) || isMaterial(material, GTMaterials.Tin) || isMaterial(material, GTMaterials.Bronze)) {
			return material;
		}
		return null;
	}

	private static Material materialForFluid(FluidStack stack) {
		if (stack.isEmpty()) {
			return null;
		}

		Material material = ChemicalHelper.getMaterial(stack.getFluid());
		if (material != null) {
			return material;
		}

		ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
		if (fluidId == null || !"gtceu".equals(fluidId.getNamespace())) {
			return null;
		}

		String materialName = fluidId.getPath();
		for (Material candidate : GTCEuAPI.materialManager.getRegisteredMaterials()) {
			if (materialName.equals(candidate.getName())) {
				return candidate;
			}
		}
		return null;
	}

	private static boolean isMaterial(Material material, Material expected) {
		return material != null && (material == expected || material.equals(expected));
	}

	private static boolean hasMixture(BlockEntity blockEntity) {
		CompoundTag mixture = getMixture(blockEntity);
		return mixture.getInt(COPPER_KEY) > 0 || mixture.getInt(TIN_KEY) > 0 || mixture.getInt(BRONZE_KEY) > 0;
	}

	private static CompoundTag getMixture(BlockEntity blockEntity) {
		return blockEntity.getPersistentData().getCompound(MIXTURE_TAG);
	}

	private static void addMixtureAmount(BlockEntity blockEntity, Material material, int amount) {
		if (amount <= 0) {
			return;
		}

		String key = mixtureKey(material);
		if (key == null) {
			return;
		}
		if (blockEntity instanceof BarrelBlockEntity barrel) {
			amount = Math.min(amount, mixtureRoom(barrel));
			if (amount <= 0) {
				return;
			}
		}

		CompoundTag data = blockEntity.getPersistentData();
		CompoundTag mixture = data.getCompound(MIXTURE_TAG);
		mixture.putInt(key, cappedAdd(mixture.getInt(key), amount));
		data.put(MIXTURE_TAG, mixture);
	}

	private static void drainMixture(BlockEntity blockEntity, MixtureCastResult result) {
		CompoundTag data = blockEntity.getPersistentData();
		CompoundTag mixture = data.getCompound(MIXTURE_TAG);
		decrement(mixture, COPPER_KEY, result.copperDrain());
		decrement(mixture, TIN_KEY, result.tinDrain());
		decrement(mixture, BRONZE_KEY, result.bronzeDrain());

		saveMixture(blockEntity, data, mixture);
	}

	private static void decrement(CompoundTag tag, String key, int amount) {
		if (amount <= 0) {
			return;
		}
		int remaining = Math.max(0, tag.getInt(key) - amount);
		if (remaining == 0) {
			tag.remove(key);
		} else {
			tag.putInt(key, remaining);
		}
	}

	private static String mixtureKey(Material material) {
		if (isMaterial(material, GTMaterials.Copper)) {
			return COPPER_KEY;
		}
		if (isMaterial(material, GTMaterials.Tin)) {
			return TIN_KEY;
		}
		if (isMaterial(material, GTMaterials.Bronze)) {
			return BRONZE_KEY;
		}
		return null;
	}

	private static void setAmountOrRemove(CompoundTag tag, String key, int amount) {
		if (amount <= 0) {
			tag.remove(key);
		} else {
			tag.putInt(key, amount);
		}
	}

	private static void saveMixture(BlockEntity blockEntity, CompoundTag data, CompoundTag mixture) {
		clampMixtureToTankCapacity(blockEntity, mixture);
		if (mixture.getInt(COPPER_KEY) <= 0 && mixture.getInt(TIN_KEY) <= 0 && mixture.getInt(BRONZE_KEY) <= 0) {
			data.remove(MIXTURE_TAG);
		} else {
			data.put(MIXTURE_TAG, mixture);
		}
		blockEntity.setChanged();
	}

	private static int mixtureRoom(BarrelBlockEntity barrel) {
		return Math.max(0, barrel.getTank().getCapacity() - visibleMixtureAmount(barrel) - hiddenMixtureAmount(barrel));
	}

	private static int visibleMixtureAmount(BarrelBlockEntity barrel) {
		FluidStack stored = barrel.getTank().getFluid();
		return materialForMixture(stored) == null ? 0 : stored.getAmount();
	}

	private static int hiddenMixtureAmount(BlockEntity blockEntity) {
		CompoundTag mixture = getMixture(blockEntity);
		return cappedAdd(cappedAdd(mixture.getInt(COPPER_KEY), mixture.getInt(TIN_KEY)), mixture.getInt(BRONZE_KEY));
	}

	private static void clampMixtureToTankCapacity(BlockEntity blockEntity, CompoundTag mixture) {
		if (!(blockEntity instanceof BarrelBlockEntity barrel)) {
			return;
		}

		int allowed = Math.max(0, barrel.getTank().getCapacity() - visibleMixtureAmount(barrel));
		int total = cappedAdd(cappedAdd(mixture.getInt(COPPER_KEY), mixture.getInt(TIN_KEY)), mixture.getInt(BRONZE_KEY));
		int excess = total - allowed;
		if (excess <= 0) {
			return;
		}

		excess = removeExcess(mixture, BRONZE_KEY, excess);
		excess = removeExcess(mixture, TIN_KEY, excess);
		removeExcess(mixture, COPPER_KEY, excess);
	}

	private static int removeExcess(CompoundTag mixture, String key, int excess) {
		if (excess <= 0) {
			return 0;
		}
		int amount = mixture.getInt(key);
		int removed = Math.min(amount, excess);
		setAmountOrRemove(mixture, key, amount - removed);
		return excess - removed;
	}

	private static int cappedAdd(int stored, int amount) {
		if (amount > Integer.MAX_VALUE - stored) {
			return Integer.MAX_VALUE;
		}
		return stored + amount;
	}

	private static void completeInteraction(PlayerInteractEvent.RightClickBlock event, Level level) {
		event.setUseBlock(Event.Result.DENY);
		event.setUseItem(Event.Result.DENY);
		event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
		event.setCanceled(true);
	}

	private static void consumeMold(PlayerInteractEvent.RightClickBlock event) {
		if (!event.getEntity().getAbilities().instabuild) {
			event.getItemStack().shrink(1);
		}
	}

	private static void updateBlock(Level level, BlockPos pos, BlockEntity blockEntity) {
		blockEntity.setChanged();
		level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), Block.UPDATE_CLIENTS);
	}

	private static void giveOrDrop(Player player, ItemStack stack) {
		if (!player.addItem(stack)) {
			player.drop(stack, false);
		}
	}

	private record MoldUse(MoldEntry entry, boolean consume, int materialAmountMultiplier) {
	}

	private record CastResult(ItemStack output, FluidStack toDrain) {
	}

	private record MixtureCastResult(ItemStack output, int copperDrain, int tinDrain, int bronzeDrain) {
	}
}
