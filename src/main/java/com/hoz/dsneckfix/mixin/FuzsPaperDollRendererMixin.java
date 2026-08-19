package com.hoz.dsneckfix.mixin;

import com.hoz.dsneckfix.DsNeckFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wraps Fuzss's PaperDoll
 * {@code renderEntityInInventoryUpdateRotations} method.
 * <p>
 * {@link Pseudo} makes this mixin silently skip when
 * Fuzss's PaperDoll is not installed.
 */
@Pseudo
@Mixin(targets = "fuzs.paperdoll.client.gui.PaperDollRenderer", remap = false)
public abstract class FuzsPaperDollRendererMixin {

    @Inject(method = "renderEntityInInventoryUpdateRotations", at = @At("HEAD"), remap = false)
    private static void dsneckfix$onRenderHead(final CallbackInfo ci) {
        DsNeckFix.pushCompatRendering();
        DsNeckFix.pushPaperDollGunData();
    }

    @Inject(method = "renderEntityInInventoryUpdateRotations", at = @At("RETURN"), remap = false)
    private static void dsneckfix$onRenderReturn(final CallbackInfo ci) {
        DsNeckFix.popCompatRendering();
        DsNeckFix.popPaperDollGunData();
    }
}
