package com.hoz.dsneckfix_pdc.mixin;

import by.dragonsurvivalteam.dragonsurvival.client.render.ClientDragonRenderer;
import by.dragonsurvivalteam.dragonsurvival.common.capability.DragonStateProvider;
import by.dragonsurvivalteam.dragonsurvival.common.entity.DragonEntity;
import by.dragonsurvivalteam.dragonsurvival.registry.attachments.MovementData;
import by.dragonsurvivalteam.dragonsurvival.server.handlers.ServerFlightHandler;
import com.hoz.dsneckfix_pdc.DsNeckFix;
import com.hoz.dsneckfix_pdc.ModelPriority;
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

    @Inject(method = "renderDragon", at = @At("HEAD"), remap = false, cancellable = true)
    private static void dsneckfix$saveMovement(final RenderPlayerEvent.Pre event, final CallbackInfo ci) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (ModelPriority.isYsmLoaded()
                && ModelPriority.useYsm(player)
                && DragonStateProvider.isDragon(player)) {
            final DragonEntity dragon = ClientDragonRenderer.getDragon(player);
            if (dragon != null) {
                dragon.renderingWasCancelled = true;
            }
            ci.cancel();
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

    /**
     * {@code prepareDragonRenderState} recalculates and overwrites
     * {@link MovementData#bodyYaw} from the flight direction after our
     * render HEAD hook. For YSM's paper doll the outer doll rotation already
     * follows the player, so align the body with the view after DS is done
     * calculating; otherwise the flight yaw is added on top and the body
     * twists toward the take-off heading.
     */
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
        // This call computes the flight pitch / bank data used by setupRender.
        // Keep it enabled when the paper doll wobble option is on, otherwise
        // there is nothing for setupRender to apply.
        if (DsNeckFix.isRenderingForCompat() && !DsNeckFix.isGlideWobbleEnabled()) {
            return false;
        }
        return original.call(player);
    }
}
