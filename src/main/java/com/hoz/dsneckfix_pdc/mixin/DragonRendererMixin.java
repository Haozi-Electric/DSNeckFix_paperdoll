package com.hoz.dsneckfix_pdc.mixin;

import by.dragonsurvivalteam.dragonsurvival.client.render.entity.dragon.DragonRenderer;
import by.dragonsurvivalteam.dragonsurvival.common.entity.DragonEntity;
import by.dragonsurvivalteam.dragonsurvival.registry.attachments.MovementData;
import by.dragonsurvivalteam.dragonsurvival.server.handlers.ServerFlightHandler;
import com.hoz.dsneckfix_pdc.DsNeckFix;
import com.hoz.dsneckfix_pdc.client.DsRenderCompat;
import com.hoz.dsneckfix_pdc.client.PassengerSync;
import com.hoz.dsneckfix_pdc.client.RidingChainPaperDoll;
import com.hoz.dsneckfix_pdc.client.SetupRenderStateStore;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.util.RenderUtil;

@Mixin(value = DragonRenderer.class, remap = false)
public abstract class DragonRendererMixin {

    /**
     * Pin the body yaw/flight values only for setupRender and restore them before the model's
     * Molang queries run. On 2.0.72 the temporary isInInventory flag forces setupRender onto the
     * MovementData path; restoring it keeps the model queries on the normal animation state.
     */
    @Inject(method = "setupRender", at = @At("HEAD"), remap = false)
    private void dsneckfix$pinSetupState(final DragonEntity dragon, final Player player,
                                         final PoseStack pose, final float partialTick,
                                         final CallbackInfo ci) {
        if (player == null || DsNeckFix.isRenderingYsmPaperDoll()) {
            // YSM drives its own doll facing/flight; the movement pin only causes 20 Hz stepping
            // and overrides its native follow rotation.
            return;
        }

        final boolean compat = DsNeckFix.isRenderingForCompat();
        final double riderYaw = compat ? Double.NaN : PassengerSync.rootBodyYaw(player, partialTick);
        if (!compat && Double.isNaN(riderYaw)) {
            return;
        }

        final SetupRenderStateStore.State state = SetupRenderStateStore.getOrCreate(dragon.getId());
        if (state.depth++ != 0) {
            return;
        }

        final MovementData movement = MovementData.getData(player);
        state.savedBodyYaw = movement.bodyYaw;
        state.savedPrevXRot = dragon.prevXRot;
        state.savedPrevZRot = dragon.prevZRot;
        state.savedInInventory = dragon.isInInventory;
        state.restore = true;

        if (compat) {
            if (!DsNeckFix.isRenderingYsmPaperDoll()) {
                // Interpolated doll facing; the host sets yBodyRot for the preview.
                movement.bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
            }
            if (!DsNeckFix.isGlideWobbleEnabled()) {
                dragon.prevXRot = 0;
                dragon.prevZRot = 0;
            } else {
                final float[] flight = PassengerSync.sampleFlight(dragon, partialTick);
                if (flight != null) {
                    dragon.prevXRot = flight[0];
                    dragon.prevZRot = flight[1];
                }
            }
        } else {
            // Align a dragon passenger with the root mount's rendered body yaw.
            movement.bodyYaw = riderYaw;
        }

        if (DsRenderCompat.usesAnimationState()) {
            dragon.isInInventory = true;
            state.forcedInInventory = true;
        }
    }

    @Inject(method = "setupRender", at = @At("RETURN"), remap = false)
    private void dsneckfix$restoreSetupState(final DragonEntity dragon, final Player player,
                                             final PoseStack pose, final float partialTick,
                                             final CallbackInfo ci) {
        final SetupRenderStateStore.State state = SetupRenderStateStore.get(dragon.getId());
        if (state == null) {
            return;
        }
        if (--state.depth > 0) {
            return;
        }
        if (state.restore) {
            if (player != null) {
                MovementData.getData(player).bodyYaw = state.savedBodyYaw;
            }
            dragon.prevXRot = state.savedPrevXRot;
            dragon.prevZRot = state.savedPrevZRot;
            if (state.forcedInInventory) {
                dragon.isInInventory = state.savedInInventory;
            }
        }
        SetupRenderStateStore.remove(dragon.getId());
    }

    /** Keep paper-doll renders from writing preview-space bone data into the shared caches. */
    @ModifyExpressionValue(
            method = "postRender(Lcom/mojang/blaze3d/vertex/PoseStack;Lby/dragonsurvivalteam/dragonsurvival/common/entity/DragonEntity;Lsoftware/bernie/geckolib/cache/object/BakedGeoModel;Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZFIII)V",
            at = @At(value = "FIELD",
                    target = "Lby/dragonsurvivalteam/dragonsurvival/common/entity/DragonEntity;isInInventory:Z"),
            remap = false, require = 0)
    private boolean dsneckfix$skipPreviewBoneCache(final boolean original) {
        return original || DsNeckFix.isRenderingForCompat();
    }

    @ModifyExpressionValue(
            method = "renderCubesOfBone",
            at = @At(value = "FIELD",
                    target = "Lby/dragonsurvivalteam/dragonsurvival/common/entity/DragonEntity;isInInventory:Z"),
            remap = false, require = 0)
    private boolean dsneckfix$skipBoneRotationCache(final boolean original) {
        return original || DsNeckFix.isRenderingForCompat();
    }

