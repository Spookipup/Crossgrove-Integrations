package com.crossgrove.integrations.client;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import com.crossgrove.integrations.CrossgroveIntegrations;
import com.crossgrove.integrations.ceramic.CeramicItems;

@Mod.EventBusSubscriber(modid = CrossgroveIntegrations.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CrossgroveClient {
	private CrossgroveClient() {
	}

	@SubscribeEvent
	public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
		Item[] filledBuckets = CeramicItems.filledVariants().stream()
				.map(RegistryObject::get)
				.toArray(Item[]::new);
		event.register(CrossgroveClient::fluidColor, filledBuckets);
	}

	private static int fluidColor(ItemStack stack, int tintIndex) {
		if (tintIndex != 1) return 0xFFFFFFFF;
		return FluidUtil.getFluidContained(stack)
				.map(fluidStack -> IClientFluidTypeExtensions.of(fluidStack.getFluid()).getTintColor(fluidStack))
				.orElse(0xFFFFFFFF);
	}
}
