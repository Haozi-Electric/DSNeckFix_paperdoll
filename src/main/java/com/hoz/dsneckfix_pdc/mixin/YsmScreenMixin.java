package com.hoz.dsneckfix_pdc.mixin;

import com.hoz.dsneckfix_pdc.DsNeckFix;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;

/**
 * YSM's paper doll config preview is a Screen, not a HUD layer.
 * {@code renderWithTooltip} is final and wraps the subclass render,
 * so this stays independent of YSM's obfuscated screen class names.
 */
@Mixin(Screen.class)
public abstract class YsmScreenMixin {

    @WrapMethod(method = "renderWithTooltip")
    private void dsneckfix$wrapYsmScreen(final GuiGraphics graphics,
                                         final int mouseX,
                                         final int mouseY,
                                         final float partialTick,
                                         final Operation<Void> original) {
        if (!DsNeckFix.isYsmScreen(this)) {
            original.call(graphics, mouseX, mouseY, partialTick);
            return;
        }

        DsNeckFix.pushYsmScreenPreview();
        DsNeckFix.pushYsmPaperDoll();
        DsNeckFix.pushCompatRendering();
        DsNeckFix.pushPaperDollGunData();
        try {
            original.call(graphics, mouseX, mouseY, partialTick);
        } finally {
            DsNeckFix.popPaperDollGunData();
            DsNeckFix.popCompatRendering();
            DsNeckFix.popYsmPaperDoll();
            DsNeckFix.popYsmScreenPreview();
        }
    }
}
