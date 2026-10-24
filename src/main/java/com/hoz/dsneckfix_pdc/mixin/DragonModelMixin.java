package com.hoz.dsneckfix_pdc.mixin;

import by.dragonsurvivalteam.dragonsurvival.client.models.DragonModel;
import by.dragonsurvivalteam.dragonsurvival.common.entity.DragonEntity;
import by.dragonsurvivalteam.dragonsurvival.registry.attachments.MovementData;
import com.hoz.dsneckfix_pdc.DsNeckFix;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animation.AnimationState;

/** Feed paper-doll pose values into the Molang queries DS uses for head/body. */
@Mixin(value = DragonModel.class, remap = false)
public abstract class DragonModelMixin {

    @Inject(method = "applyMolangQueries", at = @At("HEAD"), remap = false)
    private void dsneckfix$overridePose(final AnimationState<DragonEntity> animationState,
                                         final double currentTick, final CallbackInfo ci) {
        // YSM's paper doll applies its own body/head orientation.
        if (!DsNeckFix.isRenderingForCompat() || DsNeckFix.isRenderingYsmPaperDoll()) {
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
        if (player.isPassenger() || !player.getPassengers().isEmpty()) {
            // Forcing head angles while riding makes the head snap forward on animation changes.
            return;
        }
        movement.headYaw = 0;
        movement.headYawLastFrame = 0;
        movement.headPitch = player.getXRot();
        movement.headPitchLastFrame = movement.headPitch;
    }
}
