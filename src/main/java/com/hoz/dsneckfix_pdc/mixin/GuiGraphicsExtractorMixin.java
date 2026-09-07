package com.hoz.dsneckfix_pdc.mixin;

import by.dragonsurvivalteam.dragonsurvival.client.render.entity.dragon.DragonRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fuzss renders the paper doll into a small rectangle (x0,y0,x1,y1) and scissored to the paper doll's
 * box, so anything of the dragon that sticks out (wings, tail, horns) is clipped. When the entity
 * being rendered is a non-UI dragon (our paper doll), expand the rectangle with generous padding and
 * drop the scissor, so the whole model is kept. The gate is only the paper doll — DS's own UI dragons
 * (inUI) and normal human paper dolls are untouched.
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {

    @Redirect(method = "entity(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FLorg/joml/Vector3f;Lorg/joml/Quaternionf;Lorg/joml/Quaternionf;IIII)V",
            at = @At(value = "NEW", target = "net/minecraft/client/renderer/state/gui/pip/GuiEntityRenderState"))
    private GuiEntityRenderState dsneckfix$paperDollRenderState(
            final EntityRenderState renderState, final Vector3f translation, final Quaternionf rotation,
            final Quaternionf overrideCameraAngle, final int x0, final int y0, final int x1, final int y1,
            final float scale, final ScreenRectangle scissorArea) {
        if (dsneckfix$isPaperDollDragon(renderState)) {
            int pad = 400;
            return new GuiEntityRenderState(renderState, translation, rotation, overrideCameraAngle,
                    x0 - pad, y0 - pad, x1 + pad, y1 + pad, scale, null);
        }
        return new GuiEntityRenderState(renderState, translation, rotation, overrideCameraAngle,
                x0, y0, x1, y1, scale, scissorArea);
    }

    @Unique
    private static boolean dsneckfix$isPaperDollDragon(final EntityRenderState state) {
        if (state instanceof GeoRenderState geo) {
            DragonRenderer.DragonRenderData data = geo.getGeckolibData(DragonRenderer.DRAGON_RENDER_DATA);
            // Our paper doll extracts the dragon with inUI false; DS's own UI dragons leave it true, so
            // we only expand for the paper doll.
            return data != null && !((DragonRenderDataAccessor) (Object) data).ds$getInUI();
        }
        return false;
    }
}
