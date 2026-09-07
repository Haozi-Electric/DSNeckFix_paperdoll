package com.hoz.dsneckfix_pdc.mixin;

import by.dragonsurvivalteam.dragonsurvival.client.render.entity.dragon.DragonRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.hoz.dsneckfix_pdc.DsNeckFix;
import net.minecraft.client.gui.render.pip.GuiEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Scopes the compat flag to the paper doll's OWN render pass. {@code GuiEntityRenderer.renderToTexture}
 * is the deferred pass that {@code GuiGraphicsExtractor.entity} queues into: it calls
 * {@code entityRenderDispatcher.submit(...)} and then {@code featureRenderDispatcher.renderAllFeatures()},
 * and it is in {@code renderAllFeatures} that DragonSurvival reads {@code Compat.displayNeck} (neck
 * visibility) and runs the model pose (getModelOffset). By pushing the compat flag around exactly this
 * call — and only when the submitted entity is a non-inUI dragon (our paper doll) — the neck display and
 * centring fixes apply smoothly, and the flag can never leak into the world/first-person render (the
 * world dragon never goes through GuiEntityRenderer).
 */
@Mixin(GuiEntityRenderer.class)
public abstract class GuiEntityRendererMixin {

    @Inject(method = "renderToTexture", at = @At("HEAD"), remap = false)
    private void dsneckfix$push(final GuiEntityRenderState entityState, final PoseStack poseStack, final CallbackInfo ci) {
        if (dsneckfix$isPaperDollDragon(entityState.renderState())) {
            DsNeckFix.pushCompatRendering();
        }
    }

    @Inject(method = "renderToTexture", at = @At("RETURN"), remap = false)
    private void dsneckfix$pop(final GuiEntityRenderState entityState, final PoseStack poseStack, final CallbackInfo ci) {
        if (dsneckfix$isPaperDollDragon(entityState.renderState())) {
            DsNeckFix.popCompatRendering();
        }
    }

    @Unique
    private static boolean dsneckfix$isPaperDollDragon(final EntityRenderState state) {
        if (state instanceof GeoRenderState geo) {
            DragonRenderer.DragonRenderData data = geo.getGeckolibData(DragonRenderer.DRAGON_RENDER_DATA);
            if (data != null) {
                // Our paper doll extracts the dragon with inUI false; DS's own UI dragons (inventory/
                // smithing) leave inUI true, so we only wrap the paper doll's render.
                return !((DragonRenderDataAccessor) (Object) data).ds$getInUI();
            }
        }
        return false;
    }
}
