package com.hoz.dsneckfix_pdc.mixin;

import com.hoz.dsneckfix_pdc.DsNeckFix;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps YSM UI previews at normal player height; the world model still scales. */
@Mixin(Entity.class)
public abstract class YsmUiPreviewHeightMixin {

    @Inject(method = "getBbHeight", at = @At("HEAD"), cancellable = true)
    private void dsneckfix$fixedHeight(final CallbackInfoReturnable<Float> cir) {
        if (DsNeckFix.isYsmScreenPreview() && (Object) this == Minecraft.getInstance().player) {
            cir.setReturnValue(1.8F);
        }
    }
}
