package com.hoz.dsneckfix.mixin;

import by.dragonsurvivalteam.dragonsurvival.client.render.entity.dragon.DragonRenderer;
import by.dragonsurvivalteam.dragonsurvival.common.entity.DragonEntity;
import by.dragonsurvivalteam.dragonsurvival.registry.attachments.MovementData;
import by.dragonsurvivalteam.dragonsurvival.server.handlers.ServerFlightHandler;
import com.hoz.dsneckfix.DsNeckFix;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DragonRenderer.class, remap = false)
public abstract class DragonRendererMixin {

    @Unique
    private double dsneckfix$savedBodyYaw;
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
        movement.bodyYaw = player.yBodyRot;
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
        dsneckfix$needsRestore = false;
    }

    /**
     * During compat rendering, suppress the gliding check inside
     * {@code setupRender} so no pitch / roll rotation is applied
     * to the dragon model in the PaperDoll preview.
     */
    @WrapOperation(method = "setupRender", at = @At(value = "INVOKE",
            target = "Lby/dragonsurvivalteam/dragonsurvival/server/handlers/ServerFlightHandler;"
                   + "isGliding(Lnet/minecraft/world/entity/player/Player;)Z"),
            remap = false)
    private boolean dsneckfix$forceRenderBodyInSetupRender(final Player player,
                                                            final Operation<Boolean> original) {
        if (DsNeckFix.isRenderingForCompat()) {
            return false;
        }
        return original.call(player);
    }
}
