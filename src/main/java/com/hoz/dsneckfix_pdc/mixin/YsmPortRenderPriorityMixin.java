package com.hoz.dsneckfix_pdc.mixin;

import com.hoz.dsneckfix_pdc.ModelPriority;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.elfmcys.yesstevemodel.client.event.ReplacePlayerRenderEvent", remap = false)
public abstract class YsmPortRenderPriorityMixin {

    @Inject(
            method = "onRenderPlayerPre(Lnet/neoforged/neoforge/client/event/RenderPlayerEvent$Pre;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void dsneckfix$skipYsmRender(final RenderPlayerEvent.Pre event,
                                                final CallbackInfo ci) {
        if (ModelPriority.shouldSuppressYsm(event.getEntity())) {
            ci.cancel();
        }
    }
}
