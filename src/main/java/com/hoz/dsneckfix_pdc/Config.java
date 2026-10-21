package com.hoz.dsneckfix_pdc;

import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue PAPER_DOLL_HUMAN_SCALE = BUILDER
            .comment("Scale the dragon model to normal human height in the paper doll preview.",
                    "Large dragons would otherwise overflow the on-screen doll.",
                    "Only affects the paper doll; world and third-person rendering are untouched.",
                    "Default: true")
            .define("paperDollHumanScale", true);

    public static final ModConfigSpec.BooleanValue PAPER_DOLL_GLIDE_WOBBLE = BUILDER
            .comment("Let the paper doll dragon pitch down while gliding, like the dragon",
                    "in the world. Roll/bank is suppressed in the paper doll preview because",
                    "the preview's outer yaw turns the local roll axis into a yaw-like twist.",
                    "Only affects the paper doll rendering.",
                    "Default: false")
            .define("paperDollGlideWobble", false);

    public static ModConfigSpec.BooleanValue SELF_USE_YSM_MODEL;
    public static ModConfigSpec.BooleanValue OTHER_PLAYERS_USE_YSM_MODEL;

    static {
        // Keep the YSM-only options out of the config UI entirely when YSM
        // is not installed. The group is registered after the regular
        // paper-doll compatibility options.
        if (isYsmPresent()) {
            BUILDER.push("ysm_compat");
            SELF_USE_YSM_MODEL = BUILDER
                    .comment("Use YSM's player model for yourself when DragonSurvival and YSM are both installed.",
                            "false = Dragon Survival, true = YSM.",
                            "Only affects dragon players. The mods' own disable switches still apply.",
                            "Default: false")
                    .define("selfUseYsmModel", false);
            OTHER_PLAYERS_USE_YSM_MODEL = BUILDER
                    .comment("Use YSM's player model for other players when DragonSurvival and YSM are both installed.",
                            "false = Dragon Survival, true = YSM.",
                            "Only affects dragon players. The mods' own disable switches still apply.",
                            "Default: true")
                    .define("otherPlayersUseYsmModel", true);
            BUILDER.pop();
        }
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }

    private static boolean isYsmPresent() {
        try {
            final LoadingModList loading = FMLLoader.getLoadingModList();
            if (loading != null) {
                return loading.getModFileById("yes_steve_model") != null;
            }
        } catch (Throwable ignored) {
        }
        try {
            return net.neoforged.fml.ModList.get() != null
                    && net.neoforged.fml.ModList.get().isLoaded("yes_steve_model");
        } catch (Throwable ignored) {
            return false;
        }
    }
}
