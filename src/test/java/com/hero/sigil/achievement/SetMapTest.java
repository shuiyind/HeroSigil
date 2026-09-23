package com.hero.sigil.achievement;

import com.hero.sigil.util.SetMap;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SetMapTest {
    @Test
    void firstVisitCreatesSetAndReturnsCountOne() {
        SetMap<String> visits = new SetMap<>();

        int count = visits.add("player-1", "plains");

        assertEquals(1, count);
        assertEquals(Set.of("plains"), visits.get("player-1"));
    }

    @Test
    void repeatedVisitDoesNotIncreaseCount() {
        SetMap<String> visits = new SetMap<>();
        visits.add("player-1", "plains");

        int count = visits.add("player-1", "plains");

        assertEquals(1, count);
    }

    @Test
    void visitsAreIsolatedByPlayer() {
        SetMap<String> visits = new SetMap<>();
        visits.add("player-1", "plains");

        int count = visits.add("player-2", "desert");

        assertEquals(1, count);
        assertEquals(Set.of("desert"), visits.get("player-2"));
    }
}
