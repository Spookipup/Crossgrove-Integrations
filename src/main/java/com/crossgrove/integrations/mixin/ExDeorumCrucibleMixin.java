package com.crossgrove.integrations.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thedarkcolour.exdeorum.blockentity.AbstractCrucibleBlockEntity;

@Mixin(value = AbstractCrucibleBlockEntity.class, remap = false)
public abstract class ExDeorumCrucibleMixin {

	@Inject(method = "use", at = @At("HEAD"), cancellable = true, remap = false)
	private void crossgrove$guardPartialExtract(Level level, Player player, InteractionHand hand,
												CallbackInfoReturnable<InteractionResult> cir) {
		ItemStack held = player.getItemInHand(hand);
		IFluidHandlerItem container = held.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
		if (container == null) {
			return;
		}
		if (!container.getFluidInTank(0).isEmpty()) {
			return;
		}
		IFluidHandler tank = ((AbstractCrucibleBlockEntity) (Object) this).getTank();
		FluidStack stored = tank.getFluidInTank(0);
		if (stored.isEmpty()) {
			return;
		}
		FluidStack simulated = FluidUtil.tryFluidTransfer(container, tank, Integer.MAX_VALUE, false);
		if (simulated.isEmpty()) {
			cir.setReturnValue(InteractionResult.PASS);
		}
	}
}
