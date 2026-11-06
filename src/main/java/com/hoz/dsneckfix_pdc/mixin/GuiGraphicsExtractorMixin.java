package com.hoz.dsneckfix_pdc.mixin;

import by.dragonsurvivalteam.dragonsurvival.client.render.entity.dragon.DragonRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.hoz.dsneckfix_pdc.DsNeckFix;
import com.hoz.dsneckfix_pdc.client.PaperDollDragonState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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

    @Unique
    private static boolean dsneckfix$loggedYsmAvatarDebug;

    @Redirect(method = "entity(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FLorg/joml/Vector3f;Lorg/joml/Quaternionf;Lorg/joml/Quaternionf;IIII)V",
            at = @At(value = "NEW", target = "net/minecraft/client/renderer/state/gui/pip/GuiEntityRenderState"))
    private GuiEntityRenderState dsneckfix$paperDollRenderState(
            final EntityRenderState renderState, final Vector3f translation, final Quaternionf rotation,
            final Quaternionf overrideCameraAngle, final int x0, final int y0, final int x1, final int y1,
            final float scale, final ScreenRectangle scissorArea) {
        final boolean paperDoll = dsneckfix$isPaperDollDragon(renderState);
        // Rapid GUI scaling can pass an inverted/zero rectangle or a non-positive scale; MC's
        // picture-in-picture then tries to allocate a texture with negative dimensions.
        int left = Math.min(x0, x1);
        int right = Math.max(x0, x1);
        int top = Math.min(y0, y1);
        int bottom = Math.max(y0, y1);
        if (paperDoll) {
            final int pad = 400;
            left -= pad;
            right += pad;
            top -= pad;
            bottom += pad;
        }
        if (right <= left) {
            right = left + 1;
        }
        if (bottom <= top) {
            bottom = top + 1;
        }
        final float safeScale = Float.isFinite(scale) && scale > 0.0F ? scale : 1.0F;

        return new GuiEntityRenderState(renderState, translation, rotation, overrideCameraAngle,
                left, top, right, bottom, safeScale, paperDoll ? null : scissorArea);
    }

    /**
     * YSM's paper-doll HUD extracts an {@link AvatarRenderState} (its own player model). While that
     * HUD layer renders, swap the queued state for the live dragon state; the deferred
     * {@link GuiEntityRendererMixin} pass then applies the usual compat scoping/centering/neck fix.
     * YSM screens are not wrapped, so their native model and mouse head tracking stay untouched.
     */
    @ModifyVariable(
            method = "entity(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FLorg/joml/Vector3f;Lorg/joml/Quaternionf;Lorg/joml/Quaternionf;IIII)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private EntityRenderState dsneckfix$swapYsmPaperDoll(final EntityRenderState state) {
        if (!(state instanceof AvatarRenderState avatar)) {
            return state;
        }

        final boolean hud = DsNeckFix.isYsmPaperDollHudCall();
        final boolean settings = DsNeckFix.isYsmPaperDollSettingsCall();
        if (!dsneckfix$loggedYsmAvatarDebug) {
            final boolean ysmFrame = DsNeckFix.hasYsmStackFrame();
            if (hud || settings || ysmFrame) {
                dsneckfix$loggedYsmAvatarDebug = true;
                DsNeckFix.LOGGER.info(
                        "YSM avatar preview: hudDetection={}, settingsDetection={}, ysmFrame={}, swapping={}",
                        hud, settings, ysmFrame, hud || settings);
            }
        }
        if (!hud && !settings) {
            return state;
        }

        final Player player = Minecraft.getInstance().player;
        if (player == null) {
            return state;
        }
        try {
            final EntityRenderState dragonState = PaperDollDragonState.create(player, avatar.bodyRot, avatar.yRot);
            return dragonState != null ? dragonState : state;
        } catch (final RuntimeException | LinkageError ignored) {
            return state;
        }
    }

    @Unique
    private static boolean dsneckfix$isPaperDollDragon(final EntityRenderState state) {
        try {
            if (state instanceof GeoRenderState geo) {
                DragonRenderer.DragonRenderData data = geo.getGeckolibData(DragonRenderer.DRAGON_RENDER_DATA);
                // Our paper doll extracts the dragon with inUI false; DS's own UI dragons leave it true, so
                // we only expand for the paper doll.
                return data != null && !((DragonRenderDataAccessor) (Object) data).ds$getInUI();
            }
        } catch (final RuntimeException | LinkageError ignored) {
            // New DS version: leave the rectangle untouched.
        }
        return false;
    }
}
