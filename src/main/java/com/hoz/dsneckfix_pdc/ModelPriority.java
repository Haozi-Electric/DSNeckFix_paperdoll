package com.hoz.dsneckfix_pdc;

import by.dragonsurvivalteam.dragonsurvival.common.capability.DragonStateProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.lang.reflect.Method;

public final class ModelPriority {

    private static Method noDragonModelRendering;
    private static boolean noDragonModelRenderingChecked;

    public static boolean isYsmLoaded() {
        try {
            return ModList.get() != null && ModList.get().isLoaded("yes_steve_model");
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** True when YSM should render this player; false means Dragon Survival. */
    public static boolean useYsm(final Player player) {
        if (!isYsmLoaded()) {
            return false;
        }
        try {
            final ModConfigSpec.BooleanValue option = player == Minecraft.getInstance().player
                    ? Config.SELF_USE_YSM_MODEL
                    : Config.OTHER_PLAYERS_USE_YSM_MODEL;
            return option != null && option.get();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Dragon Survival priority for a dragon player. */
    public static boolean shouldSuppressYsm(final Player player) {
        return isYsmLoaded() && !useYsm(player) && DragonStateProvider.isDragon(player);
    }

    /**
     * Whether the dragon body still wants its model rendered. The method was
     * added after DS 2.0.54, so probe it instead of linking against it.
     */
    public static boolean dragonAllowsModel(final Player player) {
        try {
            final Object body = DragonStateProvider.getData(player).body().value();
            if (!noDragonModelRenderingChecked) {
                noDragonModelRenderingChecked = true;
                try {
                    noDragonModelRendering = body.getClass().getMethod("noDragonModelRendering");
                } catch (NoSuchMethodException ignored) {
                    noDragonModelRendering = null;
                }
            }
            return noDragonModelRendering == null
                    || !((Boolean) noDragonModelRendering.invoke(body));
        } catch (Throwable ignored) {
            return true;
        }
    }

    private ModelPriority() {
    }
}
