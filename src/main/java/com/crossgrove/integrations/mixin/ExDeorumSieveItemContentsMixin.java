package com.crossgrove.integrations.mixin;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thedarkcolour.exdeorum.blockentity.EBlockEntity;
import thedarkcolour.exdeorum.blockentity.logic.SieveLogic;
import thedarkcolour.exdeorum.client.RenderUtil;
import thedarkcolour.exdeorum.client.ter.SieveRenderer;

@Mixin(value = SieveRenderer.class, remap = false)
public abstract class ExDeorumSieveItemContentsMixin {
	@Shadow
	@Final
	private float contentsMinY;

	@Shadow
	@Final
	private float contentsMaxY;

	@Inject(method = "render", at = @At("HEAD"), remap = false)
	private void crossgrove$renderItemContents(EBlockEntity blockEntity, float partialTick, PoseStack poseStack,
											   MultiBufferSource bufferSource, int packedLight, int packedOverlay,
											   CallbackInfo ci) {
		if (!(blockEntity instanceof SieveLogic.Owner owner)) {
			return;
		}

		SieveLogic logic = owner.getLogic();
		ItemStack contents = logic.getContents();
		if (contents.isEmpty() || contents.getItem() instanceof BlockItem) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		BakedModel model = minecraft.getItemRenderer().getModel(contents, blockEntity.getLevel(), null, 0);
		List<TextureAtlasSprite> sprites = new ArrayList<>();
		List<Integer> colors = new ArrayList<>();

		crossgrove$collectItemLayers(minecraft, model, contents, sprites, colors);
		if (sprites.isEmpty()) {
			TextureAtlasSprite fallback = model.getParticleIcon();
			if (!RenderUtil.isMissingTexture(fallback)) {
				crossgrove$addLayer(sprites, colors, fallback, 0xFFFFFF);
			}
		}

		VertexConsumer consumer = bufferSource.getBuffer(RenderType.translucent());
		for (int i = 0; i < sprites.size(); i++) {
			int color = colors.get(i);
			RenderUtil.renderFlatSpriteLerp(
					consumer,
					poseStack,
					logic.getProgress(),
					color >> 16 & 255,
					color >> 8 & 255,
					color & 255,
					sprites.get(i),
					packedLight,
					1.0F,
					contentsMaxY + i * 0.001F,
					contentsMinY + i * 0.001F
			);
		}
	}

	@Unique
	private static void crossgrove$collectItemLayers(Minecraft minecraft, BakedModel model, ItemStack stack,
													 List<TextureAtlasSprite> sprites, List<Integer> colors) {
		crossgrove$collectItemLayers(minecraft, model.getQuads(null, null, RandomSource.create(42L)), stack, sprites, colors);
		for (Direction direction : Direction.values()) {
			crossgrove$collectItemLayers(
					minecraft,
					model.getQuads(null, direction, RandomSource.create(42L)),
					stack,
					sprites,
					colors
			);
		}
	}

	@Unique
	private static void crossgrove$collectItemLayers(Minecraft minecraft, List<BakedQuad> quads, ItemStack stack,
													 List<TextureAtlasSprite> sprites, List<Integer> colors) {
		for (BakedQuad quad : quads) {
			Direction direction = quad.getDirection();
			if (direction != Direction.NORTH && direction != Direction.SOUTH) {
				continue;
			}

			TextureAtlasSprite sprite = quad.getSprite();
			if (RenderUtil.isMissingTexture(sprite)) {
				continue;
			}

			int color = quad.isTinted() ? minecraft.getItemColors().getColor(stack, quad.getTintIndex()) : 0xFFFFFF;
			crossgrove$addLayer(sprites, colors, sprite, color & 0xFFFFFF);
		}
	}

	@Unique
	private static void crossgrove$addLayer(List<TextureAtlasSprite> sprites, List<Integer> colors,
											TextureAtlasSprite sprite, int color) {
		for (int i = 0; i < sprites.size(); i++) {
			if (sprites.get(i) == sprite && colors.get(i) == color) {
				return;
			}
		}

		sprites.add(sprite);
		colors.add(color);
	}
}