    @ModifyExpressionValue(
            method = "updateBoneRotation",
            at = @At(value = "FIELD",
                    target = "Lby/dragonsurvivalteam/dragonsurvival/common/entity/DragonEntity;isInInventory:Z"),
            remap = false, require = 0)
    private static boolean dsneckfix$skipBoneRotationUpdate(final boolean original) {
        return original || DsNeckFix.isRenderingForCompat();
    }

    @WrapOperation(method = "setupRender", at = @At(value = "INVOKE",
            target = "Lby/dragonsurvivalteam/dragonsurvival/server/handlers/ServerFlightHandler;"
                   + "isGliding(Lnet/minecraft/world/entity/player/Player;)Z"),
            remap = false)
    private boolean dsneckfix$forceRenderBodyInSetupRender(final Player player,
                                                            final Operation<Boolean> original) {
        if (DsNeckFix.isRenderingForCompat() && !DsNeckFix.isGlideWobbleEnabled()) {
            return false;
        }
        return original.call(player);
    }

    @Inject(method = "getModelOffset", at = @At("HEAD"), cancellable = true, remap = false)
    private void dsneckfix$humanModelOffset(final DragonEntity dragon, final float partialTicks,
                                             final CallbackInfoReturnable<Vec3> cir) {
        if (DsNeckFix.isRenderingYsmPaperDoll()) {
            // YSM cannot show the riding chain and positions its own doll; leave DS's offset alone.
            return;
        }
        if (DsNeckFix.isRenderingRidingChain()) {
            // The chain places every node from its own model-space bones; keep node origins aligned.
            cir.setReturnValue(Vec3.ZERO);
            return;
        }
        if (!DsNeckFix.isPaperDollHumanScaleEnabled()) {
            return;
        }
        final Player player = dragon.getPlayer();
        if (player == null) {
            cir.setReturnValue(Vec3.ZERO);
            return;
        }
        final float bodyYaw;
        if (DsNeckFix.isRenderingYsmPaperDoll()) {
            bodyYaw = ServerFlightHandler.isGliding(player)
                    ? player.getViewYRot(partialTicks)
                    : (float) MovementData.getData(player).bodyYaw;
        } else {
            bodyYaw = player.yBodyRot;
        }
        final float angle = -bodyYaw * ((float) Math.PI / 180);
        cir.setReturnValue(new Vec3(Mth.sin(angle), 0, Mth.cos(angle)));
    }

    /**
     * DS's PlayerRendererMixin applies the world-space seat correction to every rider render.
     * Inside a paper doll that offset is meaningless, and the chain places riders from bones.
     */
    @Inject(method = "getMountingBonePositionCorrection(Lnet/minecraft/world/entity/player/Player;F)"
                   + "Lnet/minecraft/world/phys/Vec3;",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void dsneckfix$suppressWorldSeatCorrection(final Player rider, final float partialTick,
                                                               final CallbackInfoReturnable<Vec3> cir) {
        // Only hosts with our riding-chain renderer place riders from the MountingBone. YSM
        // renders a single doll and still needs DS's world seat correction.
        if (DsNeckFix.isRenderingForCompat() && !DsNeckFix.isRenderingYsmPaperDoll()) {
            cir.setReturnValue(null);
        }
    }

    /**
     * Captures the MountingBone tracked pose relative to the current paper doll chain node base.
     * The copy stops before {@code translateAwayFromPivotPoint}, exactly where GeckoLib records
     * tracked bone matrices.
     */
    @Inject(method = "renderRecursively(Lcom/mojang/blaze3d/vertex/PoseStack;"
                   + "Lby/dragonsurvivalteam/dragonsurvival/common/entity/DragonEntity;"
                   + "Lsoftware/bernie/geckolib/cache/object/GeoBone;"
                   + "Lnet/minecraft/client/renderer/RenderType;"
                   + "Lnet/minecraft/client/renderer/MultiBufferSource;"
                   + "Lcom/mojang/blaze3d/vertex/VertexConsumer;ZFIII)V",
            at = @At("HEAD"), remap = false, require = 0)
    private void dsneckfix$captureMountingBone(final PoseStack poseStack, final DragonEntity dragon,
                                                final GeoBone bone, final RenderType renderType,
                                                final MultiBufferSource bufferSource,
                                                final VertexConsumer buffer, final boolean isReRender,
                                                final float partialTick, final int packedLight,
                                                final int packedOverlay, final int color,
                                                final CallbackInfo ci) {
        if (!RidingChainPaperDoll.isSeatCaptureActive() || !"MountingBone".equals(bone.getName())) {
            return;
        }
        final Player player = dragon.getPlayer();
        if (player == null || !RidingChainPaperDoll.isSeatCaptureTarget(player)) {
            return;
        }
        final PoseStack bonePose = new PoseStack();
        bonePose.last().pose().set(poseStack.last().pose());
        RenderUtil.translateMatrixToBone(bonePose, bone);
        RenderUtil.translateToPivotPoint(bonePose, bone);
        RenderUtil.rotateMatrixAroundBone(bonePose, bone);
        RenderUtil.scaleMatrixForBone(bonePose, bone);
        RidingChainPaperDoll.captureSeatMatrix(bonePose.last().pose());
    }
}
