package com.hoz.dsneckfix.mixin;

import com.hoz.dsneckfix.DsNeckFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.tr7zw.paperdoll.PaperDollRenderer", remap = false)
public abstract class PaperDollRendererMixin {

    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void dsneckfix$onRenderHead(final CallbackInfo ci) {
        DsNeckFix.pushCompatRendering();
        DsNeckFix.pushPaperDollGunData();
    }

    @Inject(method = "render", at = @At("RETURN"), remap = false)
    private void dsneckfix$onRenderReturn(final CallbackInfo ci) {
        DsNeckFix.popCompatRendering();
        DsNeckFix.popPaperDollGunData();
    }
}
