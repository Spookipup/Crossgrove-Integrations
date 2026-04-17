package com.crossgrove.integrations.portal;

import com.Da_Technomancer.crossroads.effects.beam_effects.ChargeEffect;
import com.Da_Technomancer.crossroads.effects.beam_effects.EnergizeEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class NetherPortalHandler {
    private static final String HEAT_BEAM_EFFECT = EnergizeEffect.class.getName();
    private static final String CHARGE_BEAM_EFFECT = ChargeEffect.class.getName();
    private static final long CHARGE_LIGHT_TICKS = 40L;
    private static final double CHARGE_LIGHT_RANGE_SQUARED = 16D;
    private static final List<AuthorizedLight> AUTHORIZED_CHARGE_LIGHTS = new ArrayList<>();

    private NetherPortalHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPortalSpawn(BlockEvent.PortalSpawnEvent event) {
        if (isBeamStack() || isRecentlyCharged(event)) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLightningCreated(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof LightningBolt lightning)) {
            return;
        }

        if (isStackFrom(CHARGE_BEAM_EFFECT)) {
            Level level = event.getLevel();
            AUTHORIZED_CHARGE_LIGHTS.add(new AuthorizedLight(
                    level.dimension(),
                    lightning.blockPosition().immutable(),
                    level.getGameTime() + CHARGE_LIGHT_TICKS
            ));
        }
    }

    private static boolean isBeamStack() {
        return isStackFrom(HEAT_BEAM_EFFECT) || isStackFrom(CHARGE_BEAM_EFFECT);
    }

    private static boolean isStackFrom(String className) {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (className.equals(element.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRecentlyCharged(BlockEvent.PortalSpawnEvent event) {
        if (!(event.getLevel() instanceof Level level)) {
            return false;
        }

        ResourceKey<Level> dimension = level.dimension();
        BlockPos pos = event.getPos();
        long gameTime = level.getGameTime();
        boolean matched = false;
        Iterator<AuthorizedLight> iterator = AUTHORIZED_CHARGE_LIGHTS.iterator();
        while (iterator.hasNext()) {
            AuthorizedLight light = iterator.next();
            if (light.expiresAt < gameTime) {
                iterator.remove();
                continue;
            }
            if (light.dimension.equals(dimension) && light.pos.distSqr(pos) <= CHARGE_LIGHT_RANGE_SQUARED) {
                matched = true;
            }
        }
        return matched;
    }

    private record AuthorizedLight(ResourceKey<Level> dimension, BlockPos pos, long expiresAt) {
    }
}
