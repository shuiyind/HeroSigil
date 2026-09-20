package com.hero.sigil.buffs;

import com.hero.sigil.HeroSigil;
import com.hero.sigil.achievement.AchievementTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * Manages Hero Sigil buff data for a player.
 * This class handles saving/loading buff states and managing active buffs.
 */
public class HeroSigilData {

    private static final String TAG_KEY = "HeroSigil";
    public static final int CURRENT_DATA_VERSION = 1;
    private static final String VERSION_TAG = "dataVersion";

    // 数据备份相关常量
    private static final String BACKUP_TAG = "HeroSigil_backup";
    private static final int MAX_BACKUPS = 3;

    // List of all available buff effects
    private static java.util.List<BuffEffect> ALL_BUFFS;

    /**
     * Initialize the list of available buff effects.
     */
    public static void initBuffs() {
        if (ALL_BUFFS == null) {
            ALL_BUFFS = BuffEffect.createAllBuffs();
        }
    }

    /**
     * Get all available buffs, initialized on first call.
     */
    public static java.util.List<BuffEffect> getAllBuffs() {
        initBuffs();
        return ALL_BUFFS;
    }

    // ==================== 数据校验与备份 ====================

    /**
     * 检查 NBT 数据完整性
     * @return 如果数据有效返回 true，否则返回 false
     */
    public static boolean validateData(CompoundTag sigilData) {
        // 检查版本号
        if (!sigilData.contains(VERSION_TAG)) {
            HeroSigil.LOGGER.warn("Missing version tag in Hero Sigil data");
            return false;
        }

        int version = sigilData.getInt(VERSION_TAG);
        if (version < 0 || version > CURRENT_DATA_VERSION + 1) {
            HeroSigil.LOGGER.warn("Invalid version {} in Hero Sigil data", version);
            return false;
        }

        // 检查必需槽位数据
        boolean hasAnySlot = false;
        for (int i = 1; i <= 3; i++) {
            String slotKey = "slot_" + i;
            if (sigilData.contains(slotKey)) {
                CompoundTag slotData = sigilData.getCompound(slotKey);

                // 检查必需字段
                if (!slotData.contains("unlocked") || !slotData.contains("active")) {
                    HeroSigil.LOGGER.warn("Missing required fields in slot_{}", i);
                    return false;
                }

                // 验证字段类型
                if (slotData.get("unlocked") != null && slotData.get("active") != null) {
                    hasAnySlot = true;
                }
            }
        }

        if (!hasAnySlot) {
            HeroSigil.LOGGER.warn("No valid slot data found in Hero Sigil data");
            return false;
        }

        return true;
    }

    /**
     * 创建当前数据的备份
     */
    private static void createBackup(CompoundTag persistData) {
        CompoundTag sigilData = persistData.getCompound(TAG_KEY);

        // 检查是否有现有备份
        int backupCount = 0;
        if (persistData.contains(BACKUP_TAG)) {
            CompoundTag backup = persistData.getCompound(BACKUP_TAG);
            backupCount = backup.getInt("count");
        }

        // 如果备份数量达到上限，删除最旧的备份
        if (backupCount >= MAX_BACKUPS) {
            HeroSigil.LOGGER.info("Maximum backup count reached, removing oldest backup");
            persistData.remove(BACKUP_TAG);
            backupCount = 0;
        }

        // 创建新备份
        CompoundTag backupData = new CompoundTag();
        backupData.putInt("timestamp", (int) System.currentTimeMillis());
        backupData.putInt("version", sigilData.getInt(VERSION_TAG));
        backupData.put("data", sigilData.copy());

        // 更新备份计数
        backupData.putInt("count", backupCount + 1);

        persistData.put(BACKUP_TAG, backupData);

        HeroSigil.LOGGER.info("Created backup {} for player data", backupCount + 1);
    }

