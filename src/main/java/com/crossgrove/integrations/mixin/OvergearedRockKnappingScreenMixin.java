package com.crossgrove.integrations.mixin;

import java.util.Set;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.stirdrem.overgeared.networking.ModMessages;
import net.stirdrem.overgeared.networking.packet.KnappingChipC2SPacket;
import net.stirdrem.overgeared.screen.RockKnappingMenu;
import net.stirdrem.overgeared.screen.RockKnappingScreen;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RockKnappingScreen.class, remap = false)
public abstract class OvergearedRockKnappingScreenMixin extends AbstractContainerScreen<RockKnappingMenu> {
	private static final ResourceLocation CROSSGROVE_CLAY_KNAPPING_TEXTURE =
			ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/clay.png");
	private static final int CROSSGROVE_GRID_ORIGIN_X = 32;
	private static final int CROSSGROVE_GRID_ORIGIN_Y = 19;
	private static final int CROSSGROVE_SLOT_SIZE = 16;
	private static final int CROSSGROVE_GRID_SIZE = CROSSGROVE_SLOT_SIZE * 3;
	private static final int CROSSGROVE_CLAY_BORDER_SIZE = 3;
	private static final int CROSSGROVE_CHIPPED_CLAY_SHADE = 0xAA000000;
	private static final int CROSSGROVE_CHIPPED_CLAY_Z = 400;
	private static final Component CROSSGROVE_SCULPTING_TITLE =
			Component.translatable("gui.crossgrove_integrations.sculpting");

	@Shadow
	@Final
	private Set<Integer> chippedSpots;

	protected OvergearedRockKnappingScreenMixin(RockKnappingMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Shadow
	private void addKnappingButtons() {
	}

	@Dynamic("Runtime Overgeared jars use obfuscated names for Minecraft screen overrides.")
	@Inject(method = {"init()V", "m_7856_()V"}, at = @At("TAIL"), remap = false)
	private void crossgrove$centerClaySculptingTitle(CallbackInfo ci) {
		if (!crossgrove$isClayKnapping()) {
			return;
		}

		this.titleLabelX = (this.imageWidth - this.font.width(CROSSGROVE_SCULPTING_TITLE)) / 2;
	}

	@Dynamic("Runtime Overgeared jars use obfuscated names for Minecraft screen overrides.")
	@Inject(
			method = {
					"renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V",
					"m_7286_(Lnet/minecraft/client/gui/GuiGraphics;FII)V"
			},
			at = @At("RETURN"),
			remap = false
	)
	private void crossgrove$renderClayGridBorder(GuiGraphics graphics, float partialTick, int mouseX, int mouseY,
												 CallbackInfo ci) {
		if (!crossgrove$isClayKnapping()) {
			return;
		}

		int x = this.leftPos + CROSSGROVE_GRID_ORIGIN_X - CROSSGROVE_CLAY_BORDER_SIZE;
		int y = this.topPos + CROSSGROVE_GRID_ORIGIN_Y - CROSSGROVE_CLAY_BORDER_SIZE;
		int size = CROSSGROVE_GRID_SIZE + CROSSGROVE_CLAY_BORDER_SIZE * 2;

		crossgrove$blitClayStrip(graphics, x, y, size, CROSSGROVE_CLAY_BORDER_SIZE);
		crossgrove$blitClayStrip(graphics, x, y + size - CROSSGROVE_CLAY_BORDER_SIZE,
				size, CROSSGROVE_CLAY_BORDER_SIZE);
		crossgrove$blitClayStrip(graphics, x, y + CROSSGROVE_CLAY_BORDER_SIZE,
				CROSSGROVE_CLAY_BORDER_SIZE, CROSSGROVE_GRID_SIZE);
		crossgrove$blitClayStrip(graphics, x + size - CROSSGROVE_CLAY_BORDER_SIZE,
				y + CROSSGROVE_CLAY_BORDER_SIZE, CROSSGROVE_CLAY_BORDER_SIZE, CROSSGROVE_GRID_SIZE);
	}

	@Dynamic("Runtime Overgeared jars use obfuscated names for Minecraft screen overrides.")
	@Inject(
			method = {
					"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
					"m_88315_(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
			},
			at = @At("TAIL"),
			remap = false
	)
	private void crossgrove$renderDarkenedClayChips(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
													CallbackInfo ci) {
		if (!crossgrove$isClayKnapping()) {
			return;
		}

		for (int index = 0; index < 9; index++) {
			if (!crossgrove$isClayChipSelected(index)) {
				continue;
			}

			int x = this.leftPos + CROSSGROVE_GRID_ORIGIN_X + index % 3 * CROSSGROVE_SLOT_SIZE;
			int y = this.topPos + CROSSGROVE_GRID_ORIGIN_Y + index / 3 * CROSSGROVE_SLOT_SIZE;
			graphics.fill(x, y, x + CROSSGROVE_SLOT_SIZE, y + CROSSGROVE_SLOT_SIZE,
					CROSSGROVE_CHIPPED_CLAY_Z, CROSSGROVE_CHIPPED_CLAY_SHADE);
		}
	}

	@Dynamic("Runtime Overgeared jars use obfuscated names for Minecraft screen overrides.")
	@Inject(
			method = {
					"renderLabels(Lnet/minecraft/client/gui/GuiGraphics;II)V",
					"m_280003_(Lnet/minecraft/client/gui/GuiGraphics;II)V"
			},
			at = @At("HEAD"),
			cancellable = true,
			remap = false
	)
	private void crossgrove$renderClaySculptingLabels(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
		if (!crossgrove$isClayKnapping()) {
			return;
		}

		graphics.drawString(this.font, CROSSGROVE_SCULPTING_TITLE, this.titleLabelX, this.titleLabelY,
				4210752, false);
		graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 4210752, false);
		ci.cancel();
	}

