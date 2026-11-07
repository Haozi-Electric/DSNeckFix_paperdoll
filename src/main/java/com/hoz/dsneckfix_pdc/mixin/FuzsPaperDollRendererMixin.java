package com.hoz.dsneckfix_pdc.mixin;

import by.dragonsurvivalteam.dragonsurvival.client.render.ClientDragonRenderer;
import by.dragonsurvivalteam.dragonsurvival.client.render.entity.dragon.DragonRenderer;
import by.dragonsurvivalteam.dragonsurvival.common.capability.DragonStateProvider;
import by.dragonsurvivalteam.dragonsurvival.common.entity.DragonEntity;
import com.geckolib.renderer.base.GeoRenderState;
import com.hoz.dsneckfix_pdc.DsNeckFix;
import com.hoz.dsneckfix_pdc.client.LegacyMovementRefresh;
import com.hoz.dsneckfix_pdc.client.PaperDollSway;
import com.hoz.dsneckfix_pdc.client.RidingChainPaperDoll;
import net.minecraft.client.Minecraft;
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

import java.util.List;

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
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void dsneckfix$renderDragonWithMovement(
            final LivingEntity livingEntity, final float partialTick,
            final CallbackInfoReturnable<LivingEntityRenderState> cir) {
        try {
            if (!(livingEntity instanceof Player player) || !DragonStateProvider.isDragon(player)) {
                return;
            }

            // When riding another dragon, the chain root becomes the main doll node; the rest is
            // submitted by RidingChainPaperDoll into the same picture-in-picture render.
            List<LivingEntity> chain = null;
            Player displayPlayer = player;
            if (DsNeckFix.isPaperDollRidingChainEnabled()) {
                chain = RidingChainPaperDoll.buildChain(player);
                if (chain != null && chain.get(0) instanceof Player root) {
                    displayPlayer = root;
                } else {
                    chain = null;
                }
            }

            DragonEntity dragon = ClientDragonRenderer.getDragon(displayPlayer);
            if (dragon == null) {
                return;
            }
            // DS 2.1.7 ticks movement on the client; older 26.1 builds need this render-time refresh.
            LegacyMovementRefresh.refresh(displayPlayer);
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            EntityRenderer<? super LivingEntity, ?> renderer = dispatcher.getRenderer(dragon);
            EntityRenderState renderState = renderer.createRenderState(dragon, partialTick);
            if (renderState instanceof GeoRenderState geo) {
                DragonRenderer.DragonRenderData renderData = geo.getGeckolibData(DragonRenderer.DRAGON_RENDER_DATA);
                if (renderData != null) {
                    DragonRenderDataAccessor accessor = (DragonRenderDataAccessor) (Object) renderData;
                    accessor.ds$setBodyYaw(0.0D);
                    if (DsNeckFix.isPaperDollHumanScaleEnabled()) {
                        accessor.ds$setVisualScale(1.0D);
                        accessor.ds$setBodyScaleMultiplier(1.0F);
                    }
                    PaperDollSway.apply(renderData, displayPlayer);
                }
            }
            LivingEntityRenderState living = (LivingEntityRenderState) renderState;
            living.lightCoords = 15728880;
            living.shadowPieces.clear();
            living.outlineColor = 0;
            if (chain != null && chain.size() > 1) {
                RidingChainPaperDoll.plan(living, chain);
            }
            cir.setReturnValue(living);
        } catch (final RuntimeException | LinkageError ignored) {
            // New DS/Fuzs version: let the host render its normal player model.
        }
    }
}
