package com.hoz.dsneckfix_pdc.client;

import by.dragonsurvivalteam.dragonsurvival.client.render.ClientDragonRenderer;
import by.dragonsurvivalteam.dragonsurvival.common.capability.DragonStateProvider;
import by.dragonsurvivalteam.dragonsurvival.common.entity.DragonEntity;
import by.dragonsurvivalteam.dragonsurvival.registry.attachments.MovementData;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Reads the mount's rendered body yaw / flight tilt for dragon-on-dragon riding. */
public final class PassengerSync {
    private static final Field ANIMATION_STATE;
    private static final Method SAMPLE;
    private static final Method BODY_YAW;
    private static final Method FLIGHT_PITCH;
    private static final Method FLIGHT_ROLL;

    static {
        Field state = null;
        Method sample = null;
        Method bodyYaw = null;
        Method flightPitch = null;
        Method flightRoll = null;
        try {
            state = DragonEntity.class.getDeclaredField("animationState");
            state.setAccessible(true);
            final Class<?> stateClass = state.getType();
            sample = stateClass.getMethod("sample", float.class);
            final Class<?> poseClass = sample.getReturnType();
            bodyYaw = poseClass.getMethod("bodyYaw");
            flightPitch = poseClass.getMethod("flightPitch");
            flightRoll = poseClass.getMethod("flightRoll");
        } catch (final ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            state = null;
            sample = null;
            bodyYaw = null;
            flightPitch = null;
            flightRoll = null;
        }
        ANIMATION_STATE = state;
        SAMPLE = sample;
        BODY_YAW = bodyYaw;
        FLIGHT_PITCH = flightPitch;
        FLIGHT_ROLL = flightRoll;
    }

    private PassengerSync() {
    }

    /** Returns NaN when the player is not riding a dragon player. */
    public static double rootBodyYaw(final Player rider, final float partialTick) {
        final Player root = rootDragonMount(rider);
        if (root == null) {
            return Double.NaN;
        }

        final DragonEntity rootDragon = ClientDragonRenderer.getDragon(root);
        if (rootDragon != null && BODY_YAW != null) {
            final Object pose = sample(rootDragon, partialTick);
            if (pose != null) {
                try {
                    return ((Number) BODY_YAW.invoke(pose)).doubleValue();
                } catch (final ReflectiveOperationException | RuntimeException ignored) {
                    // Fall back to the tick value.
                }
            }
        }

        return MovementData.getData(root).bodyYaw;
    }

    /** Returns {flightPitch, flightRoll} for 2.0.72+, or null on older versions. */
    public static float[] sampleFlight(final DragonEntity dragon, final float partialTick) {
        if (FLIGHT_PITCH == null || FLIGHT_ROLL == null) {
            return null;
        }
        final Object pose = sample(dragon, partialTick);
        if (pose == null) {
            return null;
        }
        try {
            return new float[]{
                    ((Number) FLIGHT_PITCH.invoke(pose)).floatValue(),
                    ((Number) FLIGHT_ROLL.invoke(pose)).floatValue()
            };
        } catch (final ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Object sample(final DragonEntity dragon, final float partialTick) {
        if (ANIMATION_STATE == null || SAMPLE == null) {
            return null;
        }
        try {
            final Object state = ANIMATION_STATE.get(dragon);
            return state == null ? null : SAMPLE.invoke(state, partialTick);
        } catch (final ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Player rootDragonMount(final Player player) {
        if (player == null || !player.isPassenger()) {
            return null;
        }
        if (!(player.getVehicle() instanceof Player vehicle) || !DragonStateProvider.isDragon(vehicle)) {
            return null;
        }
        Player root = vehicle;
        while (root.getVehicle() instanceof Player parent && DragonStateProvider.isDragon(parent)) {
            root = parent;
        }
        return root;
    }
}
