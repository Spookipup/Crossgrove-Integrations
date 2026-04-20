package com.crossgrove.integrations.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.crossgrove.integrations.overgeared.OvergearedTongsProtection;

@Mixin(Player.class)
public abstract class OvergearedHotItemDamageMixin {
	private static final String HOT_FLOOR_DAMAGE = "hotFloor";

	@Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
	private void crossgrove$protectCarriedHotItemsWithTongs(DamageSource source, float amount,
															CallbackInfoReturnable<Boolean> cir) {
		if (!HOT_FLOOR_DAMAGE.equals(source.getMsgId())) {
			return;
		}

		Player player = (Player) (Object) this;
		if (OvergearedTongsProtection.tryProtect(player)) {
			cir.setReturnValue(false);
		}
	}
}
