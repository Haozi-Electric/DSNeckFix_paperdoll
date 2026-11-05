package com.hoz.dsneckfix_pdc;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue PAPER_DOLL_HUMAN_SCALE = BUILDER
            .translation("dsneckfix_pdc.configuration.paperDollHumanScale")
            .comment("dsneckfix_pdc.configuration.paperDollHumanScale.tooltip")
            .define("paperDollHumanScale", true);

    public static final ModConfigSpec.BooleanValue DRAGON_DAMAGE_ANIMATIONS = BUILDER
            .translation("dsneckfix_pdc.configuration.dragonDamageAnimations")
            .comment("dsneckfix_pdc.configuration.dragonDamageAnimations.tooltip")
            .define("dragonDamageAnimations", true);

    public static final ModConfigSpec.BooleanValue PAPER_DOLL_GLIDE_WOBBLE = BUILDER
            .translation("dsneckfix_pdc.configuration.paperDollGlideWobble")
            .comment("dsneckfix_pdc.configuration.paperDollGlideWobble.tooltip")
            .define("paperDollGlideWobble", false);

    public static final ModConfigSpec.BooleanValue PAPER_DOLL_RIDING_CHAIN = BUILDER
            .translation("dsneckfix_pdc.configuration.paperDollRidingChain")
            .comment("dsneckfix_pdc.configuration.paperDollRidingChain.tooltip")
            .define("paperDollRidingChain", true);

    public static final ModConfigSpec.BooleanValue PAPER_DOLL_RIDING_CHAIN_AUTO_FIT = BUILDER
            .translation("dsneckfix_pdc.configuration.paperDollRidingChainAutoFit")
            .comment("dsneckfix_pdc.configuration.paperDollRidingChainAutoFit.tooltip")
            .define("paperDollRidingChainAutoFit", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
