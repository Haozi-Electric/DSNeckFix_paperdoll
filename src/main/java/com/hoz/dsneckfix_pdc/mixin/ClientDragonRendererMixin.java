package com.hoz.dsneckfix_pdc.mixin;

import by.dragonsurvivalteam.dragonsurvival.client.render.ClientDragonRenderer;
import by.dragonsurvivalteam.dragonsurvival.common.entity.DragonEntity;
import by.dragonsurvivalteam.dragonsurvival.registry.attachments.MovementData;
import by.dragonsurvivalteam.dragonsurvival.server.handlers.ServerFlightHandler;
import com.hoz.dsneckfix_pdc.DsNeckFix;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientDragonRenderer.class, remap = false)
public abstract class ClientDragonRendererMixin {

    @Unique
    private static double dsneckfix$savedHeadYaw;
    @Unique
    private static double dsneckfix$savedHeadPitch;
    @Unique
    private static double dsneckfix$savedBodyYaw;
    @Unique
    private static double dsneckfix$savedHeadYawLastFrame;
    @Unique
    private static double dsneckfix$savedHeadPitchLastFrame;
    @Unique
    private static double dsneckfix$savedBodyYawLastFrame;
    @Unique
    private static Vec3 dsneckfix$savedDeltaMovement;
    @Unique
    private static Vec3 dsneckfix$savedDeltaMovementLastFrame;
    @Unique
    private static boolean dsneckfix$movementSaved;

    @Inject(method = "renderDragon", at = @At("HEAD"), remap = false)
    private static void dsneckfix$saveMovement(final RenderPlayerEvent.Pre event,
                                               final CallbackInfo ci) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!DsNeckFix.isRenderingForCompat() || dsneckfix$movementSaved) {
            return;
        }
        MovementData movement = MovementData.getData(player);
        dsneckfix$savedHeadYaw = movement.headYaw;
        dsneckfix$savedHeadPitch = movement.headPitch;
        dsneckfix$savedBodyYaw = movement.bodyYaw;
        dsneckfix$savedHeadYawLastFrame = movement.headYawLastFrame;
        dsneckfix$savedHeadPitchLastFrame = movement.headPitchLastFrame;
        dsneckfix$savedBodyYawLastFrame = movement.bodyYawLastFrame;
        dsneckfix$savedDeltaMovement = movement.deltaMovement;
        dsneckfix$savedDeltaMovementLastFrame = movement.deltaMovementLastFrame;
        dsneckfix$movementSaved = true;
    }

    @Inject(method = "renderDragon", at = @At("RETURN"), remap = false)
    private static void dsneckfix$restoreMovement(final RenderPlayerEvent.Pre event, final CallbackInfo ci) {
        if (!dsneckfix$movementSaved) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            dsneckfix$movementSaved = false;
            return;
        }
        MovementData movement = MovementData.getData(player);
        movement.headYaw = dsneckfix$savedHeadYaw;
        movement.headPitch = dsneckfix$savedHeadPitch;
        movement.bodyYaw = dsneckfix$savedBodyYaw;
        movement.headYawLastFrame = dsneckfix$savedHeadYawLastFrame;
        movement.headPitchLastFrame = dsneckfix$savedHeadPitchLastFrame;
        movement.bodyYawLastFrame = dsneckfix$savedBodyYawLastFrame;
        movement.deltaMovement = dsneckfix$savedDeltaMovement;
        movement.deltaMovementLastFrame = dsneckfix$savedDeltaMovementLastFrame;
        dsneckfix$movementSaved = false;
    }

    /** Keep preview movement data out of the real player's MovementData. */
    @ModifyExpressionValue(
            method = "prepareDragonRenderState(Lnet/minecraft/world/entity/player/Player;Lby/dragonsurvivalteam/dragonsurvival/common/entity/DragonEntity;F)V",
            at = @At(value = "FIELD",
                    target = "Lby/dragonsurvivalteam/dragonsurvival/common/entity/DragonEntity;isInInventory:Z"),
            remap = false, require = 1)
    private static boolean dsneckfix$skipPreviewMovementData(final boolean original) {
        return original || DsNeckFix.isRenderingForCompat();
    }

    /** YSM's doll already follows the view; keep the flight yaw from twisting it. */
    @Inject(method = "prepareDragonRenderState", at = @At("RETURN"), remap = false)
    private static void dsneckfix$neutralizeYsmGlide(final Player player,
                                                     final DragonEntity dragon,
                                                     final float partialTick,
                                                     final CallbackInfo ci) {
        if (!DsNeckFix.isRenderingYsmPaperDoll() || !ServerFlightHandler.isGliding(player)) {
            return;
        }
        final MovementData movement = MovementData.getData(player);
        final float yaw = player.getViewYRot(partialTick);
        movement.bodyYaw = yaw;
        movement.bodyYawLastFrame = yaw;
        movement.headYaw = 0;
        movement.headYawLastFrame = 0;
        movement.headPitch = player.getXRot();
        movement.headPitchLastFrame = movement.headPitch;
    }

    @WrapOperation(method = "renderDragon", at = @At(value = "INVOKE",
            target = "Lby/dragonsurvivalteam/dragonsurvival/server/handlers/ServerFlightHandler;"
                   + "isGliding(Lnet/minecraft/world/entity/player/Player;)Z"),
            remap = false)
    private static boolean dsneckfix$forceRenderBody(final Player player,
                                                      final Operation<Boolean> original) {
        if (DsNeckFix.isRenderingForCompat()) {
            return false;
        }
        return original.call(player);
    }

    @WrapOperation(method = "handleFlightMovement", at = @At(value = "INVOKE",
            target = "Lby/dragonsurvivalteam/dragonsurvival/server/handlers/ServerFlightHandler;"
                   + "isGliding(Lnet/minecraft/world/entity/player/Player;)Z"),
            remap = false, require = 0)
    private static boolean dsneckfix$forceRenderBodyInFlightMovement(final Player player,
                                                                      final Operation<Boolean> original) {
        if (DsNeckFix.isRenderingForCompat() && !DsNeckFix.isGlideWobbleEnabled()) {
            return false;
        }
        return original.call(player);
    }
}
