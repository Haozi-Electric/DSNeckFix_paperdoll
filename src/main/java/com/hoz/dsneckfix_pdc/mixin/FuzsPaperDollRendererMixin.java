package com.hoz.dsneckfix_pdc.mixin;

import com.hoz.dsneckfix_pdc.DsNeckFix;
import com.hoz.dsneckfix_pdc.client.RidingChainPaperDoll;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Fuzs PaperDoll compatibility.
 * <p>
 * The host only ever renders one entity. For a DragonSurvival riding chain the whole chain is
 * rendered in the same GUI frame instead, using the MountingBone transforms captured from the
 * mounts (see {@link RidingChainPaperDoll}).
 */
@Pseudo
@Mixin(targets = "fuzs.paperdoll.client.gui.PaperDollRenderer", remap = false)
public abstract class FuzsPaperDollRendererMixin {

    @Unique
    private static boolean dsneckfix$compatPushed;

    @Unique
    private static boolean dsneckfix$chainFailureLogged;

    @Unique
    private static boolean dsneckfix$hostPoseChecked;

    @Unique
    private static Method dsneckfix$hostPoseMethod;

    @Inject(method = "renderEntityInInventoryUpdateRotations", at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void dsneckfix$onRenderHead(final GuiGraphics graphics, final int posX, final int posY,
                                               final int scale, final LivingEntity entity,
                                               final float partialTicks, final CallbackInfo ci) {
        DsNeckFix.pushCompatRendering();
        DsNeckFix.pushPaperDollGunData();
        dsneckfix$compatPushed = true;

        if (!DsNeckFix.isPaperDollRidingChainEnabled()) {
            return;
        }

        try {
            if (dsneckfix$renderChain(graphics, posX, posY, scale, entity, partialTicks)) {
                // Pop before cancelling so the RETURN handler never double-pops.
                dsneckfix$popCompat();
                ci.cancel();
            }
        } catch (RuntimeException exception) {
            if (!dsneckfix$chainFailureLogged) {
                dsneckfix$chainFailureLogged = true;
                DsNeckFix.LOGGER.warn("Riding chain paper doll rendering failed; falling back", exception);
            }
        }
    }

    @Inject(method = "renderEntityInInventoryUpdateRotations", at = @At("RETURN"), remap = false)
    private static void dsneckfix$onRenderReturn(final GuiGraphics graphics, final int posX, final int posY,
                                                 final int scale, final LivingEntity entity,
                                                 final float partialTicks, final CallbackInfo ci) {
        if (dsneckfix$compatPushed) {
            dsneckfix$popCompat();
        }
    }

    @Unique
    private static void dsneckfix$popCompat() {
        if (!dsneckfix$compatPushed) {
            return;
        }
        dsneckfix$compatPushed = false;
        DsNeckFix.popPaperDollGunData();
        DsNeckFix.popCompatRendering();
    }

    @Unique
    private static boolean dsneckfix$renderChain(final GuiGraphics graphics, final int posX, final int posY,
                                                 final int scale, final LivingEntity entity,
                                                 final float partialTicks) {
        final List<LivingEntity> chain = RidingChainPaperDoll.buildChain(entity);
        if (chain == null) {
            return false;
        }

        final LivingEntity root = chain.get(0);
        final Map<LivingEntity, List<LivingEntity>> children = RidingChainPaperDoll.buildChildren(chain);
        final float groupScale = RidingChainPaperDoll.getGroupScale(root, partialTicks);
        final RidingChainPaperDoll.ChainFit fit = RidingChainPaperDoll.computeFit(chain, root, scale,
                posY, graphics.guiHeight());
        final float size = scale * groupScale * fit.scale();

        final float savedXRot = entity.getXRot();
        final float savedXRotO = entity.xRotO;
        final float savedBodyRot = entity.yBodyRot;
        final float savedBodyRotO = entity.yBodyRotO;
        final float savedHeadRot = entity.yHeadRot;
        final float savedHeadRotO = entity.yHeadRotO;

        final float[] pose = {180.0F, 180.0F, 7.5F};
        if (dsneckfix$applyHostPose(entity)) {
            // Fuzs rotates the doll by its position preset (left/right 15 degrees) and tracks
            // the mouse with the head; reuse exactly what the host computed.
            pose[0] = entity.yBodyRot;
            pose[1] = entity.yHeadRot;
            pose[2] = entity.getXRot();
        }
        final Consumer<LivingEntity> poser = node -> dsneckfix$poseNode(node, pose[0], pose[1], pose[2]);

        final PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        try {
            poseStack.translate(posX, posY + fit.shift(), 50.0);
            poseStack.scale(size, size, -size);

            final Quaternionf look = new Quaternionf().rotateX((float) (Math.PI / 12));
            final Quaternionf orientation = new Quaternionf().rotateZ((float) Math.PI);
            orientation.mul(look);
            poseStack.mulPose(orientation);

            Lighting.setupForEntityInInventory();
            final Minecraft minecraft = Minecraft.getInstance();
            final EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
            dispatcher.overrideCameraOrientation(new Quaternionf(look).conjugate());
            dispatcher.setRenderShadow(false);
            final MultiBufferSource.BufferSource buffers = graphics.bufferSource();
            try {
                RidingChainPaperDoll.renderChain(poseStack, chain, children, dispatcher, buffers,
                        partialTicks, poser);
                graphics.flush();
            } finally {
                dispatcher.setRenderShadow(true);
                Lighting.setupFor3DItems();
            }
        } finally {
            poseStack.popPose();
            entity.setXRot(savedXRot);
            entity.xRotO = savedXRotO;
            entity.yBodyRot = savedBodyRot;
            entity.yBodyRotO = savedBodyRotO;
            entity.yHeadRot = savedHeadRot;
            entity.yHeadRotO = savedHeadRotO;
        }
        return true;
    }

    @Unique
    private static boolean dsneckfix$applyHostPose(final LivingEntity entity) {
        if (!dsneckfix$hostPoseChecked) {
            dsneckfix$hostPoseChecked = true;
            try {
                final Class<?> handler = Class.forName("fuzs.paperdoll.client.handler.PaperDollHandler");
                dsneckfix$hostPoseMethod = handler.getMethod("applyEntityRotations", LivingEntity.class);
            } catch (ReflectiveOperationException | LinkageError ignored) {
                dsneckfix$hostPoseMethod = null;
            }
        }
        if (dsneckfix$hostPoseMethod == null) {
            return false;
        }
        try {
            dsneckfix$hostPoseMethod.invoke(null, entity);
            return true;
        } catch (ReflectiveOperationException exception) {
            DsNeckFix.LOGGER.warn("Fuzs PaperDoll pose is not applicable; using a fixed doll pose", exception);
            dsneckfix$hostPoseMethod = null;
            return false;
        }
    }

    @Unique
    private static void dsneckfix$poseNode(final LivingEntity node, final float bodyYaw,
                                           final float headYaw, final float headPitch) {
        node.setXRot(headPitch);
        node.xRotO = headPitch;
        node.yBodyRot = bodyYaw;
        node.yBodyRotO = bodyYaw;
        node.yHeadRot = headYaw;
        node.yHeadRotO = headYaw;
    }
}
