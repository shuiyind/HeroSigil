package com.hero.sigil.achievement;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks player progress towards achievements.
 * Each achievement unlocks a buff slot on the Hero Sigil.
 */
@EventBusSubscriber(modid = HeroSigil.MODID, bus = EventBusSubscriber.Bus.GAME)
public class AchievementTracker {

    // Store achievement progress per player
    private static final Map<String, PlayerProgress> PLAYER_PROGRESSES = new HashMap<>();

    /**
     * Sample achievements (to be expanded):
     */
    public enum AchievementType {
        DEFEAT_BOSS(0),           // Unlock: Saturation
        EXPLORE_ALL_BIOMES(1),    // Unlock: Health Boost
        DEFEAT_ENDER_DRAGON(2),   // Unlock: Speed
        BUILD_REDSTONE_MACHINE(3),// Unlock: Strength
        COMPLETE_COLLECTION(4);   // Unlock: Night Vision

        private final int slotIndex;

        AchievementType(int slotIndex) {
            this.slotIndex = slotIndex;
        }

        public int getSlotIndex() {
            return slotIndex;
        }
    }

    /**
     * Player progress tracking class.
     */
    public static class PlayerProgress {
        private final Map<AchievementType, Boolean> unlockedAchievements = new HashMap<>();
        private final Map<AchievementType, Integer> progressValues = new HashMap<>();

        public boolean isUnlocked(AchievementType achievement) {
            return unlockedAchievements.getOrDefault(achievement, false);
        }

        public void unlock(AchievementType achievement) {
            if (!isUnlocked(achievement)) {
                unlockedAchievements.put(achievement, true);
                // TODO: Notify player of new buff slot available
            }
        }

        public void incrementProgress(AchievementType achievement, int amount) {
            progressValues.merge(achievement, amount, Integer::sum);
            
            // Check if progress threshold is met (simplified logic)
            checkUnlock(achievement);
        }

        private void checkUnlock(AchievementType achievement) {
            // TODO: Implement specific unlock conditions per achievement
            switch (achievement) {
                case DEFEAT_BOSS -> {
                    if (progressValues.getOrDefault(achievement, 0) >= 1) {
                        unlock(achievement);
                    }
                }
                default -> {}
            }
        }

        public Map<AchievementType, Boolean> getUnlockedAchievements() {
            return new HashMap<>(unlockedAchievements);
        }
    }

    /**
     * Get or create player progress tracking.
     */
    private static PlayerProgress getPlayerProgress(Player player) {
        String playerId = player.getStringUUID().toString();
        return PLAYER_PROGRESSES.computeIfAbsent(playerId, k -> new PlayerProgress());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getPlayer();
        // TODO: Add tick-based progress tracking (e.g., time spent in biomes)
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.boss.Boss || 
            event.getSource().getDirectEntity() instanceof net.minecraft.world.entity.player.Player) {
            
            Player player = (Player) event.getSource().getDirectEntity();
            if (player != null) {
                PlayerProgress progress = getPlayerProgress(player);
                progress.incrementProgress(AchievementType.DEFEAT_BOSS, 1);
                
                // TODO: Check for specific boss kills (Ender Dragon, Wither, etc.)
            }
        }
    }

    /**
     * Reset player progress (for testing or world reset).
     */
    public static void resetPlayerProgress(Player player) {
        String playerId = player.getStringUUID().toString();
        PLAYER_PROGRESSES.remove(playerId);
    }
}
