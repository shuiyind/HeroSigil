package com.hero.sigil.achievement;

import com.hero.sigil.HeroSigil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
    private static final Map<String, Set<ResourceKey<net.minecraft.world.level.biome.Biome>>> BIOME_VISITS = new HashMap<>();

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

        // Boss 击杀计数
        private int bossKillCount = 0;

        // 保存玩家引用用于通知和动画效果
        private Player currentPlayer;

        // 成就历史记录
        private final List<AchievementType> achievementHistory = new ArrayList<>();

        public boolean isUnlocked(AchievementType achievement) {
            return unlockedAchievements.getOrDefault(achievement, false);
        }

        /**
         * 解锁成就
         */
        public void unlock(AchievementType achievement, Player player) {
            this.currentPlayer = player;
            if (!isUnlocked(achievement)) {
                unlockedAchievements.put(achievement, true);
                achievementHistory.add(achievement);
                notifyPlayerUnlock(achievement);
            }
        }

        /**
         * 通知玩家成就已解锁
         */
        private void notifyPlayerUnlock(AchievementType achievement) {
            if (currentPlayer != null) {
                String achievementName = getAchievementName(achievement);

                // 发送中文通知
                currentPlayer.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("§6§l成就解锁§r§f: " + achievementName + " - 新的 buff 槽位已解锁！")
                );

                // 播放 buff 解锁音效
                currentPlayer.level().playSound(
                    null,
                    currentPlayer.getBlockX(),
                    currentPlayer.getBlockY(),
                    currentPlayer.getBlockZ(),
                    com.hero.sigil.registry.ModRegistries.BUFF_UNLOCK.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    1.0f,
                    1.0f
                );

                // 播放成就完成动画
                playAchievementCompleteAnimation(currentPlayer, achievement);
            }
        }

        public void incrementProgress(AchievementType achievement, int amount, Player player) {
            progressValues.merge(achievement, amount, Integer::sum);

            // Check if progress threshold is met (simplified logic)
            checkUnlock(achievement, player);
        }

        private void checkUnlock(AchievementType achievement, Player player) {
            switch (achievement) {
                case DEFEAT_BOSS -> {
                    if (progressValues.getOrDefault(achievement, 0) >= 1) {
                        unlock(achievement, player);
                    }
                }
                default -> {}
            }
        }

        /**
         * 播放成就完成动画（金色粒子效果 + 音效）
         */
        private void playAchievementCompleteAnimation(Player player, AchievementType achievement) {
            // 播放金色粒子效果
            for (int i = 0; i < 20; i++) {
                double offsetX = (Math.random() - 0.5) * 2;
                double offsetY = Math.random() * 2;
                double offsetZ = (Math.random() - 0.5) * 2;

                player.level().addParticle(
                    net.minecraft.core.particles.ParticleTypes.NOTE,
                    player.getX() + offsetX,
                    player.getY() + offsetY,
                    player.getZ() + offsetZ,
                    1.0, 0.0, 0.0
                );
            }

            // 播放成就授予音效
            player.level().playSound(
                null,
                player.getBlockX(),
                player.getBlockY(),
                player.getBlockZ(),
                net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                net.minecraft.sounds.SoundSource.PLAYERS,
                1.0f,
                1.0f
            );
        }

        public void incrementBossKillCount() {
            bossKillCount++;
        }

        public int getBossKillCount() {
            return bossKillCount;
        }

        public Map<AchievementType, Boolean> getUnlockedAchievements() {
            return new HashMap<>(unlockedAchievements);
        }

        /**
         * 获取成就历史
         */
        public List<AchievementType> getAchievementHistory() {
            return new ArrayList<>(achievementHistory);
        }

        /**
         * 显示成就历史
         */
        public void showAchievementHistory(Player player) {
            if (achievementHistory.isEmpty()) {
                player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("§e§l勇者之证§r§f: 尚未解锁任何成就")
                );
                return;
            }

            player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal("§6§l已解锁的成就§r§f:")
            );

            for (AchievementType achievement : achievementHistory) {
                String name = getAchievementName(achievement);
                player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("  §a✓§r §f" + name)
                );
            }
        }

        /**
         * 获取成就名称
         */
        private String getAchievementName(AchievementType achievement) {
            return switch (achievement) {
                case DEFEAT_BOSS -> "击败 Boss";
                case EXPLORE_ALL_BIOMES -> "探索世界";
                case DEFEAT_ENDER_DRAGON -> "击败末影龙";
                case BUILD_REDSTONE_MACHINE -> "建造红石机器";
                case COMPLETE_COLLECTION -> "完成收集";
                default -> "未知成就";
            };
        }
    }

    /**
     * Get or create player progress tracking.
     */
    private static PlayerProgress getPlayerProgress(Player player) {
        String playerId = player.getUUID().toString();
        return PLAYER_PROGRESSES.computeIfAbsent(playerId, k -> new PlayerProgress());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();

        // Track biome exploration for EXPLORE_ALL_BIOMES achievement
        if (player instanceof ServerPlayer serverPlayer) {
            // 每 100 tick (5 秒) 记录一次群系，而非每个 tick
            if (player.tickCount % 100 != 0) {
                return;
            }

            String playerId = player.getUUID().toString();

            // 已访问 50 个群系后停止记录（超过 20 已解锁成就）
            Set<ResourceKey<net.minecraft.world.level.biome.Biome>> visitedBiomes = BIOME_VISITS.get(playerId);
            if (visitedBiomes != null && visitedBiomes.size() >= 50) {
                return;
            }

            // 获取当前群系（使用 unwrapKey() 获取 ResourceKey<Biome>）
            var biomeHolder = player.level().getBiome(player.blockPosition());
            var currentBiome = biomeHolder.unwrapKey().orElse(null);

            if (currentBiome != null) {
                boolean isNewBiome = visitedBiomes == null || !visitedBiomes.contains(currentBiome);

                if (isNewBiome) {
                    BIOME_VISITS.computeIfAbsent(playerId, k -> new HashSet<>()).add(currentBiome);

                    // 更新进度提示变量（添加新群系后的数量）
                    int progressCount = visitedBiomes.size();

                    // 每访问 5 个新群系，发送一次进度提示
                    if (progressCount % 5 == 0 && progressCount < 20) {
                        int percentage = (int) ((double) progressCount / 20 * 100);
                        player.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal(
                                "§b§l群系探索§r§f: " + progressCount + "/20 (" + percentage + "%)"
                            )
                        );
                    }

                    // 检查是否解锁成就
                    PlayerProgress progress = getPlayerProgress(player);

                    // 访问 20 个不同群系后解锁 EXPLORE_ALL_BIOMES 成就
                    if (progressCount >= 20 && !progress.isUnlocked(AchievementType.EXPLORE_ALL_BIOMES)) {
                        progress.unlock(AchievementType.EXPLORE_ALL_BIOMES, player);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        String playerId = event.getEntity().getUUID().toString();

        // 清理群系访问数据
        BIOME_VISITS.remove(playerId);

        // 清理成就进度数据
        PLAYER_PROGRESSES.remove(playerId);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getDirectEntity() instanceof Player player) {
            PlayerProgress progress = getPlayerProgress(player);

            // 使用 isBossEntity() 方法检测 Boss
            if (isBossEntity(event.getEntity())) {
                String bossName = event.getEntity().getDisplayName().getString();

                // 增加 Boss 击杀计数
                progress.incrementBossKillCount();
                progress.incrementProgress(AchievementType.DEFEAT_BOSS, 1, player);

                // 特殊处理末影龙
                if (event.getEntity() instanceof EnderDragon) {
                    if (!progress.isUnlocked(AchievementType.DEFEAT_ENDER_DRAGON)) {
                        progress.unlock(AchievementType.DEFEAT_ENDER_DRAGON, player);

                        // 发送中文通知
                        player.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal("§e§l成就解锁§r§f: 成功击败末影龙！新的 buff 槽位已解锁。")
                        );
                        HeroSigil.LOGGER.info("Player {} defeated Ender Dragon", player.getScoreboardName());
                    } else {
                        // 发送击杀通知
                        player.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal("§c§lBoss 击杀§r§f: 成功击败 " + bossName + "！")
                        );
                    }
                } else {
                    // 发送其他 Boss 击杀通知
                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal("§c§lBoss 击杀§r§f: 成功击败 " + bossName + "！")
                    );
                }
            }
        }
    }

    /**
     * 检查实体是否为 Boss 类型
     */
    private static boolean isBossEntity(net.minecraft.world.entity.Entity entity) {
        // 方法 1: 检查实体类型是否为已知的 Boss
        if (entity instanceof WitherBoss || entity instanceof EnderDragon) {
            return true;
        }

        // 方法 2: 检查实体 ID 路径
        String path = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
        return switch (path) {
            case "ender_dragon", "wither", "warden", "elder_guardian" -> true;
            default -> false;
        };
    }

    /**
     * Reset player progress (for testing or world reset).
     */
    public static void resetPlayerProgress(Player player) {
        String playerId = player.getUUID().toString();
        PLAYER_PROGRESSES.remove(playerId);
        BIOME_VISITS.remove(playerId);
    }

    /**
     * Get all unlocked achievements for a player.
     */
    public static Map<AchievementType, Boolean> getPlayerUnlockedAchievements(Player player) {
        PlayerProgress progress = PLAYER_PROGRESSES.get(player.getUUID().toString());
        return progress != null ? progress.getUnlockedAchievements() : new HashMap<>();
    }

    /**
     * Check if a specific achievement is unlocked for the player.
     */
    public static boolean isAchievementUnlocked(Player player, AchievementType achievement) {
        PlayerProgress progress = PLAYER_PROGRESSES.get(player.getUUID().toString());
        return progress != null && progress.isUnlocked(achievement);
    }

    /**
     * Force unlock an achievement for testing purposes.
     */
    public static void forceUnlockAchievement(Player player, AchievementType achievement) {
        PlayerProgress progress = PLAYER_PROGRESSES.computeIfAbsent(player.getUUID().toString(), k -> new PlayerProgress());
        progress.unlock(achievement, player);
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
        PlayerProgress progress = PLAYER_PROGRESSES.get(player.getUUID().toString());
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
     * 获取当前群系访问计数（用于调试/测试）
     */
    public static int getBiomeVisitCount(Player player) {
        String playerId = player.getUUID().toString();
        Set<ResourceKey<net.minecraft.world.level.biome.Biome>> biomes = BIOME_VISITS.get(playerId);
        return biomes != null ? biomes.size() : 0;
    }

    /**
     * Get the boss kill count for a player.
     */
    public static int getBossKillCount(Player player) {
        PlayerProgress progress = PLAYER_PROGRESSES.get(player.getUUID().toString());
        return progress != null ? progress.getBossKillCount() : 0;
    }

    /**
     * Get the number of distinct biomes visited by a player.
     */
    public static int getExploredBiomeCount(Player player) {
        Set<ResourceKey<net.minecraft.world.level.biome.Biome>> biomes = BIOME_VISITS.get(player.getUUID().toString());
        return biomes != null ? biomes.size() : 0;
    }

    /**
     * Total biomes required to unlock the exploration achievement.
     */
    public static int getTotalBiomeCount(Player player) {
        return 20;
    }

    /**
     * 显示玩家的成就历史
     */
    public static void showAchievements(Player player) {
        PlayerProgress progress = PLAYER_PROGRESSES.get(player.getUUID().toString());
        if (progress != null) {
            progress.showAchievementHistory(player);
        } else {
            player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal("§e§l勇者之证§r§f: 尚未检测到您的成就数据")
            );
        }
    }

    /**
     * Clear all biome visit data (for testing).
     */
    public static void clearBiomeVisits(Player player) {
        String playerId = player.getUUID().toString();
        BIOME_VISITS.remove(playerId);
    }
}
