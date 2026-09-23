package com.hero.sigil.buffs;

import com.hero.sigil.util.TickMath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TickMathTest {
    @Test
    void convertsSecondsToMinecraftTicks() {
        assertEquals(600L, TickMath.secondsToTicks(30));
    }

    @Test
    void largeSecondValuesDoNotOverflowBeforeConversion() {
        assertEquals(42_949_672_940L, TickMath.secondsToTicks(Integer.MAX_VALUE));
    }

    @Test
    void effectDurationRejectsValuesThatCannotFitMinecraftIntTicks() {
        assertEquals(19_999_980, TickMath.secondsToEffectTicks(999_999));
        assertThrows(ArithmeticException.class, () -> TickMath.secondsToEffectTicks(Integer.MAX_VALUE));
    }
}
