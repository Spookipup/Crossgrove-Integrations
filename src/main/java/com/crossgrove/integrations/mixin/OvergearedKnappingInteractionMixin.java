package com.crossgrove.integrations.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import net.stirdrem.overgeared.event.ModItemInteractEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ModItemInteractEvents.class, remap = false)
public abstract class OvergearedKnappingInteractionMixin {
	@Redirect(
			method = "onUsingKnappable",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
					remap = true
			),
			remap = false
	)
	private static void crossgrove$playClaySculptingOpenSound(Level level, Player soundPlayer, BlockPos pos,
															  SoundEvent sound, SoundSource source, float volume,
															  float pitch, PlayerInteractEvent.RightClickItem event) {
		Player player = event.getEntity();
		if (crossgrove$isHoldingClayForSculpting(player)) {
			level.playSound(soundPlayer, pos, SoundEvents.MUD_PLACE, source, 0.7F, 1.15F);
			return;
		}

		level.playSound(soundPlayer, pos, sound, source, volume, pitch);
	}

	private static boolean crossgrove$isHoldingClayForSculpting(Player player) {
		return crossgrove$isClayBall(player.getMainHandItem()) && crossgrove$isClayBall(player.getOffhandItem());
	}

	private static boolean crossgrove$isClayBall(ItemStack stack) {
		return stack.is(Items.CLAY_BALL);
	}
}
