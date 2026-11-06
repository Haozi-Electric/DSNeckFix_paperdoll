package com.hoz.dsneckfix_pdc.mixin;

import by.dragonsurvivalteam.dragonsurvival.client.render.entity.dragon.DragonRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.hoz.dsneckfix_pdc.DsNeckFix;
import com.hoz.dsneckfix_pdc.client.BoneCacheGuard;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.render.pip.GuiEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Scopes the compat flag and DS's shared bone caches to the paper doll's OWN render pass.
 * {@code GuiEntityRenderer.renderToTexture} is the deferred pass that {@code GuiGraphicsExtractor.entity}
 * queues into. DS reads {@code Compat.displayNeck} and runs the model pose there, and its bone updaters
 * write the shared world caches from GUI space. Wrapping exactly this pass — and only for a non-inUI
 * dragon render state (our paper doll) — keeps the fixes local and the world/first-person render clean.
 */
@Mixin(GuiEntityRenderer.class)
public abstract class GuiEntityRendererMixin {

    @WrapMethod(method = "renderToTexture", remap = false)
    private void dsneckfix$scopeCompat(final GuiEntityRenderState entityState, final PoseStack poseStack,
                                       final Operation<Void> original) {
        DragonRenderer.DragonRenderData data = null;
        try {
            data = dsneckfix$paperDollData(entityState.renderState());
        } catch (final RuntimeException | LinkageError ignored) {
            // Unknown DS version: fall through to the host's normal render.
        }
        if (data == null) {
            original.call(entityState, poseStack);
            return;
        }

        boolean guarded = false;
        try {
            BoneCacheGuard.begin(ds$dragonId(data));
            guarded = true;
        } catch (final RuntimeException | LinkageError ignored) {
            // Bone cache layout changed; compat rendering still works without the guard.
        }
        DsNeckFix.pushCompatRendering();
        try {
            original.call(entityState, poseStack);
        } finally {
            DsNeckFix.popCompatRendering();
            if (guarded) {
                try {
                    BoneCacheGuard.restoreAll();
                } catch (final RuntimeException | LinkageError ignored) {
                    // Ignore; the cache guard is best-effort.
                }
            }
        }
    }

    @Unique
    private static int ds$dragonId(final DragonRenderer.DragonRenderData data) {
        try {
            return data.dragonId();
        } catch (final RuntimeException | LinkageError ignored) {
            return -1;
        }
    }

    @Unique
    private static DragonRenderer.DragonRenderData dsneckfix$paperDollData(final EntityRenderState state) {
        try {
            if (state instanceof GeoRenderState geo) {
                final DragonRenderer.DragonRenderData data = geo.getGeckolibData(DragonRenderer.DRAGON_RENDER_DATA);
                if (data != null && !((DragonRenderDataAccessor) (Object) data).ds$getInUI()) {
                    return data;
                }
            }
        } catch (final RuntimeException | LinkageError ignored) {
            // New DS version; treat as a non-dragon state.
        }
        return null;
    }
}
