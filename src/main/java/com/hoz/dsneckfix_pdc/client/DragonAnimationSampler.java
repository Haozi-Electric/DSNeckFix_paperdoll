package com.hoz.dsneckfix_pdc.client;

import by.dragonsurvivalteam.dragonsurvival.common.entity.DragonEntity;

/** Reads the tick-interpolated body yaw from DragonSurvival's animation state. */
public final class DragonAnimationSampler {
    private DragonAnimationSampler() {
    }

    public static Double sampleBodyYaw(final DragonEntity dragon, final float partialTick) {
        if (dragon == null) {
            return null;
        }
        final var pose = dragon.animationState.sample(partialTick);
        return pose == null ? null : pose.bodyYaw();
    }
}
