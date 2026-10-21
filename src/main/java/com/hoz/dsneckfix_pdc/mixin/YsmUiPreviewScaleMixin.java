package com.hoz.dsneckfix_pdc.mixin;

import com.hoz.dsneckfix_pdc.DsNeckFix;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps YSM UI previews at normal player scale; the world model still scales. */
@Mixin(LivingEntity.class)
public abstract class YsmUiPreviewScaleMixin {

    @Inject(method = "getScale", at = @At("HEAD"), cancellable = true)
    private void dsneckfix$fixedScale(final CallbackInfoReturnable<Float> cir) {
        if (DsNeckFix.isYsmScreenPreview() && (Object) this == Minecraft.getInstance().player) {
            cir.setReturnValue(1.0F);
        }
    }
}
