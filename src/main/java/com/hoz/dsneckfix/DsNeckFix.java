package com.hoz.dsneckfix;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

import java.lang.reflect.Method;

@Mod(value = DsNeckFix.MOD_ID, dist = Dist.CLIENT)
public class DsNeckFix {
    public static final String MOD_ID = "dsneckfix";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static int compatRenderingDepth;
    static Method ayameResetCullingMethod;
    static boolean ayameMethodLookedUp;

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

    /**
     * Sets Ayame-PaperDoll's {@code forceDisableCulling} flag on the
     * shared {@code BufferSource}.  Uses cached reflection.
     */
    public static void setAyameCullingFlag(final boolean value) {
        try {
            final var source = Minecraft.getInstance().renderBuffers().bufferSource();
            if (!ayameMethodLookedUp) {
                ayameResetCullingMethod = source.getClass()
                        .getMethod("ayame_PaperDoll$setForceDisableCulling", boolean.class);
                ayameMethodLookedUp = true;
            }
            if (ayameResetCullingMethod != null) {
                ayameResetCullingMethod.invoke(source, value);
            }
        } catch (final Exception ignored) {
        }
    }

    /** Convenience: reset Ayame's culling flag to {@code false}. */
    public static void resetAyameCullingFlag() {
        setAyameCullingFlag(false);
    }
}
