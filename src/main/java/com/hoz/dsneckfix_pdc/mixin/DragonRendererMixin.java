package com.hoz.dsneckfix_pdc.mixin;

import by.dragonsurvivalteam.dragonsurvival.client.render.entity.dragon.DragonRenderer;
import by.dragonsurvivalteam.dragonsurvival.common.entity.DragonEntity;
import by.dragonsurvivalteam.dragonsurvival.registry.attachments.MovementData;
import by.dragonsurvivalteam.dragonsurvival.server.handlers.ServerFlightHandler;
import com.hoz.dsneckfix_pdc.DsNeckFix;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
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
    private float dsneckfix$savedPrevXRot;
    @Unique
    private float dsneckfix$savedPrevZRot;
    @Unique
    private boolean dsneckfix$needsRestore;

    @Inject(method = "setupRender", at = @At("HEAD"), remap = false)
    private void dsneckfix$overrideBodyYaw(final DragonEntity dragon, final Player player,
                                            final PoseStack pose, final float partialTick,
                                            final CallbackInfo ci) {
        if (!DsNeckFix.isRenderingForCompat() || player == null) {
            return;
        }
        final MovementData movement = MovementData.getData(player);
        dsneckfix$savedBodyYaw = movement.bodyYaw;
        // YSM controls the paper doll's facing itself; locking the body yaw
        // here fights its orientation and makes the doll snap.
        if (!DsNeckFix.isRenderingYsmPaperDoll()) {
            movement.bodyYaw = player.yBodyRot;
        }
        // The paper doll's outer yaw skews the model-local Z roll axis into a
        // yaw-like twist, so roll is suppressed in the preview. Pitch (front /
        // back sway) is kept when the wobble option is enabled.
        dsneckfix$savedPrevZRot = dragon.prevZRot;
        dragon.prevZRot = 0;
        if (!DsNeckFix.isGlideWobbleEnabled()) {
            dsneckfix$savedPrevXRot = dragon.prevXRot;
            dragon.prevXRot = 0;
        }
        dsneckfix$needsRestore = true;
    }

    @Inject(method = "setupRender", at = @At("RETURN"), remap = false)
    private void dsneckfix$restoreBodyYaw(final DragonEntity dragon, final Player player,
                                           final PoseStack pose, final float partialTick,
                                           final CallbackInfo ci) {
        if (!dsneckfix$needsRestore || player == null) {
            return;
        }
        MovementData.getData(player).bodyYaw = dsneckfix$savedBodyYaw;
        dragon.prevZRot = dsneckfix$savedPrevZRot;
        if (!DsNeckFix.isGlideWobbleEnabled()) {
            dragon.prevXRot = dsneckfix$savedPrevXRot;
        }
        dsneckfix$needsRestore = false;
    }

    /**
     * During compat rendering with the wobble disabled, pretend the player is never
     * gliding so the paper doll body stays level (no pitch / roll). When the wobble
     * is enabled, the real gliding value is returned so the body banks like the world
     * dragon.
     */
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

    /**
     * During paper doll rendering with the human-scale feature on, recompute
     * the model offset using the human scale (1.0) so the dragon stays
     * centered instead of being shifted off the preview by the growth scale.
     */
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
