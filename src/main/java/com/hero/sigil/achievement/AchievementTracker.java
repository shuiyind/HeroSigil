package com.hero.sigil.achievement;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.BossDisplayData;
import net.minecraft.world.entity.boss.WitherBoss;
import net.minecraft.world.entity.monster.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tracks player progress towards achievements.
 * Each achievement unlocks a buff slot on the Hero Sigil.
 */
@EventBusSubscriber(modid = HeroSigil.MODID, bus = EventBusSubscriber.Bus.GAME)
public class AchievementTracker {

    // Store achievement progress per player
    private static final Map<String, PlayerProgress> PLAYER_PROGRESSES = new HashMap<>();
    
    // Track biome exploration per player (key: playerId -> set of biomes visited)
    private static final Map<String, Set<ResourceKey<net.minecraft.world.level.Level>>> BIOME_VISITS = new HashMap<>();

    /**
     * Achievements that unlock buff slots on the Hero Sigil.
     */
    public enum AchievementType {
        DEFEAT_BOSS(0),                    // Unlock: Saturation (slot 1)
        EXPLORE_ALL_BIOMES(1),             // Unlock: Health Boost (slot 2)
        DEFEAT_ENDER_DRAGON(2),            // Unlock: Speed (slot 3)
        BUILD_REDSTONE_MACHINE(3),         // Unlock: Strength (unlocked by default for now)
        COMPLETE_COLLECTION(4);            // Unlock: Night Vision (unlocked by default for now)

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
                // Notify player of new buff slot available
                notifyPlayerUnlock(achievement);
            }
        }

        private void notifyPlayerUnlock(AchievementType achievement) {
            // TODO: Send packet to client to show notification
        }

        public void incrementProgress(AchievementType achievement, int amount) {
            progressValues.merge(achievement, amount, Integer::sum);
            
            // Check if progress threshold is met (simplified logic)
            checkUnlock(achievement);
        }

        private void checkUnlock(AchievementType achievement) {
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
        
        // Track biome exploration for EXPLORE_ALL_BIOMES achievement
        if (player instanceof ServerPlayer serverPlayer) {
            String playerId = player.getStringUUID().toString();
            
            // Get current biome key
            ResourceKey<net.minecraft.world.level.Level> currentBiome = 
                player.level().getBiome(player.blockPosition()).is(Registries.BIOME);
            
            if (currentBiome != null) {
                BIOME_VISITS.computeIfAbsent(playerId, k -> new HashSet<>()).add(currentBiome);
                
                // Check if all biomes are explored (simplified check - in production would use a defined list)
                PlayerProgress progress = getPlayerProgress(player);
                Set<ResourceKey<net.minecraft.world.level.Level>> visitedBiomes = BIOME_VISITS.get(playerId);
                
                // For now, unlock after visiting 20 different biomes as a reasonable threshold
                if (visitedBiomes.size() >= 20 && !progress.isUnlocked(AchievementType.EXPLORE_ALL_BIOMES)) {
                    progress.unlock(AchievementType.EXPLORE_ALL_BIOMES);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getDirectEntity() instanceof Player player) {
            PlayerProgress progress = getPlayerProgress(player);
            
            // Check for boss kills (any entity with BossDisplayData or specific boss types)
            boolean isBoss = event.getEntity().getType().getDefaultRegistryName().getPath().contains("boss") ||
                            event.getEntity().hasCustomName() && 
                            event.getEntity().getDisplayName().getString().contains("Boss");
            
            // Also check for Wither and Ender Dragon specifically
            if (event.getEntity() instanceof WitherBoss) {
                isBoss = true;
                
                // Unlock DEFEAT_BOSS achievement
                progress.incrementProgress(AchievementType.DEFEAT_BOSS, 1);
                
                // Check if this was the final boss to unlock additional rewards
                if (!progress.isUnlocked(AchievementType.DEFEAT_ENDER_DRAGON)) {
                    // In a full implementation, we'd track which bosses have been defeated
                }
            } else if (event.getEntity() instanceof EnderDragon) {
                isBoss = true;
                
                // Unlock DEFEAT_BOSS achievement
                progress.incrementProgress(AchievementType.DEFEAT_BOSS, 1);
                
                // Specifically unlock ENDER_DRAGON achievement
                if (!progress.isUnlocked(AchievementType.DEFEAT_ENDER_DRAGON)) {
                    progress.unlock(AchievementType.DEFEAT_ENDER_DRAGON);
                    
                    // Also count as a boss kill for the general DEFEAT_BOSS achievement
                    if (progress.progressValues.getOrDefault(AchievementType.DEFEAT_BOSS, 0) < 1) {
                        progress.incrementProgress(AchievementType.DEFEAT_BOSS, 1);
                    }
                }
            } else if (isBoss && event.getEntity().getType() != EntityType.ENDER_DRAGON) {
                // Generic boss kill tracking
                progress.incrementProgress(AchievementType.DEFEAT_BOSS, 1);
                
                // Unlock DEFEAT_BOSS achievement after first valid boss kill
                if (!progress.isUnlocked(AchievementType.DEFEAT_BOSS)) {
                    progress.unlock(AchievementType.DEFEAT_BOSS);
                }
            }
        }
    }

    /**
     * Check for specific entity types that count as bosses.
     */
    private static boolean isBossEntity(net.minecraft.world.entity.Entity entity) {
        return switch (entity.getType().getDefaultRegistryName().getPath()) {
            case "ender_dragon", "wither" -> true;
            default -> false;
        };
    }

    /**
     * Reset player progress (for testing or world reset).
     */
    public static void resetPlayerProgress(Player player) {
        String playerId = player.getStringUUID().toString();
        PLAYER_PROGRESSES.remove(playerId);
        BIOME_VISITS.remove(playerId);
    }

    /**
     * Get all unlocked achievements for a player.
     */
    public static Map<AchievementType, Boolean> getPlayerUnlockedAchievements(Player player) {
        PlayerProgress progress = PLAYER_PROGRESSES.get(player.getStringUUID().toString());
        return progress != null ? progress.getUnlockedAchievements() : new HashMap<>();
    }

    /**
     * Check if a specific achievement is unlocked for the player.
     */
    public static boolean isAchievementUnlocked(Player player, AchievementType achievement) {
        PlayerProgress progress = PLAYER_PROGRESSES.get(player.getStringUUID().toString());
        return progress != null && progress.isUnlocked(achievement);
    }

    /**
     * Force unlock an achievement for testing purposes.
     */
    public static void forceUnlockAchievement(Player player, AchievementType achievement) {
        PlayerProgress progress = PLAYER_PROGRESSES.computeIfAbsent(player.getStringUUID().toString(), k -> new PlayerProgress());
        progress.unlock(achievement);
    }

    /**
     * Register a /herosigil command for testing achievements.
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // Example: /herosigil unlock <achievement_type>
        // This allows testers to quickly test different buff slots
        // Implementation would use com.mojang.brigadier.CommandDispatcher
    }

    /**
     * Get the number of unlocked achievements for a player.
     */
    public static int getUnlockedCount(Player player) {
        PlayerProgress progress = PLAYER_PROGRESSES.get(player.getStringUUID().toString());
        if (progress == null) return 0;
        
        long count = progress.getUnlockedAchievements().values().stream()
            .filter(Boolean::booleanValue)
            .count();
        return (int) count;
    }

    /**
     * Get the maximum number of buff slots available based on achievements.
     */
    public static int getMaxBuffSlots(Player player) {
        // Each unlocked achievement unlocks one slot, max 3 slots for now
        return Math.min(getUnlockedCount(player), 3);
    }

    /**
     * Get the current biome visited count for a player (for debugging/testing).
     */
    public static int getBiomeVisitCount(Player player) {
        String playerId = player.getStringUUID().toString();
        Set<ResourceKey<net.minecraft.world.level.Level>> biomes = BIOME_VISITS.get(playerId);
        return biomes != null ? biomes.size() : 0;
    }

    /**
     * Clear all biome visit data (for testing).
     */
    public static void clearBiomeVisits(Player player) {
        String playerId = player.getStringUUID().toString();
        BIOME_VISITS.remove(playerId);
    }
}
