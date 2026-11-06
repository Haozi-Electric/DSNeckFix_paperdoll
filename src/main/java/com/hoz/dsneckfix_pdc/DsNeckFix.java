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

    /** Whether paper-doll dragons are normalized to human height. */
    public static boolean isPaperDollHumanScaleEnabled() {
        return Config.PAPER_DOLL_HUMAN_SCALE.get();
    }

    public static void pushCompatRendering() {
        compatRenderingDepth++;
    }

    public static void popCompatRendering() {
        if (compatRenderingDepth > 0) {
            compatRenderingDepth--;
        }
    }

    /** YSM's paper-doll settings screen (obfuscated class name for 2.6.5 / 26.1). */
    private static final String YSM_PAPER_DOLL_SETTINGS_SCREEN =
            "com.elfmcys.yesstevemodel.oO00o0O0o00oOOOOO0O0o0Oo";

    /**
     * YSM's paper-doll HUD is a NeoForge GuiLayer; the manager wraps it in a lambda, so checking the
     * layer class is unreliable. The render stack contains both GuiLayerManager and YSM frames only
     * for the HUD layer (YSM screens do not go through the HUD manager).
     */
    public static boolean isYsmPaperDollHudCall() {
        boolean ysm = false;
        for (final StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            final String name = frame.getClassName();
            if (name.startsWith("net.neoforged.neoforge.client.gui.GuiLayerManager")) {
                return ysm;
            }
            if (isYsmPackage(name)) {
                ysm = true;
            }
        }
        return false;
    }

    /** The paper-doll settings Screen calls the same helper as the HUD, outside GuiLayerManager. */
    public static boolean isYsmPaperDollSettingsCall() {
        for (final StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            if (YSM_PAPER_DOLL_SETTINGS_SCREEN.equals(frame.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /** Diagnostic: whether any YSM frame exists in the current call stack. */
    public static boolean hasYsmStackFrame() {
        for (final StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            if (isYsmPackage(frame.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isYsmPackage(final String name) {
        return name.startsWith("com.elfmcys.yesstevemodel.")
                || name.startsWith("com.elfmcys.ysm.")
                || name.startsWith("rip.ysm.");
    }
}