    /**
     * 从备份恢复数据
     */
    public static boolean restoreFromBackup(Player player, CompoundTag persistData) {
        if (!persistData.contains(BACKUP_TAG)) {
            HeroSigil.LOGGER.warn("No backup available for player {}", player.getScoreboardName());
            return false;
        }

        CompoundTag backupData = persistData.getCompound(BACKUP_TAG);
        if (!backupData.contains("data")) {
            HeroSigil.LOGGER.warn("Backup data is invalid for player {}", player.getScoreboardName());
            return false;
        }

        // 备份当前数据（作为新备份保存）
        createBackup(persistData);

        // 恢复备份数据
        CompoundTag sigilData = backupData.getCompound("data");
        persistData.put(TAG_KEY, sigilData);

        HeroSigil.LOGGER.info("Restored data from backup for player {}", player.getScoreboardName());

        // 通知玩家
        player.sendSystemMessage(
            net.minecraft.network.chat.Component.literal("§e§l勇者之证§r§f: 数据已从备份恢复")
        );

        return true;
    }

    // ==================== 数据一致性检查与修复 ====================

    /**
     * 检查 buff 状态与 NBT 数据是否一致
     * @return 如果一致返回 true，否则返回 false
     */
    public static boolean checkDataConsistency(ServerPlayer player) {
        CompoundTag persistData = player.getPersistentData();
        if (!persistData.contains(TAG_KEY)) {
            return false;
        }

        CompoundTag sigilData = persistData.getCompound(TAG_KEY);
        java.util.List<BuffEffect> buffs = getAllBuffs();

        for (int i = 0; i < Math.min(buffs.size(), 3); i++) {
            BuffEffect buff = buffs.get(i);
            String slotKey = "slot_" + (i + 1);

            if (sigilData.contains(slotKey)) {
                CompoundTag slotData = sigilData.getCompound(slotKey);
                boolean unlockedFromNBT = slotData.getBoolean("unlocked");
                boolean activeFromNBT = slotData.getBoolean("active");

                // 检查一致性
                if (buff.isUnlocked() != unlockedFromNBT || buff.isActive() != activeFromNBT) {
                    HeroSigil.LOGGER.warn("Data inconsistency detected for slot {} in player {}: " +
                        "NBT={}, Buff={}, NBT_active={}, Buff_active={}",
                        i + 1, player.getScoreboardName(), unlockedFromNBT, buff.isUnlocked(),
                        activeFromNBT, buff.isActive());
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 自动修复数据不一致
     */
    public static void fixDataInconsistency(ServerPlayer player) {
        CompoundTag persistData = player.getPersistentData();
        if (!persistData.contains(TAG_KEY)) {
            return;
        }

        CompoundTag sigilData = persistData.getCompound(TAG_KEY);
        java.util.List<BuffEffect> buffs = getAllBuffs();
        boolean changed = false;

        for (int i = 0; i < Math.min(buffs.size(), 3); i++) {
            BuffEffect buff = buffs.get(i);
            String slotKey = "slot_" + (i + 1);

            if (sigilData.contains(slotKey)) {
                CompoundTag slotData = sigilData.getCompound(slotKey);
                boolean unlockedFromNBT = slotData.getBoolean("unlocked");
                boolean activeFromNBT = slotData.getBoolean("active");

                // 修复不一致
                if (buff.isUnlocked() != unlockedFromNBT || buff.isActive() != activeFromNBT) {
                    slotData.putBoolean("unlocked", buff.isUnlocked());
                    slotData.putBoolean("active", buff.isActive());
                    changed = true;

                    HeroSigil.LOGGER.info("Fixed data inconsistency for slot {} in player {}",
                        i + 1, player.getScoreboardName());
                }
            }
        }

        if (changed) {
            persistData.put(TAG_KEY, sigilData);
            HeroSigil.LOGGER.info("Data inconsistency fixed for player {}", player.getScoreboardName());

            player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal("§e§l勇者之证§r§f: 数据不一致已自动修复")
            );
        }
    }

    /**
     * Save buff states to player NBT data.
     */
    public static void onSave(Player player, CompoundTag nbt) {
        CompoundTag sigilData = new CompoundTag();

        // 保存版本号
        sigilData.putInt(VERSION_TAG, CURRENT_DATA_VERSION);

        java.util.List<BuffEffect> buffs = getAllBuffs();
        for (int i = 0; i < Math.min(buffs.size(), 3); i++) { // Max 3 slots
            BuffEffect buff = buffs.get(i);
            String slotKey = "slot_" + (i + 1);

            CompoundTag slotData = new CompoundTag();
            slotData.putBoolean("unlocked", buff.isUnlocked());
            slotData.putBoolean("active", buff.isActive());
            sigilData.put(slotKey, slotData);
        }

        nbt.put(TAG_KEY, sigilData);
    }

    /**
     * Load buff states from player NBT data.
     */
    public static void onLoad(Player player, CompoundTag nbt) {
        if (nbt.contains(TAG_KEY)) {
            CompoundTag sigilData = nbt.getCompound(TAG_KEY);

            // 获取并处理版本号
            int dataVersion = sigilData.getInt(VERSION_TAG);
            if (dataVersion == 0) {
                // 旧版本数据没有版本号，默认为 v1
                dataVersion = 1;
                sigilData.putInt(VERSION_TAG, 1);
            }
            HeroSigil.LOGGER.info("Loading Hero Sigil data version {} for player {}", dataVersion, player.getScoreboardName());

            // 检查数据完整性
            if (!validateData(sigilData)) {
                HeroSigil.LOGGER.warn("Hero Sigil data is corrupted for player {}, attempting recovery",
                    player.getScoreboardName());

                // 尝试从备份恢复
                if (!restoreFromBackup(player, nbt)) {
                    // 如果没有备份，创建默认数据
                    HeroSigil.LOGGER.info("No backup available, creating default data for player {}",
                        player.getScoreboardName());
                    sigilData = new CompoundTag();
                    sigilData.putInt(VERSION_TAG, CURRENT_DATA_VERSION);

                    // 创建默认槽位数据
                    for (int i = 1; i <= 3; i++) {
                        CompoundTag slotData = new CompoundTag();
                        slotData.putBoolean("unlocked", false);
                        slotData.putBoolean("active", false);
                        sigilData.put("slot_" + i, slotData);
                    }

                    nbt.put(TAG_KEY, sigilData);
                } else {
                    // 重新加载恢复后的数据
                    sigilData = nbt.getCompound(TAG_KEY);
                    // 重新获取恢复后的版本号
                    dataVersion = sigilData.getInt(VERSION_TAG);
                    if (dataVersion == 0) {
                        dataVersion = 1;
                        sigilData.putInt(VERSION_TAG, 1);
                    }
                }
            }

            // 数据迁移
            if (dataVersion < CURRENT_DATA_VERSION) {
                sigilData = migrateData(dataVersion, sigilData, player);
            } else if (dataVersion > CURRENT_DATA_VERSION) {
                HeroSigil.LOGGER.warn("Player data version {} is newer than current version {}, may be incompatible",
                    dataVersion, CURRENT_DATA_VERSION);
            }

            // 加载 buff 状态
            java.util.List<BuffEffect> buffs = getAllBuffs();
            for (int i = 0; i < Math.min(buffs.size(), 3); i++) { // Max 3 slots
                BuffEffect buff = buffs.get(i);
                String slotKey = "slot_" + (i + 1);

                if (sigilData.contains(slotKey)) {
                    CompoundTag slotData = sigilData.getCompound(slotKey);
                    buff.setUnlocked(slotData.getBoolean("unlocked"));
                    buff.setActive(slotData.getBoolean("active"));

                    // Re-apply active buffs after loading
                    if (buff.isActive() && buff.isUnlocked()) {
                        buff.applyBuff(player);
                    }
                }
            }

            // 更新为最新版本
            sigilData.putInt(VERSION_TAG, CURRENT_DATA_VERSION);

            HeroSigil.LOGGER.info("Successfully loaded Hero Sigil data for player {}", player.getScoreboardName());
        } else {
            // First time - apply default unlocks based on achievements
            applyDefaultBuffsForPlayer(player);
        }
    }

    /**
     * 根据旧版本迁移数据到当前版本（按版本逐步迁移）
     */
    private static CompoundTag migrateData(int oldVersion, CompoundTag data, Player player) {
        HeroSigil.LOGGER.info("Migrating Hero Sigil data from version {} to {} for player {}",
            oldVersion, CURRENT_DATA_VERSION, player.getScoreboardName());

        // 按版本逐步迁移（确保每个中间版本都被正确处理）
        if (oldVersion < CURRENT_DATA_VERSION) {
            for (int version = oldVersion; version < CURRENT_DATA_VERSION; version++) {
                data = migrateFromVersion(version, data, player);
            }
        }

        // 迁移完成后更新版本号
        data.putInt(VERSION_TAG, CURRENT_DATA_VERSION);

        HeroSigil.LOGGER.info("Data migration completed successfully for player {}", player.getScoreboardName());
        return data;
    }

    /**
     * 从指定版本迁移到下一个版本
     */
    private static CompoundTag migrateFromVersion(int fromVersion, CompoundTag data, Player player) {
        long startTime = System.currentTimeMillis();

        try {
            CompoundTag result = switch (fromVersion) {
                case 1 -> migrateFromV1ToV2(data, player);
                case 2 -> migrateFromV2ToV3(data, player);
                default -> {
                    HeroSigil.LOGGER.warn("Unknown migration path from version {}, keeping data as-is", fromVersion);
                    yield data;
                }
            };

            long duration = System.currentTimeMillis() - startTime;
            HeroSigil.LOGGER.info("Migration from v{} completed in {}ms for player {}",
                fromVersion, duration, player.getScoreboardName());

            return result;
        } catch (Exception e) {
            HeroSigil.LOGGER.error("Error during migration from v{} for player {}: {}",
                fromVersion, player.getScoreboardName(), e.getMessage());
            // 返回原始数据，避免数据丢失
            return data;
        }
    }

    /**
     * 从 v1 迁移到 v2: 添加新 buff 槽位支持
     */
    private static CompoundTag migrateFromV1ToV2(CompoundTag data, Player player) {
        HeroSigil.LOGGER.info("Migrating from v1 to v2: Adding support for additional buff slots");

        // 检查现有槽位数据
        int existingSlots = 0;
        for (int i = 1; i <= 3; i++) {
            String slotKey = "slot_" + i;
            if (data.contains(slotKey)) {
                existingSlots = i;
            }
        }

        // v2 支持 5 个槽位，如果现有 3 个槽位，创建新的空槽位
        if (existingSlots >= 3) {
            for (int i = existingSlots + 1; i <= 5; i++) {
                String slotKey = "slot_" + i;
                if (!data.contains(slotKey)) {
                    CompoundTag slotData = new CompoundTag();
                    slotData.putBoolean("unlocked", false);
                    slotData.putBoolean("active", false);
                    data.put(slotKey, slotData);

                    HeroSigil.LOGGER.info("Created new empty buff slot {} for player {}", i, player.getScoreboardName());
                }
            }
        }

        // 更新版本号
        data.putInt(VERSION_TAG, 2);

        HeroSigil.LOGGER.info("Migration v1 -> v2 completed for player {}", player.getScoreboardName());
        return data;
    }

    /**
     * 从 v2 迁移到 v3: Buff ID 重新映射
     */
    private static CompoundTag migrateFromV2ToV3(CompoundTag data, Player player) {
        HeroSigil.LOGGER.info("Migrating from v2 to v3: Remapping buff IDs for player {}", player.getScoreboardName());

        // 定义旧 ID 到新 ID 的映射
        java.util.Map<String, String> buffIdMapping = new java.util.HashMap<>();
        buffIdMapping.put("saturation", "food_saver");      // 示例：重命名
        buffIdMapping.put("health_boost", "vitality");       // 示例：重命名
        buffIdMapping.put("speed", "swiftness");             // 示例：重命名

        // 迁移每个槽位的 buff ID
        for (int i = 1; i <= 3; i++) {
            String slotKey = "slot_" + i;
            if (data.contains(slotKey)) {
                CompoundTag slotData = data.getCompound(slotKey);

                // 如果需要迁移 buff ID，可以在这里添加逻辑
                // 例如：slotData.putString("buffId", buffIdMapping.get(slotData.getString("buffId")))
            }
        }

        // 更新版本号
        data.putInt(VERSION_TAG, 3);

        HeroSigil.LOGGER.info("Migration v2 -> v3 completed for player {}", player.getScoreboardName());
        return data;
    }

    // ==================== 回滚方法 ====================

    /**
     * 回滚数据到指定版本（用于测试或修复）
     */
    public static CompoundTag rollbackData(CompoundTag data, int targetVersion) {
        HeroSigil.LOGGER.info("Rolling back data to version {}", targetVersion);

        int currentVersion = data.getInt(VERSION_TAG);
        if (currentVersion <= targetVersion) {
            HeroSigil.LOGGER.info("Data is already at or before target version");
            return data;
        }

        // 逐步降级
        for (int version = currentVersion; version > targetVersion; version--) {
            data = rollbackToVersion(version, data);
        }

        data.putInt(VERSION_TAG, targetVersion);
        HeroSigil.LOGGER.info("Rollback to v{} completed", targetVersion);

        return data;
    }

    /**
     * 从指定版本回滚到前一个版本
     */
    private static CompoundTag rollbackToVersion(int fromVersion, CompoundTag data) {
        switch (fromVersion) {
            case 2:
                return rollbackFromV2ToV1(data);
            case 3:
                return rollbackFromV3ToV2(data);
            default:
                HeroSigil.LOGGER.warn("Unknown rollback path from version {}", fromVersion);
                return data;
        }
    }

    private static CompoundTag rollbackFromV2ToV1(CompoundTag data) {
        HeroSigil.LOGGER.info("Rolling back from v2 to v1: Removing additional buff slots");

        // 移除 v2 新增的槽位
        for (int i = 4; i <= 5; i++) {
            String slotKey = "slot_" + i;
            data.remove(slotKey);
        }

        data.putInt(VERSION_TAG, 1);
        return data;
    }

    private static CompoundTag rollbackFromV3ToV2(CompoundTag data) {
        HeroSigil.LOGGER.info("Rolling back from v3 to v2: Reverting buff ID mappings");

        // v3 的 buff ID 映射是单向的，回滚时保留原始数据
        // 如果需要完全回滚，可以在这里添加逻辑恢复原始 ID

        data.putInt(VERSION_TAG, 2);
        return data;
    }

    /**
     * Get the number of unlocked slots for a player.
     */
    public static int getMaxBuffSlots(Player player) {
        return com.hero.sigil.achievement.AchievementTracker.getMaxBuffSlots(player);
    }

    /**
     * Apply default buffs based on current achievement progress.
     */
    private static void applyDefaultBuffsForPlayer(Player player) {
        java.util.List<BuffEffect> buffs = getAllBuffs();

        int unlockedCount = com.hero.sigil.achievement.AchievementTracker.getUnlockedCount(player);

        // Unlock slots based on achievements (max 3)
        for (int i = 0; i < Math.min(unlockedCount, buffs.size()); i++) {
            buffs.get(i).setUnlocked(true);

            // Auto-activate unlocked buffs by default
            if (!buffs.get(i).isActive()) {
                buffs.get(i).toggleActive();
            }
        }
    }

    /**
     * Sync buff states from achievement tracker to buff data.
     */
    public static void syncFromAchievements(Player player) {
        java.util.List<BuffEffect> buffs = getAllBuffs();

        // Update unlocked status based on achievements
        for (int i = 0; i < Math.min(buffs.size(), AchievementTracker.AchievementType.values().length); i++) {
            AchievementTracker.AchievementType achievement = AchievementTracker.AchievementType.values()[i];

            if (com.hero.sigil.achievement.AchievementTracker.isAchievementUnlocked(player, achievement)) {
                buffs.get(i).setUnlocked(true);

                // Auto-activate newly unlocked buff
                if (!buffs.get(i).isActive()) {
                    buffs.get(i).toggleActive();

                    // 对永久 buff 立即应用（处理最大生命值变化）
                    if (buffs.get(i).isPermanent() && !player.level().isClientSide() &&
                        player instanceof net.minecraft.server.level.ServerPlayer) {
                        buffs.get(i).applyBuff(player);
                    }
                }
            }
        }
    }

    /**
     * Get all current buff states as an array for network synchronization.
     */
    public static int[] getBuffStatesArray(Player player) {
        java.util.List<BuffEffect> buffs = getAllBuffs();
        int[] states = new int[3]; // Max 3 slots

        for (int i = 0; i < Math.min(buffs.size(), 3); i++) {
            BuffEffect buff = buffs.get(i);

            // Encode state: bit 0 = unlocked, bit 1 = active, bits 8-15 = buff type index
            int state = (buff.isUnlocked() ? 0x1 : 0) |
                       (buff.isActive() ? 0x2 : 0);

            states[i] = state;
        }

        return states;
    }

    /**
     * Activate a specific buff slot.
     */
    public static void activateSlot(Player player, int slotIndex) {
        java.util.List<BuffEffect> buffs = getAllBuffs();

        if (slotIndex >= 0 && slotIndex < Math.min(buffs.size(), 3)) {
            BuffEffect buff = buffs.get(slotIndex);

            // Check if unlocked by achievements
            AchievementTracker.AchievementType achievement =
                AchievementTracker.AchievementType.values()[slotIndex];

            if (com.hero.sigil.achievement.AchievementTracker.isAchievementUnlocked(player, achievement) ||
                buff.isUnlocked()) {

                boolean wasActive = buff.isActive();
                buff.toggleActive();

                // If deactivating, remove the effect
                if (!buff.isActive() && !wasActive) {
                    player.removeEffect(buff.getMobEffect());
                } else if (buff.isActive() && !wasActive) {
                    // Apply new effect when activating
                    buff.applyBuff(player);

                    // Sync to client
                    syncToClient(player);
                }
            }
        }
    }

    /**
     * Send updated buff states to the player's client.
     */
    public static void syncToClient(Player player) {
        int[] states = getBuffStatesArray(player);

        com.hero.sigil.network.HeroSigilNetworkManager.sendToPlayer(
            new com.hero.sigil.network.BuffSlotSyncPacket(player.getId(), states),
            (net.minecraft.server.level.ServerPlayer) player
        );
    }

    /**
     * 检查指定 buff 是否可以应用（考虑冲突）
     */
    public static boolean canApplyBuff(Player player, BuffEffect buff) {
        if (buff.getMobEffect() == MobEffects.MOVEMENT_SPEED) {
            MobEffectInstance existingEffect = player.getEffect(buff.getMobEffect());
            if (existingEffect != null) {
                // 如果已有更强的加速效果，返回 false
                return existingEffect.getAmplifier() <= buff.getAmplifier();
            }
        }
        return true;
    }

    /**
     * Reset all buff data for a player.
     */
    public static void reset(Player player) {
        java.util.List<BuffEffect> buffs = getAllBuffs();

        // Remove all active effects from player
        for (BuffEffect buff : buffs) {
            if (!player.level().isClientSide()) {
                buff.removeBuff(player);
            }
            buff.setUnlocked(false);
            buff.setActive(false);
        }
    }

    /**
     * Apply all active buffs to the player. Called periodically by HeroSigilItem.onPlayerTick()
     */
    public static void applyAllBuffs(Player player) {
        java.util.List<BuffEffect> buffs = getAllBuffs();

        for (BuffEffect buff : buffs) {
            if (buff.isActive() && buff.isUnlocked()) {
                buff.applyBuff(player);
            }
        }
    }

    /**
     * Get the current unlocked status of all slots.
     */
    public static boolean[] getSlotStatus(Player player) {
        java.util.List<BuffEffect> buffs = getAllBuffs();
        boolean[] statuses = new boolean[3]; // Max 3 slots

        for (int i = 0; i < Math.min(buffs.size(), 3); i++) {
            statuses[i] = buffs.get(i).isUnlocked();
        }

        return statuses;
    }

    /**
     * Check if a specific slot is unlocked.
     */
    public static boolean isSlotUnlocked(Player player, int slotIndex) {
        java.util.List<BuffEffect> buffs = getAllBuffs();

        if (slotIndex >= 0 && slotIndex < Math.min(buffs.size(), 3)) {
            return buffs.get(slotIndex).isUnlocked();
        }

        return false;
    }

    /**
     * Check if a specific slot is active.
     */
    public static boolean isSlotActive(Player player, int slotIndex) {
        java.util.List<BuffEffect> buffs = getAllBuffs();

        if (slotIndex >= 0 && slotIndex < Math.min(buffs.size(), 3)) {
            return buffs.get(slotIndex).isActive();
        }

        return false;
    }

    /**
     * Get the MobEffects that are currently active for a player.
     */
    public static java.util.List<MobEffectInstance> getActiveEffects(Player player) {
        java.util.List<BuffEffect> buffs = getAllBuffs();
        java.util.ArrayList<MobEffectInstance> effects = new java.util.ArrayList<>();

        for (BuffEffect buff : buffs) {
            if (buff.isActive() && buff.isUnlocked()) {
                // Get the current effect instance from the player
                MobEffectInstance existingEffect = player.getEffect(buff.getMobEffect());
                if (existingEffect != null) {
                    effects.add(existingEffect);
                }
            }
        }

        return effects;
    }

    /**
     * Get a list of all unlocked and active buff names for display.
     */
    public static java.util.List<String> getActiveBuffNames(Player player) {
        java.util.List<BuffEffect> buffs = getAllBuffs();
        java.util.ArrayList<String> activeNames = new java.util.ArrayList<>();

        for (BuffEffect buff : buffs) {
            if (buff.isActive() && buff.isUnlocked()) {
                // Get the localized name from the MobEffect's translation key
                String displayName = buff.getMobEffect().value().getDescriptionId();
                activeNames.add(displayName);
            }
        }

        return activeNames;
    }

    /**
     * Check if any buffs are currently active for a player.
     */
    public static boolean hasActiveBuffs(Player player) {
        java.util.List<BuffEffect> buffs = getAllBuffs();

        for (BuffEffect buff : buffs) {
            if (buff.isActive() && buff.isUnlocked()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Count the number of active buffs.
     */
    public static int getActiveBuffsCount(Player player) {
        java.util.List<BuffEffect> buffs = getAllBuffs();
        int count = 0;

        for (BuffEffect buff : buffs) {
            if (buff.isActive() && buff.isUnlocked()) {
                count++;
            }
        }

        return count;
    }

    /**
     * Get the current tick-based state of all active buffs. Used for periodic refresh.
     */
    public static int[] getBuffTickState(Player player) {
        java.util.List<BuffEffect> buffs = getAllBuffs();
        int[] states = new int[3]; // Max 3 slots

        for (int i = 0; i < Math.min(buffs.size(), 3); i++) {
            BuffEffect buff = buffs.get(i);

            // Store: bit 0 = has effect, bits 1-5 = remaining ticks (simplified)
            int tickState = 0x0;
            if (!player.level().isClientSide()) {
                MobEffectInstance currentEffect = player.getEffect(buff.getMobEffect());
                if (currentEffect != null && buff.isActive() && buff.isUnlocked()) {
                    tickState |= 0x1; // Mark as having effect

                    // Store remaining ticks (scaled down for storage efficiency)
                    int remainingTicks = Math.min(currentEffect.getDuration(), 320); // Cap at 16 seconds
                    states[i] = tickState | (remainingTicks << 8);
                } else {
                    states[i] = 0;
                }
            }
        }

        return states;
    }

    /**
     * Force unlock a buff slot for testing purposes.
     */
    public static void forceUnlockSlot(Player player, int slotIndex) {
        java.util.List<BuffEffect> buffs = getAllBuffs();

        if (slotIndex >= 0 && slotIndex < Math.min(buffs.size(), 3)) {
            buffs.get(slotIndex).setUnlocked(true);

            // Sync to client
            syncToClient(player);
        }
    }

    /**
     * Force activate a buff slot for testing purposes.
     */
    public static void forceActivateSlot(Player player, int slotIndex) {
        java.util.List<BuffEffect> buffs = getAllBuffs();

        if (slotIndex >= 0 && slotIndex < Math.min(buffs.size(), 3)) {
            BuffEffect buff = buffs.get(slotIndex);

            // Ensure unlocked first
            buff.setUnlocked(true);
            buff.toggleActive();

            // Apply immediately
            if (!player.level().isClientSide()) {
                buff.applyBuff(player);

                // Sync to client
                syncToClient(player);
            }
        }
    }

    /**
     * Get the current state of all buffs for debugging purposes.
     */
    public static java.util.Map<String, Object> getDebugState(Player player) {
        java.util.List<BuffEffect> buffs = getAllBuffs();
        java.util.HashMap<String, Object> debugInfo = new java.util.HashMap<>();

        for (int i = 0; i < Math.min(buffs.size(), 3); i++) {
            BuffEffect buff = buffs.get(i);

            // Get current effect instance if any
            MobEffectInstance currentEffect = !player.level().isClientSide() ?
                player.getEffect(buff.getMobEffect()) : null;

            java.util.HashMap<String, Object> slotInfo = new java.util.HashMap<>();
            slotInfo.put("id", buff.getId());
            slotInfo.put("unlocked", buff.isUnlocked());
            slotInfo.put("active", buff.isActive());
            slotInfo.put("hasEffect", currentEffect != null);

            if (currentEffect != null) {
                slotInfo.put("durationTicks", currentEffect.getDuration());
                slotInfo.put("amplifier", currentEffect.getAmplifier());
            }

            debugInfo.put("slot_" + (i + 1), slotInfo);
        }

        return debugInfo;
    }

}
