package com.hero.sigil;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Mod configuration for Hero Sigil.
 */
public class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<Integer> MAX_BUFF_SLOTS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        MAX_BUFF_SLOTS = builder
            .comment("Maximum number of buff slots on the Hero Sigil\n英雄纹章的最大 buff 槽位数")
            .defineInRange("maxBuffSlots", 3, 1, 9);
        SPEC = builder.build();
    }
}
