package com.crossgrove.integrations.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class EndPortalHandler {
    private static final Component BLOCKED_MESSAGE = Component.translatable("message.crossgrove_integrations.portal.end_blocked");

    private EndPortalHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPortalFrameUse(PlayerInteractEvent.RightClickBlock event) {
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!state.is(Blocks.END_PORTAL_FRAME) || !event.getItemStack().is(Items.ENDER_EYE)) {
            return;
        }

        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
        stripEye(event.getLevel(), event.getPos(), state);
        showBlockedMessage(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPortalFramePlaced(BlockEvent.EntityPlaceEvent event) {
        stripEye(event.getLevel(), event.getPos(), event.getPlacedBlock());
    }

    @SubscribeEvent
    public static void onPortalFrameUpdated(BlockEvent.NeighborNotifyEvent event) {
        stripEye(event.getLevel(), event.getPos(), event.getState());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEndTravel(EntityTravelToDimensionEvent event) {
        if (!Level.END.equals(event.getDimension())) {
            return;
        }

        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player) {
            showBlockedMessage(player);
        }
    }

    private static void stripEye(LevelAccessor level, BlockPos pos, BlockState state) {
        if (state.is(Blocks.END_PORTAL_FRAME) && state.getValue(EndPortalFrameBlock.HAS_EYE)) {
            level.setBlock(pos, state.setValue(EndPortalFrameBlock.HAS_EYE, false), 3);
        }
    }

    private static void showBlockedMessage(Player player) {
        if (!player.level().isClientSide()) {
            player.displayClientMessage(BLOCKED_MESSAGE, true);
        }
    }
}
