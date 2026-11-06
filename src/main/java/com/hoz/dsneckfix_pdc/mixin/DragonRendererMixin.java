package com.hoz.dsneckfix_pdc.mixin;

import by.dragonsurvivalteam.dragonsurvival.client.render.entity.dragon.DragonRenderer;
import com.hoz.dsneckfix_pdc.DsNeckFix;
import com.hoz.dsneckfix_pdc.client.PassengerSync;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
}
