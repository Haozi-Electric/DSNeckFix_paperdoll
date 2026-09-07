package com.hoz.dsneckfix_pdc.mixin;

import by.dragonsurvivalteam.dragonsurvival.compat.Compat;
import com.hoz.dsneckfix_pdc.DsNeckFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The live dragon render hides the neck/head while the player is in first person unless
 * {@link Compat#displayNeck()} is true. During the paper doll render the compat flag is pushed
 * (see {@link GuiEntityRendererMixin}), so we force it here — without setting inUI, which would make
 * DragonSurvival skip the animation-transition smoothing (stutter).
 */
@Mixin(value = Compat.class, remap = false)
public abstract class CompatDisplayNeckMixin {

    @Inject(method = "displayNeck", at = @At("RETURN"), cancellable = true, remap = false)
    private static void dsneckfix$displayNeck(final CallbackInfoReturnable<Boolean> cir) {
        // Only override if the original checks didn't already return true —
        // this preserves DS's own Iris/Freecam/Vista compatibility.
        if (!cir.getReturnValue() && DsNeckFix.isRenderingForCompat()) {
            cir.setReturnValue(true);
        }
    }
}
