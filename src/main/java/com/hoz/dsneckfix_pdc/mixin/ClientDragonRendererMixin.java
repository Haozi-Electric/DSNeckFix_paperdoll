package com.hoz.dsneckfix_pdc.mixin;

import by.dragonsurvivalteam.dragonsurvival.client.render.ClientDragonRenderer;
import by.dragonsurvivalteam.dragonsurvival.common.capability.DragonStateProvider;
import by.dragonsurvivalteam.dragonsurvival.common.entity.DragonEntity;
import by.dragonsurvivalteam.dragonsurvival.registry.attachments.MovementData;
import by.dragonsurvivalteam.dragonsurvival.server.handlers.ServerFlightHandler;
import com.hoz.dsneckfix_pdc.Config;
import com.hoz.dsneckfix_pdc.DsNeckFix;
import com.hoz.dsneckfix_pdc.client.MovementDataCompat;
import com.hoz.dsneckfix_pdc.client.RenderStateStore;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientDragonRenderer.class, remap = false)
public abstract class ClientDragonRendererMixin {

    @Inject(method = "renderDragon", at = @At("HEAD"), remap = false)
    private static void dsneckfix$saveMovement(final RenderPlayerEvent.Pre event,
                                               final CallbackInfo ci) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        final RenderStateStore.State state = RenderStateStore.getOrCreate(player.getId());
        if (state.depth++ != 0) {
            return;
        }
        dsneckfix$syncDamageState(player, state);
        if (!DsNeckFix.isRenderingForCompat()) {
            return;
        }
        state.movement = MovementDataCompat.snapshot(MovementData.getData(player));
        state.movementSaved = true;
    }

    @Inject(method = "renderDragon", at = @At("RETURN"), remap = false)
    private static void dsneckfix$restoreMovement(final RenderPlayerEvent.Pre event, final CallbackInfo ci) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        final RenderStateStore.State state = RenderStateStore.get(player.getId());
        if (state == null) {
            return;
        }
        if (--state.depth > 0) {
            return;
        }
        dsneckfix$restoreDamageState(state);
        if (state.movementSaved && state.movement != null) {
            MovementDataCompat.restore(MovementData.getData(player), state.movement);
        }
        RenderStateStore.remove(player.getId());
    }

    /**
     * DS never copies the player's hurtTime/deathTime to the client-side dragon proxy, so
     * GeckoLib's hurt overlay and death fall never trigger. Copy them for the duration of the
     * render only; the proxy itself stays untouched between frames.
     */
    @Unique
    private static void dsneckfix$syncDamageState(final Player player, final RenderStateStore.State state) {
        if (!Config.DRAGON_DAMAGE_ANIMATIONS.get() || !DragonStateProvider.isDragon(player)) {
            return;
        }
        final DragonEntity dragon = ClientDragonRenderer.getOrCreateDragon(player);
        state.damageDragon = dragon;
        state.hurtTime = dragon.hurtTime;
        state.deathTime = dragon.deathTime;
        dragon.hurtTime = player.hurtTime;
        dragon.deathTime = player.deathTime;
    }

    @Unique
    private static void dsneckfix$restoreDamageState(final RenderStateStore.State state) {
        if (state.damageDragon == null) {
            return;
        }
        state.damageDragon.hurtTime = state.hurtTime;
        state.damageDragon.deathTime = state.deathTime;
        state.damageDragon = null;
    }

    /** Keep preview movement data out of the real player's MovementData (2.0.71; removed in 2.0.72). */
    @ModifyExpressionValue(
            method = "prepareDragonRenderState(Lnet/minecraft/world/entity/player/Player;Lby/dragonsurvivalteam/dragonsurvival/common/entity/DragonEntity;F)V",
            at = @At(value = "FIELD",
                    target = "Lby/dragonsurvivalteam/dragonsurvival/common/entity/DragonEntity;isInInventory:Z"),
            remap = false, require = 0)
    private static boolean dsneckfix$skipPreviewMovementData(final boolean original) {
        return original || DsNeckFix.isRenderingForCompat();
    }

    /** 2.0.71 prepares movement data at render time; keep the YSM doll's glide yaw stable. */
    @Inject(method = "prepareDragonRenderState", at = @At("RETURN"), remap = false, require = 0)
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
        movement.headYaw = 0;
        movement.headPitch = player.getXRot();
        MovementDataCompat.setBodyYawLast(movement, yaw);
        MovementDataCompat.setHeadYawLast(movement, 0);
        MovementDataCompat.setHeadPitchLast(movement, movement.headPitch);
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
