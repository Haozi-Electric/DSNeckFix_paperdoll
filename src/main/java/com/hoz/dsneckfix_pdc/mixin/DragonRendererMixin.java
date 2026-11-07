package com.hoz.dsneckfix_pdc.mixin;

import by.dragonsurvivalteam.dragonsurvival.client.render.entity.dragon.DragonRenderer;
import by.dragonsurvivalteam.dragonsurvival.common.entity.DragonEntity;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.GeoRenderState;
import com.hoz.dsneckfix_pdc.DsNeckFix;
import com.hoz.dsneckfix_pdc.client.PassengerSync;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Paper doll: zero the model offset so the dragon stays centred instead of orbiting the preview
 * origin. World: align a dragon passenger's body/offset with the root mount, since DS only does
 * that on the passenger's own client. Every DS access is best-effort for newer versions.
 */
@Mixin(value = DragonRenderer.class, remap = false)
public abstract class DragonRendererMixin {

    @Unique
    private double dsneckfix$savedBodyYaw;
    @Unique
    private boolean dsneckfix$bodyYawSynced;

    @Unique
    private boolean dsneckfix$damageApplied;
    @Unique
    private DragonEntity dsneckfix$damageDragon;
    @Unique
    private LivingEntityRenderState dsneckfix$damageState;
    @Unique
    private int dsneckfix$savedDragonHurt;
    @Unique
    private int dsneckfix$savedDragonDeath;
    @Unique
    private float dsneckfix$savedStateDeath;
    @Unique
    private boolean dsneckfix$savedStateHurt;

    @Inject(method = "getModelOffset", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void dsneckfix$centerModel(final DragonRenderer.DragonRenderData renderData,
                                       final CallbackInfoReturnable<Vec3> cir) {
        try {
            if (DsNeckFix.isRenderingForCompat()) {
                cir.setReturnValue(Vec3.ZERO);
                return;
            }

            final Player player = renderData.player();
            if (player == null) {
                return;
            }
            final Player root = PassengerSync.rootDragonMount(player);
            if (root == null) {
                return;
            }

            final double yaw = PassengerSync.rootBodyYaw(root);
            final double angle = -yaw * (Math.PI / 180.0);
            final double scale = renderData.visualScale() * (double) renderData.bodyScaleMultiplier();
            cir.setReturnValue(new Vec3(Math.sin(angle) * scale, 0, Math.cos(angle) * scale));
        } catch (final RuntimeException | LinkageError ignored) {
            // Unknown DS version; keep DS's default offset.
        }
    }

    @Inject(method = "setupRender", at = @At("HEAD"), remap = false, require = 0)
    private void dsneckfix$syncPassengerBodyYaw(final DragonRenderer.DragonRenderData renderData,
                                                final PoseStack pose, final CallbackInfo ci) {
        dsneckfix$bodyYawSynced = false;
        try {
            if (DsNeckFix.isRenderingForCompat()) {
                return;
            }

            final Player player = renderData.player();
            if (player == null) {
                return;
            }
            final Player root = PassengerSync.rootDragonMount(player);
            if (root == null) {
                return;
            }

            final DragonRenderDataAccessor accessor = (DragonRenderDataAccessor) (Object) renderData;
            dsneckfix$savedBodyYaw = accessor.ds$getBodyYaw();
            accessor.ds$setBodyYaw(PassengerSync.rootBodyYaw(root));
            dsneckfix$bodyYawSynced = true;
        } catch (final RuntimeException | LinkageError ignored) {
            // Unknown DS version; no passenger alignment.
        }
    }

    @Inject(method = "setupRender", at = @At("RETURN"), remap = false, require = 0)
    private void dsneckfix$restorePassengerBodyYaw(final DragonRenderer.DragonRenderData renderData,
                                                   final PoseStack pose, final CallbackInfo ci) {
        if (!dsneckfix$bodyYawSynced) {
            return;
        }
        try {
            ((DragonRenderDataAccessor) (Object) renderData).ds$setBodyYaw(dsneckfix$savedBodyYaw);
        } catch (final RuntimeException | LinkageError ignored) {
            // Ignore; the render data layout changed.
        } finally {
            dsneckfix$bodyYawSynced = false;
        }
    }

    /**
     * DS's client-side dragon proxy never receives the player's hurtTime/deathTime, so GeckoLib's
     * red overlay and death fall stay frozen. Copy them for the duration of the render pass only.
     */
    @Inject(method = "performRenderPass(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;"
                     + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;"
                     + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;Ljava/util/List;)V",
            at = @At("HEAD"), remap = false, require = 0)
    private void dsneckfix$syncDamageAnimations(final LivingEntityRenderState renderState, final PoseStack pose,
                                                final SubmitNodeCollector tasks, final CameraRenderState camera,
                                                final List<?> boneUpdaters, final CallbackInfo ci) {
        dsneckfix$damageApplied = false;
        try {
            if (!DsNeckFix.isDragonDamageAnimationsEnabled() || !(renderState instanceof GeoRenderState geo)) {
                return;
            }

            final DragonRenderer.DragonRenderData renderData =
                    geo.getGeckolibData(DragonRenderer.DRAGON_RENDER_DATA);
            if (renderData == null) {
                return;
            }
            final Player player = renderData.player();
            final DragonEntity dragon = renderData.dragon();
            if (player == null || dragon == null) {
                return;
            }

            dsneckfix$damageDragon = dragon;
            dsneckfix$damageState = renderState;
            dsneckfix$savedDragonHurt = dragon.hurtTime;
            dsneckfix$savedDragonDeath = dragon.deathTime;
            dsneckfix$savedStateHurt = renderState.hasRedOverlay;
            dsneckfix$savedStateDeath = renderState.deathTime;

            dragon.hurtTime = player.hurtTime;
            dragon.deathTime = player.deathTime;
            final boolean hurtOrDead = player.hurtTime > 0 || player.deathTime > 0;
            renderState.hasRedOverlay = hurtOrDead;
            renderState.deathTime = player.deathTime > 0 ? player.deathTime + renderState.partialTick : 0.0F;
            // GeckoLib captures the packed overlay before the render pass; refresh it here so the
            // hurt/death red tint is actually used by the model pass.
            geo.addGeckolibData(DataTickets.PACKED_OVERLAY, OverlayTexture.pack(0.0F, hurtOrDead));
            dsneckfix$damageApplied = true;
        } catch (final RuntimeException | LinkageError ignored) {
            // Unknown DS version; leave the dragon's own damage state untouched.
        }
    }

