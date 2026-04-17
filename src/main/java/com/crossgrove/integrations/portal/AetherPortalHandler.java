package com.crossgrove.integrations.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

public final class AetherPortalHandler {
    private static final ResourceLocation AETHER_DIMENSION_ID = Objects.requireNonNull(
            ResourceLocation.tryParse("aether:the_aether")
    );
    private static final ResourceKey<Level> AETHER_DIMENSION = ResourceKey.create(Registries.DIMENSION, AETHER_DIMENSION_ID);
    private static final ResourceLocation ACTIVATOR_ID = Objects.requireNonNull(
            ResourceLocation.tryParse("aether:aether_portal_activation_items")
    );
    private static final TagKey<Item> ACTIVATORS = TagKey.create(Registries.ITEM, ACTIVATOR_ID);
    private static final Component BLOCKED_MESSAGE = Component.translatable("message.crossgrove_integrations.portal.aether_blocked");
    private static final String PORTAL_SHAPE_CLASS = "com.aetherteam.aether.block.portal.AetherPortalShape";

    private static Method findEmptyAetherPortalShape;

    private AetherPortalHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPortalFrameUse(PlayerInteractEvent.RightClickBlock event) {
        Direction face = event.getFace();
        if (face == null || !isPortalActivator(event)) {
            return;
        }

        if (!canCreateAetherPortal(event.getLevel(), event.getPos().relative(face))) {
            return;
        }

        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
        showBlockedMessage(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onWaterPortalAttempt(BlockEvent.NeighborNotifyEvent event) {
        if (!event.getLevel().getFluidState(event.getPos()).is(FluidTags.WATER)) {
            return;
        }

        if (canCreateAetherPortal(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAetherTravel(EntityTravelToDimensionEvent event) {
        if (!AETHER_DIMENSION.equals(event.getDimension())) {
            return;
        }

        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player) {
            showBlockedMessage(player);
        }
    }

    private static boolean canCreateAetherPortal(LevelAccessor level, BlockPos pos) {
        Method method = getFindEmptyAetherPortalShape();
        if (method == null) {
            return false;
        }

        try {
            Object result = method.invoke(null, level, pos, Direction.Axis.X);
            return result instanceof Optional<?> optional && optional.isPresent();
        } catch (IllegalAccessException | InvocationTargetException | LinkageError exception) {
            return false;
        }
    }

    private static Method getFindEmptyAetherPortalShape() {
        if (findEmptyAetherPortalShape != null) {
            return findEmptyAetherPortalShape;
        }

        try {
            Class<?> portalShapeClass = Class.forName(PORTAL_SHAPE_CLASS);
            findEmptyAetherPortalShape = portalShapeClass.getMethod(
                    "findEmptyAetherPortalShape",
                    LevelAccessor.class,
                    BlockPos.class,
                    Direction.Axis.class
            );
            return findEmptyAetherPortalShape;
        } catch (ClassNotFoundException | NoSuchMethodException | LinkageError exception) {
            return null;
        }
    }

    private static boolean isPortalActivator(PlayerInteractEvent.RightClickBlock event) {
        return event.getItemStack().is(ACTIVATORS) || event.getItemStack().is(Items.WATER_BUCKET);
    }

    private static void showBlockedMessage(Player player) {
        if (!player.level().isClientSide()) {
            player.displayClientMessage(BLOCKED_MESSAGE, true);
        }
    }
}
