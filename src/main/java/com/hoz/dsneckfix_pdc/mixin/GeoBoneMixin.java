package com.hoz.dsneckfix_pdc.mixin;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.RenderPassInfo;
import com.hoz.dsneckfix_pdc.client.RidingChainPaperDoll;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures the MountingBone transform while the paper doll's root mount renders. The pose stack at
 * this point contains the full picture-in-picture frame plus the bone's translation/rotation, which
 * is exactly the seat frame needed to place riders.
 */
@Mixin(value = GeoBone.class, remap = false)
public abstract class GeoBoneMixin {

    @Inject(method = "updateBonePositionListeners", at = @At("HEAD"), remap = false, require = 0)
    private void dsneckfix$captureSeat(final PoseStack poseStack, final RenderPassInfo<?> renderPassInfo,
                                       final CallbackInfo ci) {
        try {
            final GeoBone bone = (GeoBone) (Object) this;
            if ("MountingBone".equals(bone.name())) {
                RidingChainPaperDoll.captureSeat(poseStack.last().pose());
            }
        } catch (final RuntimeException | LinkageError ignored) {
            // GeckoLib layout changed; the chain falls back to the root node.
        }
    }
}
