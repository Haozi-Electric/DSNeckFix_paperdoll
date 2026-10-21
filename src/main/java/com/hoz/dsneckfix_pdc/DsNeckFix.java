package com.hoz.dsneckfix_pdc;

import com.hoz.ds_tacz_compat.GunRenderData;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(value = DsNeckFix.MOD_ID, dist = Dist.CLIENT)
public class DsNeckFix {
    public static final String MOD_ID = "dsneckfix_pdc";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static int compatRenderingDepth;
    private static int ysmPaperDollDepth;
    private static int ysmScreenPreviewDepth;

    private static boolean dsTaczLoaded;
    private static boolean dsTaczChecked;
    private static boolean ysmPaperDollLayerLogged;

    public DsNeckFix(final ModContainer modContainer) {
        LOGGER.info("DS Neck Fix loaded");
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
    }


    public static boolean isRenderingForCompat() {
        return compatRenderingDepth > 0;
    }

    /** True while a paper doll is rendering the dragon and the human-scale feature is enabled. */
    public static boolean isPaperDollHumanScaleEnabled() {
        return compatRenderingDepth > 0 && Config.PAPER_DOLL_HUMAN_SCALE.get();
    }

    /** Whether the paper doll dragon is allowed to pitch / bank (wobble) while gliding. */
    public static boolean isGlideWobbleEnabled() {
        return Config.PAPER_DOLL_GLIDE_WOBBLE.get();
    }

    public static void pushCompatRendering() {
        compatRenderingDepth++;
    }

    public static void popCompatRendering() {
        if (compatRenderingDepth > 0) {
            compatRenderingDepth--;
        }
    }

    /** True while YSM's own paper doll layer is rendering. */
    public static boolean isRenderingYsmPaperDoll() {
        return ysmPaperDollDepth > 0;
    }

    public static void pushYsmPaperDoll() {
        ysmPaperDollDepth++;
    }

    public static void popYsmPaperDoll() {
        if (ysmPaperDollDepth > 0) {
            ysmPaperDollDepth--;
        }
    }

    /** True while a YSM GUI or HUD paper doll is rendering the local player. */
    public static boolean isYsmScreenPreview() {
        return ysmScreenPreviewDepth > 0;
    }

    public static void pushYsmScreenPreview() {
        ysmScreenPreviewDepth++;
    }

    public static void popYsmScreenPreview() {
        if (ysmScreenPreviewDepth > 0) {
            ysmScreenPreviewDepth--;
        }
    }

    /**
     * YSM registers its extra player render (paper doll) as a HUD
     * {@code LayeredDraw.Layer}. Obfuscation changes class and method names
     * between builds, but the package stays stable, so this is the
     * version-independent anchor for the layer hook.
     */
    public static boolean isYsmPaperDollLayer(final Object layer) {
        return isYsmPackage(layer.getClass().getName());
    }

    /** YSM's paper doll config preview is rendered from its own Screen. */
    public static boolean isYsmScreen(final Object screen) {
        return isYsmPackage(screen.getClass().getName());
    }

    private static boolean isYsmPackage(final String name) {
        return name.startsWith("com.elfmcys.yesstevemodel.")
                || name.startsWith("com.elfmcys.ysm.")
                || name.startsWith("rip.ysm.");
    }

    public static void logYsmPaperDollLayerOnce() {
        if (!ysmPaperDollLayerLogged) {
            ysmPaperDollLayerLogged = true;
            LOGGER.info("YSM paper doll HUD layer detected; paper doll compat rendering enabled");
        }
    }

    private static boolean isDsTaczLoaded() {
        if (!dsTaczChecked) {
            dsTaczChecked = true;
            try {
                dsTaczLoaded = ModList.get() != null && ModList.get().isLoaded("ds_tacz_compat");
            } catch (Throwable ignored) {
                dsTaczLoaded = false;
            }
        }
        return dsTaczLoaded;
    }

    /**
     * Marks the tr7zw / Fuzss paper doll as actively rendering so
     * ds_tacz_compat's gun renderers draw the floating / back gun in
     * the HUD preview like in third person. No-op when ds_tacz_compat
     * is not installed (one-way compat).
     */
    public static void pushPaperDollGunData() {
        if (isDsTaczLoaded()) {
            GunRenderData.paperDollRenderDepth++;
        }
    }

    public static void popPaperDollGunData() {
        if (isDsTaczLoaded() && GunRenderData.paperDollRenderDepth > 0) {
            GunRenderData.paperDollRenderDepth--;
        }
    }
}
