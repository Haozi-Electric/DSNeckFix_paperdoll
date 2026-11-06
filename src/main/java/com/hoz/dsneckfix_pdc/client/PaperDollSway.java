package com.hoz.dsneckfix_pdc.client;

import by.dragonsurvivalteam.dragonsurvival.client.render.entity.dragon.DragonRenderer;
import by.dragonsurvivalteam.dragonsurvival.server.handlers.ServerFlightHandler;
import com.hoz.dsneckfix_pdc.DsNeckFix;
import com.hoz.dsneckfix_pdc.mixin.DragonRenderDataAccessor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Drives the paper-doll flight pitch/tail from the player's vertical velocity. The live render
 * data can be zero/stale when a GUI render state is built, so both paper doll hosts share this.
 */
public final class PaperDollSway {
    private static final float PITCH_LERP = 0.4F;
    private static float lastPitch;

    private PaperDollSway() {
    }

    public static void apply(final DragonRenderer.DragonRenderData data, final Player player) {
        try {
            final DragonRenderDataAccessor accessor = (DragonRenderDataAccessor) (Object) data;
            if (!DsNeckFix.isGlideSwayEnabled()) {
                accessor.ds$setPrevXRot(0.0F);
                accessor.ds$setCurrentTailMotionUp(0.0D);
                return;
            }

            final float vertical = (float) player.getDeltaMovement().y;
            final float target = ServerFlightHandler.isGliding(player)
                    ? Mth.clamp(vertical * 20.0F, -80.0F, 80.0F) : 0.0F;
            lastPitch = Mth.lerp(PITCH_LERP, lastPitch, target);
            accessor.ds$setPrevXRot(lastPitch);
            accessor.ds$setCurrentTailMotionUp(Mth.clamp(-vertical * 10.0F, -10.0F, 10.0F));
        } catch (final RuntimeException | LinkageError ignored) {
            // Unknown DS version; leave the live animation untouched.
        }
    }
}
