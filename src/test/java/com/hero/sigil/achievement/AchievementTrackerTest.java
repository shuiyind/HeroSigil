package com.hero.sigil.achievement;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AchievementTrackerTest {
    @Test
    void firstBiomeVisitReturnsFreshCount() {
        String playerId = "first-biome-visit";

        try {
            ResourceKey<Biome> biome = null;
            int count = AchievementTracker.recordBiomeVisit(playerId, biome);

            assertEquals(1, count);
            assertEquals(1, AchievementTracker.getBiomeVisitCount(playerId));
        } finally {
            AchievementTracker.clearBiomeVisits(playerId);
        }
    }

    @Test
    void repeatedBiomeVisitKeepsCountStable() {
        String playerId = "repeated-biome-visit";

        try {
            ResourceKey<Biome> biome = null;
            AchievementTracker.recordBiomeVisit(playerId, biome);

            int count = AchievementTracker.recordBiomeVisit(playerId, biome);

            assertEquals(1, count);
            assertEquals(1, AchievementTracker.getBiomeVisitCount(playerId));
        } finally {
            AchievementTracker.clearBiomeVisits(playerId);
        }
    }
}
