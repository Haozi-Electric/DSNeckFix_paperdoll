package com.hoz.dsneckfix_pdc.client;

import by.dragonsurvivalteam.dragonsurvival.registry.attachments.MovementData;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;

/** DragonSurvival 2.0.72 renamed the previous-tick movement fields from *LastFrame to *LastTick. */
public final class MovementDataCompat {
    private static final Field HEAD_YAW_LAST = resolve("headYawLastFrame", "headYawLastTick");
    private static final Field HEAD_PITCH_LAST = resolve("headPitchLastFrame", "headPitchLastTick");
    private static final Field BODY_YAW_LAST = resolve("bodyYawLastFrame", "bodyYawLastTick");
    private static final Field DELTA_MOVEMENT_LAST = resolve("deltaMovementLastFrame", "deltaMovementLastTick");

    private MovementDataCompat() {
    }

    public static Snapshot snapshot(final MovementData movement) {
        return new Snapshot(
                movement.bodyYaw, getDouble(BODY_YAW_LAST, movement),
                movement.headYaw, getDouble(HEAD_YAW_LAST, movement),
                movement.headPitch, getDouble(HEAD_PITCH_LAST, movement),
                movement.deltaMovement, getVec(DELTA_MOVEMENT_LAST, movement));
    }

    public static void restore(final MovementData movement, final Snapshot snapshot) {
        movement.bodyYaw = snapshot.bodyYaw();
        movement.headYaw = snapshot.headYaw();
        movement.headPitch = snapshot.headPitch();
        movement.deltaMovement = snapshot.deltaMovement();
        setDouble(BODY_YAW_LAST, movement, snapshot.bodyYawLast());
        setDouble(HEAD_YAW_LAST, movement, snapshot.headYawLast());
        setDouble(HEAD_PITCH_LAST, movement, snapshot.headPitchLast());
        setVec(DELTA_MOVEMENT_LAST, movement, snapshot.deltaMovementLast());
    }

    public static double getBodyYawLast(final MovementData movement) {
        return getDouble(BODY_YAW_LAST, movement);
    }

    public static void setBodyYawLast(final MovementData movement, final double value) {
        setDouble(BODY_YAW_LAST, movement, value);
    }

    public static void setHeadYawLast(final MovementData movement, final double value) {
        setDouble(HEAD_YAW_LAST, movement, value);
    }

    public static void setHeadPitchLast(final MovementData movement, final double value) {
        setDouble(HEAD_PITCH_LAST, movement, value);
    }

    private static Field resolve(final String... names) {
        for (final String name : names) {
            try {
                return MovementData.class.getField(name);
            } catch (final NoSuchFieldException ignored) {
                // Try the next naming scheme.
            }
        }
        return null;
    }

    private static double getDouble(final Field field, final MovementData movement) {
        if (field == null) {
            return 0;
        }
        try {
            return field.getDouble(movement);
        } catch (final IllegalAccessException ignored) {
            return 0;
        }
    }

    private static void setDouble(final Field field, final MovementData movement, final double value) {
        if (field == null) {
            return;
        }
        try {
            field.setDouble(movement, value);
        } catch (final IllegalAccessException ignored) {
            // The fields are public; a failure leaves the value untouched.
        }
    }

    private static Vec3 getVec(final Field field, final MovementData movement) {
        if (field == null) {
            return Vec3.ZERO;
        }
        try {
            return (Vec3) field.get(movement);
        } catch (final IllegalAccessException ignored) {
            return Vec3.ZERO;
        }
    }

    private static void setVec(final Field field, final MovementData movement, final Vec3 value) {
        if (field == null) {
            return;
        }
        try {
            field.set(movement, value);
        } catch (final IllegalAccessException ignored) {
            // The fields are public; a failure leaves the value untouched.
        }
    }

    public record Snapshot(double bodyYaw, double bodyYawLast, double headYaw, double headYawLast,
                           double headPitch, double headPitchLast, Vec3 deltaMovement, Vec3 deltaMovementLast) {
    }
}
