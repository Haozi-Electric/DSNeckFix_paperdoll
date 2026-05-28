package com.hoz.dsneckfix.mixin;

import by.dragonsurvivalteam.dragonsurvival.compat.Compat;
import com.hoz.dsneckfix.DsNeckFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds our compat-rendering flag to DragonSurvival's existing
 * {@link Compat#displayNeck()} hook, which was specifically
 * designed for mods that need the neck/head displayed in first person.
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
