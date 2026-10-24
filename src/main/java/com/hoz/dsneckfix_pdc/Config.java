package com.hoz.dsneckfix_pdc;

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

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
