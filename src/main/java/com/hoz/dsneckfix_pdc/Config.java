package com.hoz.dsneckfix_pdc;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue PAPER_DOLL_GLIDE_SWAY = BUILDER
            .translation("dsneckfix_pdc.configuration.paperDollGlideSway")
            .comment("EN: Let the paper doll dragon sway while gliding (dive pitch + tail), like the",
                    "third-person dragon. Set to false to keep it level and the tail straight. Default: true.",
                    "中文：让纸娃娃里的龙在滑翔时摆动（俯冲俯仰 + 尾巴），像第三人称那条龙一样。",
                    "设为 false 则保持水平、尾巴伸直。默认：true")
            .define("paperDollGlideSway", true);

    public static final ModConfigSpec.BooleanValue PAPER_DOLL_ALWAYS_SHOW = BUILDER
            .translation("dsneckfix_pdc.configuration.paperDollAlwaysShow")
            .comment("EN: Always show the paper doll instead of letting it auto-hide. Default: false.",
                    "中文：一直显示纸娃娃，而不是让它自动隐藏。默认：false")
            .define("paperDollAlwaysShow", false);

    public static final ModConfigSpec.BooleanValue PAPER_DOLL_HUMAN_SCALE = BUILDER
            .translation("dsneckfix_pdc.configuration.paperDollHumanScale")
            .comment("EN: Scale the paper doll dragon to human height, like the other paper dolls.",
                    "Default: true.",
                    "中文：把纸娃娃里的龙缩放到人类身高，和其他纸娃娃一致。默认：true")
            .define("paperDollHumanScale", true);

    public static final ModConfigSpec.BooleanValue PAPER_DOLL_RIDING_CHAIN = BUILDER
            .translation("dsneckfix_pdc.configuration.paperDollRidingChain")
            .comment("EN: Render the full DragonSurvival riding chain in the Fuzs paper doll.",
                    "Default: true.",
                    "中文：让 Fuzs 纸娃娃渲染完整的龙之生存骑乘链。默认：true")
            .define("paperDollRidingChain", true);

    public static final ModConfigSpec.BooleanValue DRAGON_DAMAGE_ANIMATIONS = BUILDER
            .translation("dsneckfix_pdc.configuration.dragonDamageAnimations")
            .comment("EN: Show DragonSurvival dragons with the vanilla hurt flash and death fall.",
                    "Default: true.",
                    "中文：让龙之生存的龙显示原版的受伤红光与死亡倾倒动画。默认：true")
            .define("dragonDamageAnimations", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
