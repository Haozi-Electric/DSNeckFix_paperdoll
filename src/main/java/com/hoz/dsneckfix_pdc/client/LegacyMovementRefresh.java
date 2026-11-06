package com.hoz.dsneckfix_pdc.client;

import by.dragonsurvivalteam.dragonsurvival.client.render.ClientDragonRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;

/**
 * DS 2.1.7 moved movement updates to the client tick and removed the old
 * {@code setDragonMovementData(Player, float)} overload. Refreshing is only needed when the
 * running DragonSurvival still exposes it.
 */
public final class LegacyMovementRefresh {
    private static final Method SET_MOVEMENT = find("setDragonMovementData", Player.class, float.class);

    private LegacyMovementRefresh() {
    }

    public static void refresh(final Player player) {
        if (SET_MOVEMENT == null) {
            return;
        }
        try {
            SET_MOVEMENT.invoke(null, player, Minecraft.getInstance().getDeltaTracker().getRealtimeDeltaTicks());
        } catch (final ReflectiveOperationException | RuntimeException ignored) {
            // Older DS versions only; failure leaves the tick-updated movement untouched.
        }
    }

    private static Method find(final String name, final Class<?>... parameters) {
        try {
            final Method method = ClientDragonRenderer.class.getDeclaredMethod(name, parameters);
            method.setAccessible(true);
            return method;
        } catch (final NoSuchMethodException | RuntimeException ignored) {
            return null;
        }
    }
}