	@Redirect(
			method = "addKnappingButtons",
			at = @At(
					value = "INVOKE",
					target = "Lnet/stirdrem/overgeared/screen/RockKnappingMenu;isChipped(I)Z"
			),
			remap = false
	)
	private boolean crossgrove$keepClayButtonTexturePresent(RockKnappingMenu menu, int index) {
		if (crossgrove$isClayKnapping()) {
			return false;
		}
		return menu.isChipped(index);
	}

	@Inject(method = "lambda$addKnappingButtons$0", at = @At("HEAD"), cancellable = true, remap = false)
	private void crossgrove$toggleClayChip(boolean hasResult, boolean canEditResult, boolean wasChippedWhenAdded,
										   int index, boolean resultCollected, Button button, CallbackInfo ci) {
		if (!crossgrove$isClayKnapping()) {
			return;
		}

		ci.cancel();
		if (this.menu.isKnappingFinished() || resultCollected || hasResult && !canEditResult) {
			return;
		}

		boolean wasChipped = this.menu.isChipped(index);
		this.menu.setChip(index);
		if (wasChipped) {
			this.chippedSpots.remove(index);
		} else {
			this.chippedSpots.add(index);
		}

		ModMessages.sendToServer(new KnappingChipC2SPacket(index));
		if (this.minecraft != null && this.minecraft.player != null) {
			this.minecraft.player.playSound(this.menu.getSound(), 1.0F, 1.0F);
		}
		addKnappingButtons();
	}

	private boolean crossgrove$isClayKnapping() {
		ItemStack inputRock = ((OvergearedRockKnappingMenuAccessor) this.menu).crossgrove$getInputRock();
		return inputRock.is(Items.CLAY_BALL) || CROSSGROVE_CLAY_KNAPPING_TEXTURE.equals(this.menu.getUnchippedTexture());
	}

	private boolean crossgrove$isClayChipSelected(int index) {
		return this.menu.isChipped(index) || this.chippedSpots.contains(index);
	}

	private void crossgrove$blitClayStrip(GuiGraphics graphics, int x, int y, int width, int height) {
		for (int currentX = 0; currentX < width; currentX += CROSSGROVE_SLOT_SIZE) {
			int tileWidth = Math.min(CROSSGROVE_SLOT_SIZE, width - currentX);
			for (int currentY = 0; currentY < height; currentY += CROSSGROVE_SLOT_SIZE) {
				int tileHeight = Math.min(CROSSGROVE_SLOT_SIZE, height - currentY);
				graphics.blit(CROSSGROVE_CLAY_KNAPPING_TEXTURE, x + currentX, y + currentY,
						0.0F, 0.0F, tileWidth, tileHeight, CROSSGROVE_SLOT_SIZE, CROSSGROVE_SLOT_SIZE);
			}
		}
	}
}
