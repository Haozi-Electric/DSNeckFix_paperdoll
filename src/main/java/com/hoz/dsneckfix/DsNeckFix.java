package com.hoz.dsneckfix;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(value = DsNeckFix.MOD_ID, dist = Dist.CLIENT)
public class DsNeckFix {
    public static final String MOD_ID = "dsneckfix";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static int compatRenderingDepth;

    public DsNeckFix() {
        LOGGER.info("DS Neck Fix loaded");
    }

    public static boolean isRenderingForCompat() {
        return compatRenderingDepth > 0;
    }

    public static void pushCompatRendering() {
        compatRenderingDepth++;
    }

    public static void popCompatRendering() {
        if (compatRenderingDepth > 0) {
            compatRenderingDepth--;
        }
    }
}
