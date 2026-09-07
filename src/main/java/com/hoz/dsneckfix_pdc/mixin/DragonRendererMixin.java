package com.hoz.dsneckfix_pdc.mixin;

import by.dragonsurvivalteam.dragonsurvival.client.render.entity.dragon.DragonRenderer;
import com.hoz.dsneckfix_pdc.DsNeckFix;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Zeroes the dragon model offset during the paper doll render so the model stays centred instead of
 * orbiting the preview origin with the player's body yaw. Gated on the compat flag, which
 * {@link GuiEntityRendererMixin} pushes only around the paper doll's own render pass — so it never
 * affects the world dragon.
 */
@Mixin(value = DragonRenderer.class, remap = false)
public abstract class DragonRendererMixin {

    @Inject(method = "getModelOffset", at = @At("HEAD"), cancellable = true, remap = false)
    private void dsneckfix$centerModel(final DragonRenderer.DragonRenderData renderData,
                                       final CallbackInfoReturnable<Vec3> cir) {
        if (DsNeckFix.isRenderingForCompat()) {
            cir.setReturnValue(Vec3.ZERO);
        }
    }
}
