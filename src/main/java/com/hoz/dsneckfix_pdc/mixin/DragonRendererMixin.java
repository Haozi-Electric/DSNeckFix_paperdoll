package com.hoz.dsneckfix_pdc.mixin;

import by.dragonsurvivalteam.dragonsurvival.client.render.entity.dragon.DragonRenderer;
import by.dragonsurvivalteam.dragonsurvival.common.capability.DragonStateProvider;
import by.dragonsurvivalteam.dragonsurvival.common.entity.DragonEntity;
import by.dragonsurvivalteam.dragonsurvival.registry.attachments.MovementData;
import by.dragonsurvivalteam.dragonsurvival.server.handlers.ServerFlightHandler;
import com.hoz.dsneckfix_pdc.DsNeckFix;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DragonRenderer.class, remap = false)
public abstract class DragonRendererMixin {

    @Unique
    private double dsneckfix$savedBodyYaw;
    @Unique
    private double dsneckfix$savedBodyYawLastFrame;
    @Unique
    private float dsneckfix$savedPrevXRot;
    @Unique
    private float dsneckfix$savedPrevZRot;
    @Unique
    private boolean dsneckfix$needsRestore;
    @Unique
    private boolean dsneckfix$savedInInventory;
    @Unique
    private boolean dsneckfix$restoreInInventory;

    @Inject(method = "setupRender", at = @At("HEAD"), remap = false)
    private void dsneckfix$overrideBodyYaw(final DragonEntity dragon, final Player player,
                                            final PoseStack pose, final float partialTick,
                                            final CallbackInfo ci) {
        if (player == null) {
            return;
        }
        if (!DsNeckFix.isRenderingForCompat()) {
            dsneckfix$syncPassengerBodyYaw(dragon, player);
            return;
        }
        final MovementData movement = MovementData.getData(player);
        dsneckfix$savedBodyYaw = movement.bodyYaw;
        dsneckfix$savedBodyYawLastFrame = movement.bodyYawLastFrame;
        // YSM controls the paper doll's facing itself.
        if (!DsNeckFix.isRenderingYsmPaperDoll()) {
            movement.bodyYaw = player.yBodyRot;
        }
        // Roll is suppressed; pitch stays when wobble is enabled.
        dsneckfix$savedPrevZRot = dragon.prevZRot;
        dragon.prevZRot = 0;
        if (!DsNeckFix.isGlideWobbleEnabled()) {
            dsneckfix$savedPrevXRot = dragon.prevXRot;
            dragon.prevXRot = 0;
        }
        dsneckfix$needsRestore = true;
    }

    /** DS only aligns a dragon passenger's body on its own client; align it on every client. */
    @Unique
    private void dsneckfix$syncPassengerBodyYaw(final DragonEntity dragon, final Player player) {
        if (dsneckfix$needsRestore || !player.isPassenger()) {
            return;
        }
        if (!(player.getVehicle() instanceof Player vehicle) || !DragonStateProvider.isDragon(vehicle)) {
            return;
        }
        final MovementData movement = MovementData.getData(player);
        final MovementData vehicleMovement = MovementData.getData(vehicle);
        dsneckfix$savedBodyYaw = movement.bodyYaw;
        dsneckfix$savedBodyYawLastFrame = movement.bodyYawLastFrame;
        dsneckfix$savedPrevZRot = dragon.prevZRot;
        dsneckfix$savedPrevXRot = dragon.prevXRot;
        final double yaw = vehicleMovement.bodyYawLastFrame;
        movement.bodyYaw = yaw;
        movement.bodyYawLastFrame = yaw;
        dsneckfix$needsRestore = true;
    }

    @Inject(method = "setupRender", at = @At("RETURN"), remap = false)
    private void dsneckfix$restoreBodyYaw(final DragonEntity dragon, final Player player,
                                           final PoseStack pose, final float partialTick,
                                           final CallbackInfo ci) {
        if (!dsneckfix$needsRestore || player == null) {
            return;
        }
        final MovementData movement = MovementData.getData(player);
        movement.bodyYaw = dsneckfix$savedBodyYaw;
        movement.bodyYawLastFrame = dsneckfix$savedBodyYawLastFrame;
        dragon.prevZRot = dsneckfix$savedPrevZRot;
        if (!DsNeckFix.isGlideWobbleEnabled()) {
            dragon.prevXRot = dsneckfix$savedPrevXRot;
        }
        dsneckfix$needsRestore = false;
    }

    /** Keep paper-doll renders from writing preview-space MountingBone data into the shared bone cache. */
    @Inject(method = "postRender(Lcom/mojang/blaze3d/vertex/PoseStack;Lby/dragonsurvivalteam/dragonsurvival/common/entity/DragonEntity;Lsoftware/bernie/geckolib/cache/object/BakedGeoModel;Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZFIII)V",
            at = @At("HEAD"), remap = false, require = 1)
    private void dsneckfix$skipPreviewBoneCache(final PoseStack pose, final DragonEntity dragon,
                                                final BakedGeoModel model, final MultiBufferSource buffers,
                                                final VertexConsumer buffer, final boolean isReRender,
                                                final float partialTick, final int packedLight,
                                                final int packedOverlay, final int color,
                                                final CallbackInfo ci) {
        if (DsNeckFix.isRenderingForCompat()) {
            dsneckfix$savedInInventory = dragon.isInInventory;
            dragon.isInInventory = true;
            dsneckfix$restoreInInventory = true;
        }
    }

    @Inject(method = "postRender(Lcom/mojang/blaze3d/vertex/PoseStack;Lby/dragonsurvivalteam/dragonsurvival/common/entity/DragonEntity;Lsoftware/bernie/geckolib/cache/object/BakedGeoModel;Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZFIII)V",
            at = @At("RETURN"), remap = false, require = 1)
    private void dsneckfix$restoreBoneCacheGuard(final PoseStack pose, final DragonEntity dragon,
                                                 final BakedGeoModel model, final MultiBufferSource buffers,
                                                 final VertexConsumer buffer, final boolean isReRender,
                                                 final float partialTick, final int packedLight,
                                                 final int packedOverlay, final int color,
                                                 final CallbackInfo ci) {
        if (dsneckfix$restoreInInventory) {
            dragon.isInInventory = dsneckfix$savedInInventory;
            dsneckfix$restoreInInventory = false;
        }
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
        if (!DsNeckFix.isPaperDollHumanScaleEnabled()) {
            return;
        }
        Player player = dragon.getPlayer();
        if (player == null) {
            cir.setReturnValue(Vec3.ZERO);
            return;
        }
        float angle = -(float) MovementData.getData(player).bodyYaw * ((float) Math.PI / 180);
        float x = Mth.sin(angle);
        float z = Mth.cos(angle);
        cir.setReturnValue(new Vec3(x, 0, z));
    }
}
