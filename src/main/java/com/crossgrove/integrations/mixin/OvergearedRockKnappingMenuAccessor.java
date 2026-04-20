package com.crossgrove.integrations.mixin;

import net.minecraft.world.item.ItemStack;

import net.stirdrem.overgeared.screen.RockKnappingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = RockKnappingMenu.class, remap = false)
public interface OvergearedRockKnappingMenuAccessor {
	@Accessor("inputRock")
	ItemStack crossgrove$getInputRock();
}
