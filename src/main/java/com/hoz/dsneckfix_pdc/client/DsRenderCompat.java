package com.hoz.dsneckfix_pdc.client;

/** Detects the DragonSurvival 2.0.72+ tick-based animation pipeline at runtime. */
public final class DsRenderCompat {
    private static final boolean ANIMATION_STATE = hasClass(
            "by.dragonsurvivalteam.dragonsurvival.client.render.util.DragonAnimationState");

    private DsRenderCompat() {
    }

    public static boolean usesAnimationState() {
        return ANIMATION_STATE;
    }

    private static boolean hasClass(final String name) {
        try {
            Class.forName(name, false, DsRenderCompat.class.getClassLoader());
            return true;
        } catch (final Throwable ignored) {
            return false;
        }
    }
}
