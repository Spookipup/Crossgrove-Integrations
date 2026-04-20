package com.crossgrove.integrations.ceramic;

import java.util.function.Supplier;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FilledCeramicBucketItem extends Item {
	private final Supplier<Fluid> fluidSupplier;

	public FilledCeramicBucketItem(Properties props, Supplier<Fluid> fluidSupplier) {
		super(props.stacksTo(1));
		this.fluidSupplier = fluidSupplier;
	}

	public Fluid getFluid() {
		Fluid f = fluidSupplier.get();
		return f == null ? Fluids.EMPTY : f;
	}

	@Override
	@Nullable
	public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
		return new FilledHandler(stack, this);
	}

	private static final class FilledHandler implements ICapabilityProvider, IFluidHandlerItem {
		private ItemStack container;
		private final FilledCeramicBucketItem owner;
		private final LazyOptional<IFluidHandlerItem> optional = LazyOptional.of(() -> this);

		FilledHandler(ItemStack container, FilledCeramicBucketItem owner) {
			this.container = container;
			this.owner = owner;
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
			Fluid f = owner.getFluid();
			return f == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(f, CeramicBucketItem.CAPACITY);
		}

		@Override
		public int getTankCapacity(int tank) {
			return CeramicBucketItem.CAPACITY;
		}

		@Override
		public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
			return stack.getFluid() == owner.getFluid();
		}

		@Override
		public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
			return 0;
		}

		@NotNull
		@Override
		public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
			if (container.getCount() != 1 || resource.isEmpty()) return FluidStack.EMPTY;
			Fluid myFluid = owner.getFluid();
			if (myFluid == Fluids.EMPTY || resource.getFluid() != myFluid) return FluidStack.EMPTY;
			if (resource.getAmount() < CeramicBucketItem.CAPACITY) return FluidStack.EMPTY;
			return drainInternal(action);
		}

		@NotNull
		@Override
		public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
			if (container.getCount() != 1 || maxDrain < CeramicBucketItem.CAPACITY) return FluidStack.EMPTY;
			Fluid myFluid = owner.getFluid();
			if (myFluid == Fluids.EMPTY) return FluidStack.EMPTY;
			return drainInternal(action);
		}

		private FluidStack drainInternal(IFluidHandler.FluidAction action) {
			FluidStack result = new FluidStack(owner.getFluid(), CeramicBucketItem.CAPACITY);
			if (action.execute()) {
				container = new ItemStack(CeramicItems.CERAMIC_BUCKET.get());
			}
			return result;
		}
	}
}
