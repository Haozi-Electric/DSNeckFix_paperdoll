package com.hoz.dsneckfix.mixin;

import by.dragonsurvivalteam.dragonsurvival.client.render.ClientDragonRenderer;
import by.dragonsurvivalteam.dragonsurvival.server.handlers.ServerFlightHandler;
import com.hoz.dsneckfix.DsNeckFix;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientDragonRenderer.class, remap = false)
public abstract class ClientDragonRendererMixin {

    /**
     * Skip {@code setDragonMovementData} during compat rendering so
     * PaperDoll's temporary entity-rotation overrides don't pollute
     * {@code MovementData} and cause first-person body jitter.
     */
    @Inject(method = "setDragonMovementData", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dsneckfix$skipMovementData(final Player player, final float realtimeDeltaTick,
                                                    final CallbackInfo ci) {
        if (DsNeckFix.isRenderingForCompat()) {
            ci.cancel();
        }
    }

    /**
     * During compat rendering, pretend the player is never gliding so
     * the dragon body always renders (even during sprint-flying) and
     * {@code handleFlightMovement} skips its flight-animation branch.
     */
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
}
