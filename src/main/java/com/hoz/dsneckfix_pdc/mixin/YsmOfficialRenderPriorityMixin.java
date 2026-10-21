package com.hoz.dsneckfix_pdc.mixin;

import com.hoz.dsneckfix_pdc.ModelPriority;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Official YSM 2.6.5 handler. Class and method names are obfuscated but
 * stable for the supported build; @Pseudo skips this entirely when the
 * class is absent (OpenYSM port, future layouts).
 */
@Pseudo
@Mixin(targets = "com.elfmcys.yesstevemodel.O0ooo0O0oOo0oooo0O00O00o", remap = false)
public abstract class YsmOfficialRenderPriorityMixin {

    @Inject(
            method = "oOo0OO0O0o000OO0O000oo0o(Lnet/neoforged/neoforge/client/event/RenderPlayerEvent$Pre;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void dsneckfix$skipYsmRender(final RenderPlayerEvent.Pre event,
                                                final CallbackInfo ci) {
        if (ModelPriority.shouldSuppressYsm(event.getEntity())) {
            ci.cancel();
        }
    }
}
