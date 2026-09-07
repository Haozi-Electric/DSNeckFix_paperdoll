package com.hoz.dsneckfix_pdc;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;

@Mod(value = DsNeckFix.MOD_ID, dist = Dist.CLIENT)
public class DsNeckFix {
    public static final String MOD_ID = "dsneckfix_pdc";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static int compatRenderingDepth;

    public DsNeckFix(final ModContainer modContainer) {
        LOGGER.info("DS Neck Fix loaded");
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        // NeoForge's built-in config screen, so the Mods menu "Config" button opens the above options.
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (container, parent) -> new ConfigurationScreen(container, parent));
    }

    public static boolean isRenderingForCompat() {
        return compatRenderingDepth > 0;
    }

    /** Whether the paper doll dragon should sway (dive pitch + tail) while gliding. */
    public static boolean isGlideSwayEnabled() {
        return Config.PAPER_DOLL_GLIDE_SWAY.get();
    }

    /** Whether the paper doll should always be shown instead of auto-hiding. */
    public static boolean isPaperDollAlwaysShow() {
        return Config.PAPER_DOLL_ALWAYS_SHOW.get();
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
