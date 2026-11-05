package com.hoz.dsneckfix_pdc.mixin;

import by.dragonsurvivalteam.dragonsurvival.common.entity.DragonEntity;
import com.hoz.dsneckfix_pdc.DsNeckFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Render dragons at human height while a paper doll is active. */
@Mixin(value = DragonEntity.class, remap = false)
public abstract class DragonEntityMixin {

    @Inject(method = "getScale", at = @At("HEAD"), cancellable = true, remap = false)
    private void dsneckfix$humanScale(final CallbackInfoReturnable<Float> cir) {
        if (DsNeckFix.isPaperDollHumanScaleEnabled()) {
            cir.setReturnValue(1.0F);
        }
    }

    /** DS 2.0.72 only advances animations for paused inventory/UI renders; the paper doll is a UI render. */
    @Inject(method = "shouldPlayAnimsWhileGamePaused", at = @At("RETURN"),
            cancellable = true, require = 0, remap = false)
    private void dsneckfix$playAnimsWhilePaused(final CallbackInfoReturnable<Boolean> cir) {
        if (DsNeckFix.isRenderingForCompat()) {
            cir.setReturnValue(true);
        }
    }
}
