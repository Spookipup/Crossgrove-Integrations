package com.crossgrove.integrations.gtceu.overgeared;

import net.minecraft.world.item.ItemStack;

import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.item.tool.ToolHelper;
import net.stirdrem.overgeared.util.ModTags;

public final class OvergearedHammerCompat {
	private static final ThreadLocal<GTToolType> ACTIVE_TOOL_TYPE = new ThreadLocal<>();

	private OvergearedHammerCompat() {
	}

	public static boolean isSmithingHammer(ItemStack stack) {
		return !stack.isEmpty()
				&& (stack.is(ModTags.Items.SMITHING_HAMMERS) || ToolHelper.is(stack, GTToolType.HARD_HAMMER));
	}

	public static boolean isForgeActionTool(ItemStack stack) {
		GTToolType toolType = toolTypeFor(stack);
		ACTIVE_TOOL_TYPE.set(toolType);
		return toolType != null;
	}

	public static boolean isUsableForgingTool(ItemStack stack) {
		return toolTypeFor(stack) != null;
	}

	public static GTToolType activeToolType() {
		return ACTIVE_TOOL_TYPE.get();
	}

	public static void captureActiveTool(ItemStack stack) {
		ACTIVE_TOOL_TYPE.set(toolTypeFor(stack));
	}

	public static void clearActiveTool() {
		ACTIVE_TOOL_TYPE.remove();
	}

	private static GTToolType toolTypeFor(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		if (isSmithingHammer(stack)) {
			return GTToolType.HARD_HAMMER;
		}
		if (ToolHelper.is(stack, GTToolType.FILE)) {
			return GTToolType.FILE;
		}
		if (ToolHelper.is(stack, GTToolType.SAW)) {
			return GTToolType.SAW;
		}
		if (ToolHelper.is(stack, GTToolType.SCYTHE)) {
			return GTToolType.SCYTHE;
		}
		return null;
	}
}
