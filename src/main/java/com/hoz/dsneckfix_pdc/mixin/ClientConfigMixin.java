package com.hoz.dsneckfix_pdc.mixin;

import com.hoz.dsneckfix_pdc.DsNeckFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fuzss PaperDoll compat — when our "always show" config is enabled, force Fuzss's
 * {@code isAlwaysDisplayed()} to true so the paper doll stays up instead of auto-hiding after its
 * display time. {@link Pseudo} makes this silently skip when Fuzss's PaperDoll is not installed.
 */
@Pseudo
@Mixin(targets = "fuzs.paperdoll.common.config.ClientConfig", remap = false)
public abstract class ClientConfigMixin {

    @Inject(method = "isAlwaysDisplayed", at = @At("HEAD"), cancellable = true, remap = false)
    private void dsneckfix$forceAlwaysDisplayed(final CallbackInfoReturnable<Boolean> cir) {
        if (DsNeckFix.isPaperDollAlwaysShow()) {
            cir.setReturnValue(true);
        }
    }
}