    @Inject(method = "performRenderPass(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;"
                     + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;"
                     + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;Ljava/util/List;)V",
            at = @At("RETURN"), remap = false, require = 0)
    private void dsneckfix$restoreDamageAnimations(final LivingEntityRenderState renderState, final PoseStack pose,
                                                   final SubmitNodeCollector tasks, final CameraRenderState camera,
                                                   final List<?> boneUpdaters, final CallbackInfo ci) {
        if (!dsneckfix$damageApplied) {
            return;
        }
        dsneckfix$damageApplied = false;
        try {
            final DragonEntity dragon = dsneckfix$damageDragon;
            if (dragon != null) {
                dragon.hurtTime = dsneckfix$savedDragonHurt;
                dragon.deathTime = dsneckfix$savedDragonDeath;
            }
            final LivingEntityRenderState state = dsneckfix$damageState;
            if (state != null) {
                state.hasRedOverlay = dsneckfix$savedStateHurt;
                state.deathTime = dsneckfix$savedStateDeath;
            }
        } catch (final RuntimeException | LinkageError ignored) {
            // Ignore; the render data layout changed.
        } finally {
            dsneckfix$damageDragon = null;
            dsneckfix$damageState = null;
        }
    }

    /**
     * The paper-doll chain places riders from the MountingBone matrix itself; DS's world-space seat
     * correction would double-offset human riders inside the doll.
     */
    @Inject(method = "getMountingBonePositionCorrection(Lnet/minecraft/world/entity/player/Player;F)"
                     + "Lnet/minecraft/world/phys/Vec3;",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void dsneckfix$suppressChainSeatCorrection(final Player rider, final float partialTick,
                                                              final CallbackInfoReturnable<Vec3> cir) {
        if (DsNeckFix.isRenderingRidingChain()) {
            cir.setReturnValue(null);
        }
    }
}
