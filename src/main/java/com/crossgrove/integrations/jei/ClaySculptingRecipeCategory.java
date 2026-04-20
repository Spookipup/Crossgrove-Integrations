package com.crossgrove.integrations.jei;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.stirdrem.overgeared.recipe.RockKnappingRecipe;

import com.crossgrove.integrations.CrossgroveIntegrations;

public final class ClaySculptingRecipeCategory implements IRecipeCategory<RockKnappingRecipe> {
	public static final RecipeType<RockKnappingRecipe> TYPE = RecipeType.create(
			CrossgroveIntegrations.MOD_ID,
			"clay_sculpting",
			RockKnappingRecipe.class
	);

	private static final ResourceLocation BACKGROUND_TEXTURE =
			ResourceLocation.fromNamespaceAndPath("overgeared", "textures/gui/rock_knapping_jei.png");
	private static final ResourceLocation CLAY_TEXTURE =
			ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/clay.png");
	private static final int GRID_X = 25;
	private static final int GRID_Y = 3;
	private static final int SLOT_SIZE = 16;
	private static final int GRID_SIZE = SLOT_SIZE * 3;
	private static final int BORDER_SIZE = 3;
	private static final int SELECTED_SHADE = 0xAA000000;

	private final IDrawable background;
	private final IDrawable icon;

	public ClaySculptingRecipeCategory(IGuiHelper helper) {
		this.background = helper.createDrawable(BACKGROUND_TEXTURE, 7, 16, 138, 54);
		this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.CLAY_BALL));
	}

	@Override
	public RecipeType<RockKnappingRecipe> getRecipeType() {
		return TYPE;
	}

	@Override
	public Component getTitle() {
		return Component.translatable("gui.crossgrove_integrations.sculpting");
	}

	@Override
	public IDrawable getBackground() {
		return this.background;
	}

	@Override
	public IDrawable getIcon() {
		return this.icon;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, RockKnappingRecipe recipe, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.INPUT, 1, 19)
				.addIngredients(recipe.getIngredient());
		builder.addSlot(RecipeIngredientRole.OUTPUT, 117, 19)
				.addItemStack(recipe.getResultItem(null));
	}

	@Override
	public void draw(RockKnappingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
					 double mouseX, double mouseY) {
		drawClayBorder(graphics);

		boolean[][] pattern = recipe.getPattern();
		int height = pattern.length;
		int width = height > 0 ? pattern[0].length : 0;

		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 3; column++) {
				int x = GRID_X + column * SLOT_SIZE;
				int y = GRID_Y + row * SLOT_SIZE;
				graphics.blit(CLAY_TEXTURE, x, y, 0.0F, 0.0F, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);

				boolean selected = row >= height || column >= width || !pattern[row][column];
				if (selected) {
					graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SELECTED_SHADE);
				}
			}
		}
	}

	public static boolean isClaySculptingRecipe(RockKnappingRecipe recipe) {
		return recipe.getIngredient().test(new ItemStack(Items.CLAY_BALL));
	}

	private static void drawClayBorder(GuiGraphics graphics) {
		int x = GRID_X - BORDER_SIZE;
		int y = GRID_Y - BORDER_SIZE;
		int size = GRID_SIZE + BORDER_SIZE * 2;

		blitClayStrip(graphics, x, y, size, BORDER_SIZE);
		blitClayStrip(graphics, x, y + size - BORDER_SIZE, size, BORDER_SIZE);
		blitClayStrip(graphics, x, y + BORDER_SIZE, BORDER_SIZE, GRID_SIZE);
		blitClayStrip(graphics, x + size - BORDER_SIZE, y + BORDER_SIZE, BORDER_SIZE, GRID_SIZE);
	}

	private static void blitClayStrip(GuiGraphics graphics, int x, int y, int width, int height) {
		for (int currentX = 0; currentX < width; currentX += SLOT_SIZE) {
			int tileWidth = Math.min(SLOT_SIZE, width - currentX);
			for (int currentY = 0; currentY < height; currentY += SLOT_SIZE) {
				int tileHeight = Math.min(SLOT_SIZE, height - currentY);
				graphics.blit(CLAY_TEXTURE, x + currentX, y + currentY,
						0.0F, 0.0F, tileWidth, tileHeight, SLOT_SIZE, SLOT_SIZE);
			}
		}
	}
}
