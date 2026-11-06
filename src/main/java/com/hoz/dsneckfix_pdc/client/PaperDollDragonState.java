package com.hoz.dsneckfix_pdc.client;

import by.dragonsurvivalteam.dragonsurvival.client.render.ClientDragonRenderer;
import by.dragonsurvivalteam.dragonsurvival.client.render.entity.dragon.DragonRenderer;
import by.dragonsurvivalteam.dragonsurvival.common.capability.DragonStateProvider;
import by.dragonsurvivalteam.dragonsurvival.common.entity.DragonEntity;
import com.geckolib.renderer.base.GeoRenderState;
import com.hoz.dsneckfix_pdc.DsNeckFix;
import com.hoz.dsneckfix_pdc.mixin.DragonRenderDataAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.player.Player;

/** Builds a live dragon render state for hosts that only extract a player state themselves. */
public final class PaperDollDragonState {
    private static boolean logged;

    private PaperDollDragonState() {
    }

    /**
     * @param hostBodyRot the player body rotation YSM uses in the preview pose (kept for the log).
     * @param hostViewRot the player view rotation. Cancelling the view instead of the body keeps
     *                    the body's limited view-relative turn, like YSM's corrected preview,
     *                    and faces the dragon toward the screen (the old body-yaw cancel left
     *                    it backwards and hard-locked the heading).
     */
    public static EntityRenderState create(final Player player, final float hostBodyRot,
                                           final float hostViewRot) {
        if (player == null || !DragonStateProvider.isDragon(player)) {
            return null;
        }
        final DragonEntity dragon = ClientDragonRenderer.getDragon(player);
        if (dragon == null) {
            return null;
        }

        final EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        final EntityRenderer<? super DragonEntity, ?> renderer = dispatcher.getRenderer(dragon);
        final float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        final EntityRenderState state = renderer.createRenderState(dragon, partialTick);

        if (state instanceof GeoRenderState geo) {
            final DragonRenderer.DragonRenderData data = geo.getGeckolibData(DragonRenderer.DRAGON_RENDER_DATA);
            if (data != null) {
                final DragonRenderDataAccessor accessor = (DragonRenderDataAccessor) (Object) data;
                final double originalVisualScale = data.visualScale();
                final float originalBodyScale = data.bodyScaleMultiplier();
                final double liveBodyYaw = data.bodyYaw();
                // Not a DS inventory preview; the paper doll needs the live animation.
                accessor.ds$setInUI(false);

                // Stable fixed-screen correction: cancel the preview pose's body rotation
                // (the "butt-facing" method, flipped 180 so the dragon faces the camera).
                accessor.ds$setBodyYaw(hostBodyRot);

                if (DsNeckFix.isPaperDollHumanScaleEnabled()) {
                    accessor.ds$setVisualScale(1.0D);
                    accessor.ds$setBodyScaleMultiplier(1.0F);
                    if (state instanceof LivingEntityRenderState living) {
                        if (living.scale > 1.0E-3F) {
                            living.boundingBoxWidth /= living.scale;
                            living.boundingBoxHeight /= living.scale;
                        }
                        living.scale = originalVisualScale > 1.0E-3D
                                ? (float) (1.0D / originalVisualScale) : 1.0F;
                    }
                }
                PaperDollSway.apply(data, player);
                if (!logged) {
                    logged = true;
                    final LivingEntityRenderState living = state instanceof LivingEntityRenderState l ? l : null;
                    DsNeckFix.LOGGER.info(
                            "YSM paper-doll dragon state: hostBodyRot={}, hostViewRot={}, liveBodyYaw={}, "
                                    + "bodyYaw={}, prevXRot={}, tail={}, inUI={}, "
                                    + "visualScale={}->{}, bodyScale={}->{}, stateScale={}, bbox={}x{}, humanScale={}",
                            hostBodyRot, hostViewRot, liveBodyYaw,
                            data.bodyYaw(),
                            data.prevXRot(), data.currentTailMotionUp(), data.inUI(),
                            originalVisualScale, data.visualScale(),
                            originalBodyScale, data.bodyScaleMultiplier(),
                            living == null ? -1 : living.scale,
                            living == null ? -1 : living.boundingBoxWidth,
                            living == null ? -1 : living.boundingBoxHeight,
                            DsNeckFix.isPaperDollHumanScaleEnabled());
                }
            }
        }

        state.lightCoords = 15728880;
        state.shadowPieces.clear();
        state.outlineColor = 0;
        return state;
    }
}
