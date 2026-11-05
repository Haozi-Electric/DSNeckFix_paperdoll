package com.hoz.dsneckfix_pdc.mixin;

import com.hoz.dsneckfix_pdc.DsNeckFix;
import com.hoz.dsneckfix_pdc.client.RidingChainPaperDoll;
import dev.tr7zw.trender.gui.client.RenderContext;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * tr7zw PaperDoll compatibility.
 * <p>
 * tr7zw draws the target and every vehicle as separate entities, offset by world positions. For a
 * DragonSurvival riding chain those offsets are meaningless, so the frame is taken over and the
 * whole chain is rendered into one PoseStack, anchored on the vehicles' MountingBone transforms
 * (see {@link RidingChainPaperDoll}).
 * <p>
 * The host's own classes are only touched reflectively because its dependency jars (TRender /
 * TRansition) are nested and not available at compile time.
 */
@Pseudo
@Mixin(targets = "dev.tr7zw.paperdoll.PaperDollRenderer", remap = false)
public abstract class PaperDollRendererMixin {

    @Unique
    private static boolean dsneckfix$compatPushed;

    @Unique
    private static boolean dsneckfix$reflectionReady;

    @Unique
    private static boolean dsneckfix$reflectionFailed;

    @Unique
    private static boolean dsneckfix$chainFailureLogged;

    @Unique
    private static Field dsneckfix$sharedInstanceField;
    @Unique
    private static Field dsneckfix$settingsField;
    @Unique
    private static Field dsneckfix$locationField;
    @Unique
    private static Field dsneckfix$dollXOffsetField;
    @Unique
    private static Field dsneckfix$dollYOffsetField;
    @Unique
    private static Field dsneckfix$dollSizeField;
    @Unique
    private static Field dsneckfix$dollLookingSidesField;
    @Unique
    private static Field dsneckfix$dollLookingUpDownField;
    @Unique
    private static Field dsneckfix$dollHeadModeField;
    @Unique
    private static Field dsneckfix$hideVehicleField;
    @Unique
    private static Field dsneckfix$extractingField;
    @Unique
    private static Method dsneckfix$shouldRenderMethod;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private void dsneckfix$onRenderHead(final float delta, final RenderContext context, final CallbackInfo ci) {
        DsNeckFix.pushCompatRendering();
        DsNeckFix.pushPaperDollGunData();
        dsneckfix$compatPushed = true;

        if (!DsNeckFix.isPaperDollRidingChainEnabled() || !dsneckfix$resolveReflection(this)) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final Entity target = minecraft.getCameraEntity() != null ? minecraft.getCameraEntity() : minecraft.player;
        final List<LivingEntity> chain = RidingChainPaperDoll.buildChain(target);
        // hideVehicle only hides the target's own mounts; passengers of the target are not vehicles.
        if (chain == null || !dsneckfix$shouldRender(this)
                || target.isPassenger()
                        && dsneckfix$booleanField(dsneckfix$settings(this), dsneckfix$hideVehicleField)) {
            return;
        }

        final Object previousExtracting = dsneckfix$getExtractingPaperDoll(this);
        dsneckfix$setExtractingPaperDoll(this, Boolean.TRUE);
        try {
            dsneckfix$renderChain(this, minecraft, chain, delta);
        } catch (RuntimeException exception) {
            if (!dsneckfix$chainFailureLogged) {
                dsneckfix$chainFailureLogged = true;
                DsNeckFix.LOGGER.warn("Riding chain paper doll rendering failed; falling back", exception);
            }
            return;
        } finally {
            dsneckfix$setExtractingPaperDoll(this, previousExtracting);
        }

        dsneckfix$popCompat();
        ci.cancel();
    }

