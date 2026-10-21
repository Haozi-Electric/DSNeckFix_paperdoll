package com.hoz.dsneckfix_pdc.mixin;

import com.hoz.dsneckfix_pdc.DsNeckFix;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.neoforged.neoforge.client.gui.GuiLayerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's HUD is rendered by {@link GuiLayerManager}, not the vanilla
 * {@link LayeredDraw}. This is the layer boundary that actually sees
 * YSM's paper doll overlay.
 */
@Mixin(GuiLayerManager.class)
public abstract class YsmGuiLayerManagerMixin {

    @WrapOperation(
            method = "renderInner(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/LayeredDraw$Layer;render"
                           + "(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"))
    private void dsneckfix$wrapYsmPaperDollLayer(final LayeredDraw.Layer layer,
                                                 final GuiGraphics graphics,
                                                 final DeltaTracker deltaTracker,
                                                 final Operation<Void> original) {
        if (!DsNeckFix.isYsmPaperDollLayer(layer)) {
            original.call(layer, graphics, deltaTracker);
            return;
        }

        DsNeckFix.logYsmPaperDollLayerOnce();
        DsNeckFix.pushYsmScreenPreview();
        DsNeckFix.pushYsmPaperDoll();
        DsNeckFix.pushCompatRendering();
        DsNeckFix.pushPaperDollGunData();
        try {
            original.call(layer, graphics, deltaTracker);
        } finally {
            DsNeckFix.popPaperDollGunData();
            DsNeckFix.popCompatRendering();
            DsNeckFix.popYsmPaperDoll();
            DsNeckFix.popYsmScreenPreview();
        }
    }
}
