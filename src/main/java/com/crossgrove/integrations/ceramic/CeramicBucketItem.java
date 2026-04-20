package com.crossgrove.integrations.ceramic;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CeramicBucketItem extends Item {
	public static final int CAPACITY = 432;

	public CeramicBucketItem(Properties props) {
		super(props.stacksTo(16));
	}

	@Override
	@Nullable
	public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
		return new EmptyHandler(stack);
	}

	private static final class EmptyHandler implements ICapabilityProvider, IFluidHandlerItem {
		private ItemStack container;
		private final LazyOptional<IFluidHandlerItem> optional = LazyOptional.of(() -> this);

		EmptyHandler(ItemStack container) {
			this.container = container;
		}

		@NotNull
		@Override
		public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
			return cap == ForgeCapabilities.FLUID_HANDLER_ITEM ? optional.cast() : LazyOptional.empty();
		}

		@Override
		public ItemStack getContainer() {
			return container;
		}

		@Override
		public int getTanks() {
			return 1;
		}

		@Override
		public FluidStack getFluidInTank(int tank) {
			return FluidStack.EMPTY;
		}

		@Override
		public int getTankCapacity(int tank) {
			return CAPACITY;
		}

		@Override
		public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
			return CeramicItems.getFilledBucketFor(stack.getFluid()) != null;
		}

		@Override
		public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
			if (container.getCount() != 1 || resource.isEmpty() || resource.getAmount() < CAPACITY) {
				return 0;
			}
			Fluid fluid = resource.getFluid();
			Item filled = CeramicItems.getFilledBucketFor(fluid);
			if (filled == null) {
				return 0;
			}
			if (action.execute()) {
				container = new ItemStack(filled);
			}
			return CAPACITY;
		}

		@NotNull
		@Override
		public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
			return FluidStack.EMPTY;
		}

		@NotNull
		@Override
		public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
			return FluidStack.EMPTY;
		}
	}
}
