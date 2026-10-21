package com.hoz.dsneckfix_pdc.mixin;

import com.hoz.dsneckfix_pdc.DsNeckFix;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * YSM's model selection screen previews the real local player through
 * {@link InventoryScreen}. Its fit formula uses the player's scale and
 * hitbox height, which vary with dragon growth and make the preview
 * inversely sized. Use normal player dimensions for that preview.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenPreviewMixin {

    @WrapOperation(
            method = "renderEntityInInventoryFollowsAngle",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getScale()F"))
    private static float dsneckfix$fixedPreviewScale(final LivingEntity entity,
                                                     final Operation<Float> original) {
        if (DsNeckFix.isYsmScreenPreview() && entity == Minecraft.getInstance().player) {
            return 1.0F;
        }
        return original.call(entity);
    }

    @WrapOperation(
            method = "renderEntityInInventoryFollowsAngle",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getBbHeight()F"))
    private static float dsneckfix$fixedPreviewHeight(final LivingEntity entity,
                                                      final Operation<Float> original) {
        if (DsNeckFix.isYsmScreenPreview() && entity == Minecraft.getInstance().player) {
            return 1.8F;
        }
        return original.call(entity);
    }
}
