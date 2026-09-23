package com.hero.sigil.util;

/**
 * Time conversions for Minecraft tick values.
 */
public final class TickMath {
    private static final long TICKS_PER_SECOND = 20L;

    private TickMath() {
    }

    public static long secondsToTicks(int seconds) {
        return seconds * TICKS_PER_SECOND;
    }

    public static int secondsToEffectTicks(int seconds) {
        return Math.toIntExact(secondsToTicks(seconds));
    }
}
