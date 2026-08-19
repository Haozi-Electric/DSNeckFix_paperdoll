package com.hoz.dsneckfix.mixin;

import by.dragonsurvivalteam.dragonsurvival.common.entity.DragonEntity;
import com.hoz.dsneckfix.DsNeckFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While the paper doll is rendering a dragon, pins the render scale to
 * normal human height (1.0) instead of the growth-stage scale, so large
 * dragons no longer overflow the on-screen preview. Only active during
 * paper doll rendering — world and third-person rendering are untouched.
 */
@Mixin(value = DragonEntity.class, remap = false)
public abstract class DragonEntityMixin {

    @Inject(method = "getScale", at = @At("HEAD"), cancellable = true, remap = false)
    private void dsneckfix$humanScale(final CallbackInfoReturnable<Float> cir) {
        if (DsNeckFix.isPaperDollHumanScaleEnabled()) {
            cir.setReturnValue(1.0F);
        }
    }
}
