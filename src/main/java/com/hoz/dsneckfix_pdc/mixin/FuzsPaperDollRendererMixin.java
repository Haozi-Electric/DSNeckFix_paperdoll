package com.hoz.dsneckfix_pdc.mixin;

import by.dragonsurvivalteam.dragonsurvival.client.render.ClientDragonRenderer;
import by.dragonsurvivalteam.dragonsurvival.client.render.entity.dragon.DragonRenderer;
import by.dragonsurvivalteam.dragonsurvival.common.capability.DragonStateProvider;
import by.dragonsurvivalteam.dragonsurvival.common.entity.DragonEntity;
import by.dragonsurvivalteam.dragonsurvival.registry.attachments.MovementData;
import by.dragonsurvivalteam.dragonsurvival.server.handlers.ServerFlightHandler;
import com.geckolib.renderer.base.GeoRenderState;
import com.hoz.dsneckfix_pdc.DsNeckFix;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fuzss PaperDoll compat — makes the paper doll render the player as a DragonSurvival dragon.
 *
 * <p><b>How the pieces fit together (add a new paper doll mod by mirroring this one):</b>
 * <ul>
 *   <li>{@link DsNeckFix#pushCompatRendering()}/{@link DsNeckFix#isRenderingForCompat()} is the shared
 *       compat flag. Paper-doll-agnostic DS fixes ({@link CompatDisplayNeckMixin},
 *       {@link DragonRendererMixin}, {@link DragonRenderDataAccessor}) key off it.</li>
 *   <li>{@link GuiEntityRendererMixin} scopes that flag to exactly one paper doll's deferred render
 *       pass ({@code submit} → {@code renderAllFeatures}), so it never leaks into the world or
 *       first-person render.</li>
 *   <li>This mixin is the only per-mod piece: it must find the paper doll's "extract a render state"
 *       point and, for a dragon player, swap in the live dragon render state. To support another paper
 *       doll mod, add a mirror of this mixin targeting that mod's renderer (extracting a dragon the
 *       same way); {@link GuiEntityRendererMixin} + the DS-side mixins are reusable as-is.</li>
 * </ul>
 * {@link Pseudo} makes this mixin silently skip when Fuzss's PaperDoll is not installed.
 */
@Pseudo
@Mixin(targets = "fuzs.paperdoll.common.client.util.PaperDollRenderer", remap = false)
public abstract class FuzsPaperDollRendererMixin {

    /** Smoothes the glide pitch ramp for the paper doll. */
    @Unique
    private static float dsneckfix$lastPitch;

    @Unique
    private static final float FLIGHT_TILT_LERP = 0.4F;

    /**
     * At the paper doll's render-state extraction, if the entity is a dragon player, replace the
     * (player) state with the LIVE dragon render state. The live dragon's controllers read the player's
     * real MovementData, so the walk/run/fly animation AND the head/neck/tail dynamics are kept. We
     * build a fresh state via the renderer's {@code createRenderState} — NOT DS's
     * {@code EntityRenderDispatcher.extractEntity} and NOT {@code prepareDragonRenderState}, both of
     * which mutated the shared world dragon and polluted the first-person render. We keep it NON-UI
     * ({@code inUI} stays false) so DragonSurvival does not skip the animation-transition smoothing,
     * and only face the body forward. The neck display + centring are done by
     * {@link GuiEntityRendererMixin} pushing the compat flag around the actual render pass.
     */
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dsneckfix$renderDragonWithMovement(
            final LivingEntity livingEntity, final float partialTick,
            final CallbackInfoReturnable<LivingEntityRenderState> cir) {
        if (!(livingEntity instanceof Player player) || !DragonStateProvider.isDragon(player)) {
            return;
        }
        DragonEntity dragon = ClientDragonRenderer.getDragon(player);
        if (dragon == null) {
            return;
        }
        // First-person glide leaves MovementData stale: it is only refreshed (setDragonMovementData /
        // handleFlightMovement) when the world dragon's bones are needed (i.e. mounting), not every
        // frame while hidden. updateAnimationState derives the head/tail PHYSICAL animation from these
        // deltas, so a stale MovementData is exactly why the paper doll's head/tail freeze at the
        // pre-glide value. Refresh it before building the render state.
        ClientDragonRenderer.setDragonMovementData(player, Minecraft.getInstance().getDeltaTracker().getRealtimeDeltaTicks());
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderer<? super LivingEntity, ?> renderer = dispatcher.getRenderer(dragon);
        EntityRenderState renderState = renderer.createRenderState(dragon, partialTick);
        if (renderState instanceof GeoRenderState geo) {
            DragonRenderer.DragonRenderData renderData = geo.getGeckolibData(DragonRenderer.DRAGON_RENDER_DATA);
            if (renderData != null) {
                // Keep the dragon as a NON-UI render (so DS does NOT skip the animation-transition
                // smoothing — that is what made inUI stutter). Only face the body forward; everything
                // else (head/neck/tail animation, dimensions) is left as the live dragon so it follows
                // the player's view and flight naturally. The neck display and centring are handled by
                // GuiEntityRendererMixin (compat flag) so they stay smooth and never leak to the world.
                DragonRenderDataAccessor accessor = (DragonRenderDataAccessor) (Object) renderData;
                accessor.ds$setBodyYaw(0.0D);
                MovementData movement = MovementData.getData(player);
                if (DsNeckFix.isGlideSwayEnabled()) {
                    // Drive the flight pitch from the vertical velocity (clamp(dy*20), smoothed) instead
                    // of movement.prevXRot, which is stale in first person — so the paper doll dives like
                    // the third-person dragon. The paper doll view is flipped, so negate to keep down = down.
                    float pitchTarget = ServerFlightHandler.isGliding(player)
                            ? Mth.clamp((float) (player.getDeltaMovement().y * 20), -80, 80) : 0;
                    dsneckfix$lastPitch = Mth.lerp(FLIGHT_TILT_LERP, dsneckfix$lastPitch, pitchTarget);
                    accessor.ds$setPrevXRot(-dsneckfix$lastPitch);
                    // With MovementData now refreshed, feed the tail the raw vertical motion (no glide-pitch
                    // damping) so it keeps its weight instead of going stone-still while gliding.
                    accessor.ds$setCurrentTailMotionUp(Mth.clamp(-movement.deltaMovement.y * 10.0F, -10.0F, 10.0F));
                } else {
                    // Sway disabled: keep the paper doll level with a straight tail while gliding.
                    accessor.ds$setPrevXRot(0);
                    accessor.ds$setCurrentTailMotionUp(0);
                }
            }
        }
        LivingEntityRenderState living = (LivingEntityRenderState) renderState;
        living.lightCoords = 15728880;
        living.shadowPieces.clear();
        living.outlineColor = 0;
        cir.setReturnValue(living);
    }
}
