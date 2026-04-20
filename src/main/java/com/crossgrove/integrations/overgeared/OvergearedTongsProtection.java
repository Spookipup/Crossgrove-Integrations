package com.crossgrove.integrations.overgeared;

import java.util.HashMap;
import java.util.UUID;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class OvergearedTongsProtection {
	private static final TagKey<Item> TONGS = itemTag("overgeared", "tongs");
	private static final TagKey<Item> HEATED_METALS = itemTag("overgeared", "heated_metals");
	private static final TagKey<Item> HOT_ITEMS = itemTag("overgeared", "hot_items");
	private static final String HEATED_TAG = "Heated";
	private static final HashMap<UUID, Long> LAST_TONGS_DAMAGE = new HashMap<>();

	private OvergearedTongsProtection() {
	}

	public static boolean tryProtect(Player player) {
		if (player == null || player.level().isClientSide || !hasHotItem(player)) {
			return false;
		}

		ItemStack tongs = findTongs(player);
		if (tongs.isEmpty()) {
			return false;
		}

		if (!isHeld(player, tongs)) {
			damageTongs(player, tongs);
		}
		return true;
	}

	public static boolean hasHotItem(Player player) {
		for (ItemStack stack : player.getInventory().items) {
			if (isHotItem(stack)) {
				return true;
			}
		}
		return isHotItem(player.getOffhandItem());
	}

	private static ItemStack findTongs(Player player) {
		ItemStack mainHand = player.getMainHandItem();
		if (isTongs(mainHand)) {
			return mainHand;
		}

		ItemStack offHand = player.getOffhandItem();
		if (isTongs(offHand)) {
			return offHand;
		}

		for (ItemStack stack : player.getInventory().items) {
			if (isTongs(stack)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static boolean isTongs(ItemStack stack) {
		return !stack.isEmpty() && stack.is(TONGS);
	}

	private static boolean isHotItem(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		if (stack.is(HEATED_METALS) || stack.is(HOT_ITEMS)) {
			return true;
		}
		return stack.hasTag() && stack.getTag().contains(HEATED_TAG);
	}

	private static boolean isHeld(Player player, ItemStack stack) {
		return stack == player.getMainHandItem() || stack == player.getOffhandItem();
	}

	private static void damageTongs(Player player, ItemStack tongs) {
		long gameTime = player.level().getGameTime();
		if (gameTime % 40L != 0L) {
			return;
		}

		UUID playerId = player.getUUID();
		if (LAST_TONGS_DAMAGE.getOrDefault(playerId, -1L) == gameTime) {
			return;
		}

		tongs.hurtAndBreak(1, player, owner -> {
			if (tongs == owner.getOffhandItem()) {
				owner.broadcastBreakEvent(InteractionHand.OFF_HAND);
			} else if (tongs == owner.getMainHandItem()) {
				owner.broadcastBreakEvent(InteractionHand.MAIN_HAND);
			} else {
				owner.broadcastBreakEvent(EquipmentSlot.MAINHAND);
			}
		});
		LAST_TONGS_DAMAGE.put(playerId, gameTime);
	}

	private static TagKey<Item> itemTag(String namespace, String path) {
		return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path));
	}
}