    @Inject(method = "render", at = @At("RETURN"), remap = false)
    private void dsneckfix$onRenderReturn(final float delta, final RenderContext context, final CallbackInfo ci) {
        dsneckfix$popCompat();
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
    private static boolean dsneckfix$resolveReflection(final Object renderer) {
        if (dsneckfix$reflectionReady) {
            return true;
        }
        if (dsneckfix$reflectionFailed) {
            return false;
        }
        try {
            final Class<?> rendererClass = renderer.getClass();
            dsneckfix$sharedInstanceField = rendererClass.getDeclaredField("instance");
            dsneckfix$sharedInstanceField.setAccessible(true);
            final Object shared = dsneckfix$sharedInstanceField.get(renderer);

            final Class<?> sharedClass = shared.getClass();
            dsneckfix$settingsField = sharedClass.getField("settings");
            dsneckfix$extractingField = sharedClass.getField("isExtractingPaperDoll");
            dsneckfix$shouldRenderMethod = rendererClass.getDeclaredMethod("shouldRender");
            dsneckfix$shouldRenderMethod.setAccessible(true);

            final Object settings = dsneckfix$settingsField.get(shared);
            final Class<?> settingsClass = settings.getClass();
            dsneckfix$locationField = settingsClass.getField("location");
            dsneckfix$dollXOffsetField = settingsClass.getField("dollXOffset");
            dsneckfix$dollYOffsetField = settingsClass.getField("dollYOffset");
            dsneckfix$dollSizeField = settingsClass.getField("dollSize");
            dsneckfix$dollLookingSidesField = settingsClass.getField("dollLookingSides");
            dsneckfix$dollLookingUpDownField = settingsClass.getField("dollLookingUpDown");
            dsneckfix$dollHeadModeField = settingsClass.getField("dollHeadMode");
            dsneckfix$hideVehicleField = settingsClass.getField("hideVehicle");

            dsneckfix$reflectionReady = true;
            return true;
        } catch (ReflectiveOperationException exception) {
            dsneckfix$reflectionFailed = true;
            DsNeckFix.LOGGER.warn("tr7zw PaperDoll layout is not readable; riding chain disabled", exception);
            return false;
        }
    }

    @Unique
    private static boolean dsneckfix$shouldRender(final Object renderer) {
        try {
            return (Boolean) dsneckfix$shouldRenderMethod.invoke(renderer);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Unique
    private static Object dsneckfix$settings(final Object renderer) {
        try {
            final Object shared = dsneckfix$sharedInstanceField.get(renderer);
            return dsneckfix$settingsField.get(shared);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Unique
    private static Object dsneckfix$getExtractingPaperDoll(final Object renderer) {
        try {
            final Object shared = dsneckfix$sharedInstanceField.get(renderer);
            return dsneckfix$extractingField.get(shared);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Unique
    private static void dsneckfix$setExtractingPaperDoll(final Object renderer, final Object value) {
        try {
            final Object shared = dsneckfix$sharedInstanceField.get(renderer);
            dsneckfix$extractingField.set(shared, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Unique
    private static int dsneckfix$intField(final Object settings, final Field field) {
        try {
            return field.getInt(settings);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Unique
    private static boolean dsneckfix$booleanField(final Object settings, final Field field) {
        try {
            return field.getBoolean(settings);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Unique
    private static String dsneckfix$nameField(final Object settings, final Field field) {
        try {
            return String.valueOf(field.get(settings));
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Unique
    private static void dsneckfix$renderChain(final Object renderer, final Minecraft minecraft,
                                              final List<LivingEntity> chain, final float delta) {
        final Object settings = dsneckfix$settings(renderer);
        final int size = 25 + dsneckfix$intField(settings, dsneckfix$dollSizeField);
        final int windowWidth = minecraft.getWindow().getGuiScaledWidth();
        final int windowHeight = minecraft.getWindow().getGuiScaledHeight();
        final int dollXOffset = dsneckfix$intField(settings, dsneckfix$dollXOffsetField);
        final int dollYOffset = dsneckfix$intField(settings, dsneckfix$dollYOffsetField);
        final String location = dsneckfix$nameField(settings, dsneckfix$locationField);

        final double xpos;
        final double ypos;
        switch (location) {
            case "TOP_RIGHT": {
                xpos = windowWidth - (25 + dollXOffset);
                ypos = 55 + dollYOffset;
                break;
            }
            case "BOTTOM_LEFT": {
                xpos = 25 + dollXOffset;
                ypos = windowHeight - (55 + dollYOffset);
                break;
            }
            case "BOTTOM_RIGHT": {
                xpos = windowWidth - (25 + dollXOffset);
                ypos = windowHeight - (55 + dollYOffset);
                break;
            }
            case "TOP_LEFT":
            default: {
                xpos = 25 + dollXOffset;
                ypos = 55 + dollYOffset;
                break;
            }
        }

        final float lookSides = -dsneckfix$intField(settings, dsneckfix$dollLookingSidesField);
        final float lookUpDown = dsneckfix$intField(settings, dsneckfix$dollLookingUpDownField);
        final String headMode = dsneckfix$nameField(settings, dsneckfix$dollHeadModeField);
        final boolean lockHeadYRot = "LOCKED".equals(headMode);
        final boolean lockHeadXRot = lockHeadYRot || "FREE_HORIZONTAL".equals(headMode)
                || "STATIC_HORIZONTAL".equals(headMode);
        final boolean freeHeadMode = !lockHeadYRot
                && ("FREE".equals(headMode) || "FREE_HORIZONTAL".equals(headMode));

        final float rotationSide = (float) Math.atan(lookSides / 40.0F);
        final float rotationUp = (float) Math.atan(lookUpDown / 40.0F);
        final LivingEntity root = chain.get(0);
        final Map<LivingEntity, List<LivingEntity>> children = RidingChainPaperDoll.buildChildren(chain);
        final float groupScale = RidingChainPaperDoll.getGroupScale(root, delta);
        final RidingChainPaperDoll.ChainFit fit = RidingChainPaperDoll.computeFit(chain, root, size,
                (float) ypos, (float) minecraft.getWindow().getGuiScaledHeight());
        final Consumer<LivingEntity> poser = node -> dsneckfix$poseNode(node, rotationSide, rotationUp,
                lockHeadXRot, lockHeadYRot, freeHeadMode);

        RenderSystem.getModelViewStack().pushMatrix();
        try {
            RenderSystem.getModelViewStack().translate((float) xpos, (float) ypos + fit.shift(), 1050.0F);
            RenderSystem.getModelViewStack().scale(-1.0F, 1.0F, 1.0F);
            RenderSystem.applyModelViewMatrix();

            final PoseStack poseStack = new PoseStack();
            poseStack.translate(0.0, 0.0, 1000.0);
            final float scale = size * groupScale * fit.scale();
            poseStack.scale(scale, scale, scale);

            // tr7zw feeds the radian-looking value into rotationDegrees; mirror that exactly.
            final Quaternionf look = new Quaternionf().rotateX((float) Math.toRadians(rotationUp * 20.0F));
            final Quaternionf orientation = new Quaternionf().rotateZ((float) Math.PI);
            orientation.mul(look);
            poseStack.mulPose(orientation);

            Lighting.setupForEntityInInventory();
            final EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
            dispatcher.overrideCameraOrientation(new Quaternionf(look).conjugate());
            dispatcher.setRenderShadow(false);
            final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
            try {
                RidingChainPaperDoll.renderChain(poseStack, chain, children, dispatcher, buffers, delta, poser);
            } finally {
                buffers.endBatch();
                dispatcher.setRenderShadow(true);
                Lighting.setupFor3DItems();
            }
        } finally {
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    /** Mirrors tr7zw's per-entity doll orientation. */
    @Unique
    private static void dsneckfix$poseNode(final LivingEntity node, final float rotationSide,
                                           final float rotationUp, final boolean lockHeadXRot,
                                           final boolean lockHeadYRot, final boolean freeHeadMode) {
        final float originalYRot = node.getYRot();
        final float originalYHeadRot = node.yHeadRot;
        final float originalYBodyRot = node.yBodyRot;
        final float yaw = rotationSide * 40.0F;

        node.setYRot(yaw);
        node.yRotO = yaw;
        node.yBodyRot = rotationSide * 20.0F;
        node.yBodyRotO = node.yBodyRot;

        if (lockHeadXRot) {
            node.setXRot(-rotationUp * 20.0F);
            node.xRotO = node.getXRot();
        }

        if (lockHeadYRot) {
            node.yHeadRot = yaw;
            node.yHeadRotO = yaw;
        } else if (freeHeadMode) {
            node.yHeadRot = yaw - (originalYBodyRot - originalYHeadRot);
            node.yHeadRotO = node.yHeadRot;
        } else {
            node.yHeadRot = yaw - (originalYRot - originalYHeadRot);
            node.yHeadRotO = node.yHeadRot;
        }
    }
}
