package com.hoz.dsneckfix.mixin;

import by.dragonsurvivalteam.dragonsurvival.client.models.DragonModel;
import by.dragonsurvivalteam.dragonsurvival.common.entity.DragonEntity;
import by.dragonsurvivalteam.dragonsurvival.registry.attachments.MovementData;
import com.hoz.dsneckfix.DsNeckFix;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animation.AnimationState;

/**
 * Overrides select MovementData fields at the point where DS feeds them
 * into Molang queries, so the paper doll's pose drives head pitch / yaw
 * and tail orientation without fighting the real player's data.
 * <p>
 * bodyYaw is re-applied here because DragonRendererMixin restores it
 * at setupRender RETURN, which is before the animation system reads it.
 */
@Mixin(value = DragonModel.class, remap = false)
public abstract class DragonModelMixin {

    @Inject(method = "applyMolangQueries", at = @At("HEAD"), remap = false)
    private void dsneckfix$overridePose(final AnimationState<DragonEntity> animationState,
                                         final double currentTick, final CallbackInfo ci) {
        if (!DsNeckFix.isRenderingForCompat()) {
            return;
        }

        DragonEntity dragon = animationState.getAnimatable();
        Player player = dragon.getPlayer();

        if (player == null) {
            return;
        }

        MovementData movement = MovementData.getData(player);
        movement.bodyYaw = player.yBodyRot;
        movement.bodyYawLastFrame = movement.bodyYaw;
        movement.headYaw = 0;
        movement.headYawLastFrame = 0;
        movement.headPitch = player.getXRot();
        movement.headPitchLastFrame = movement.headPitch;
    }
}
