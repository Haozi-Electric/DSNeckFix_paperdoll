package com.hoz.dsneckfix_pdc.mixin;

import by.dragonsurvivalteam.dragonsurvival.client.render.entity.dragon.DragonRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lets the paper doll read / override a couple of fields on DragonSurvival's render data:
 * {@code bodyYaw} (face the model forward) and {@code inUI} (to tell our paper doll dragon from DS's
 * own UI dragons in {@link GuiEntityRendererMixin}). Matches DragonSurvival 26.1's
 * {@code DragonRenderer.DragonRenderData}.
 */
@Mixin(DragonRenderer.DragonRenderData.class)
public interface DragonRenderDataAccessor {

    @Accessor("bodyYaw")
    double ds$getBodyYaw();

    @Accessor("bodyYaw")
    void ds$setBodyYaw(double bodyYaw);

    @Accessor("inUI")
    boolean ds$getInUI();

    @Accessor("inUI")
    void ds$setInUI(boolean inUI);

    @Accessor("prevXRot")
    float ds$getPrevXRot();

    @Accessor("prevXRot")
    void ds$setPrevXRot(float prevXRot);

    @Accessor("currentTailMotionUp")
    double ds$getCurrentTailMotionUp();

    @Accessor("currentTailMotionUp")
    void ds$setCurrentTailMotionUp(double currentTailMotionUp);
}
