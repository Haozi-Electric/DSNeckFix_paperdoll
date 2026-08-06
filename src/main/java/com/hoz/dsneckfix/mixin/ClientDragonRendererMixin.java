package com.hoz.dsneckfix.mixin;

import by.dragonsurvivalteam.dragonsurvival.client.render.ClientDragonRenderer;
import by.dragonsurvivalteam.dragonsurvival.registry.attachments.MovementData;
import by.dragonsurvivalteam.dragonsurvival.server.handlers.ServerFlightHandler;
import com.hoz.dsneckfix.DsNeckFix;
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
    private static void dsneckfix$saveMovement(final RenderPlayerEvent.Pre event, final CallbackInfo ci) {
        if (!DsNeckFix.isRenderingForCompat() || dsneckfix$movementSaved) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
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
        if (DsNeckFix.isRenderingForCompat()) {
            return false;
        }
        return original.call(player);
    }
}
