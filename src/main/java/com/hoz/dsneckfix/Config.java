package com.hoz.dsneckfix;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue PAPER_DOLL_HUMAN_SCALE = BUILDER
            .comment("Scale the dragon model to normal human height in the paper doll preview.",
                    "Large dragons would otherwise overflow the on-screen doll.",
                    "Only affects the paper doll; world and third-person rendering are untouched.",
                    "Default: true")
            .define("paperDollHumanScale", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
