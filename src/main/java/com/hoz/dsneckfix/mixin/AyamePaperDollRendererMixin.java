package com.hoz.dsneckfix.mixin;

import com.hoz.dsneckfix.DsNeckFix;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wraps Ayame-PaperDoll's {@code performRendering} method.
 * <p>
 * In addition to the standard compat-rendering flag, this mixin fixes two
 * Ayame-PaperDoll rendering defects:
 * <ol>
 *   <li>Resets the {@code forceDisableCulling} flag so subsequent frames
 *       don't suffer z-fighting on all dragon models.</li>
 *   <li>When mirror mode is active, flips the OpenGL front-face winding
 *       to {@code GL_CW} (instead of letting Ayame disable culling
 *       entirely), which correctly compensates for the reversal of
 *       triangle winding caused by the X-axis mirror scale.  This
 *       prevents the "see-through / inside-out" appearance.</li>
 * </ol>
 * <p>
 * {@link Pseudo} makes this mixin silently skip when Ayame-PaperDoll is not installed.
 */
@Pseudo
@Mixin(targets = "org.ayamemc.ayamepaperdoll.hud.PaperDollRenderer", remap = false)
public abstract class AyamePaperDollRendererMixin {

    @Inject(method = "performRendering", at = @At("HEAD"), remap = false)
    private void dsneckfix$onPerformRenderingHead(final Entity targetEntity, final double posX,
                                                   final double posY, final double size,
                                                   final boolean mirror, final Vector3f offset,
                                                   final double lightDegree, final float partialTicks,
                                                   final GuiGraphics guiGraphics, final CallbackInfo ci) {
        DsNeckFix.pushCompatRendering();

        // Prevent Ayame's BufferSourceMixin from disabling face culling.
        // We handle the mirror case correctly via front-face flipping below.
        DsNeckFix.setAyameCullingFlag(false);

        if (mirror) {
            // Mirror scales X by -1, which reverses triangle winding.
            // Flip the front-face to CW so culling still removes the
            // correct (now inside-out) faces.
            RenderSystem.disableCull(); // must be off to safely switch frontFace
            org.lwjgl.opengl.GL11.glFrontFace(org.lwjgl.opengl.GL11.GL_CW);
            RenderSystem.enableCull();
        }
    }

    /**
     * Inject right before the {@code bufferSource.endBatch()} call inside
     * {@code performRendering}.  Ayame has just set
     * {@code forceDisableCulling = true} on the line above; we flip it
     * back to {@code false} so Ayame's {@code BufferSourceMixin} does
     * NOT disable culling during the draw.
     */
    @Inject(method = "performRendering", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V",
            ordinal = 0), remap = false)
    private void dsneckfix$beforeEndBatch(final CallbackInfo ci) {
        DsNeckFix.setAyameCullingFlag(false);
    }

    @Inject(method = "performRendering", at = @At("RETURN"), remap = false)
    private void dsneckfix$onPerformRenderingReturn(final Entity targetEntity, final double posX,
                                                     final double posY, final double size,
                                                     final boolean mirror, final Vector3f offset,
                                                     final double lightDegree, final float partialTicks,
                                                     final GuiGraphics guiGraphics, final CallbackInfo ci) {
        if (mirror) {
            // Restore default counter-clockwise front-face.
            RenderSystem.disableCull();
            org.lwjgl.opengl.GL11.glFrontFace(org.lwjgl.opengl.GL11.GL_CCW);
            RenderSystem.enableCull();
        }

        // Belt and suspenders: ensure Ayame's flag is cleared.
        DsNeckFix.resetAyameCullingFlag();

        DsNeckFix.popCompatRendering();
    }
}
