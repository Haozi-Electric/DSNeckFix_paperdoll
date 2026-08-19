package com.hoz.dsneckfix;

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
    public static final String MOD_ID = "dsneckfix";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static int compatRenderingDepth;

    private static boolean dsTaczLoaded;
    private static boolean dsTaczChecked;

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

    public static void pushCompatRendering() {
        compatRenderingDepth++;
    }

    public static void popCompatRendering() {
        if (compatRenderingDepth > 0) {
            compatRenderingDepth--;
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
