package com.hero.sigil.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据恢复管理器 - 封装备份/恢复功能，提供统一的数据恢复接口
 */
public class DataRecovery {

    private static final String TAG_KEY = "HeroSigil";
    private static final String BACKUP_TAG = "HeroSigil_backup";
    private static final int MAX_BACKUPS = 3;

    /**
     * 备份记录
     */
    public static class BackupRecord {
        private final long timestamp;
        private final int version;

        public BackupRecord(long timestamp, int version) {
            this.timestamp = timestamp;
            this.version = version;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public int getVersion() {
            return version;
        }

        /**
         * 格式化时间戳为可读字符串
         */
        public String formatTimestamp() {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(new java.util.Date(timestamp));
        }
    }

    /**
     * 恢复结果
     */
    public static class RecoveryResult {
        private final boolean success;
        private final String message;
        private final int restoredSlots;

        public RecoveryResult(boolean success, String message, int restoredSlots) {
            this.success = success;
            this.message = message;
            this.restoredSlots = restoredSlots;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public int getRestoredSlots() {
            return restoredSlots;
        }
    }

    /**
     * 检查 NBT 数据完整性
     */
    public static boolean validateData(CompoundTag sigilData) {
        // 检查版本号
        if (!sigilData.contains("dataVersion")) {
            return false;
        }

        int version = sigilData.getInt("dataVersion");
        if (version < 0 || version > getCurrentVersion() + 1) {
            return false;
        }

        // 检查必需槽位数据
        boolean hasAnySlot = false;
        for (int i = 1; i <= 3; i++) {
            String slotKey = "slot_" + i;
            if (sigilData.contains(slotKey)) {
                CompoundTag slotData = sigilData.getCompound(slotKey);

                // 检查必需字段
                if (slotData.contains("unlocked") && slotData.contains("active")) {
                    hasAnySlot = true;
                }
            }
        }

        return hasAnySlot;
    }

    /**
     * 创建当前数据的备份
     */
    public static void createBackup(Player player, CompoundTag persistData) {
        CompoundTag sigilData = persistData.getCompound(TAG_KEY);

        // 检查是否有现有备份
        int backupCount = 0;
        if (persistData.contains(BACKUP_TAG)) {
            CompoundTag backup = persistData.getCompound(BACKUP_TAG);
            backupCount = backup.getInt("count");
        }

        // 如果备份数量达到上限，删除最旧的备份
        if (backupCount >= MAX_BACKUPS) {
            persistData.remove(BACKUP_TAG);
            backupCount = 0;
        }

        // 创建新备份
        CompoundTag backupData = new CompoundTag();
        backupData.putInt("timestamp", (int) System.currentTimeMillis());
        backupData.putInt("version", sigilData.getInt("dataVersion"));
        backupData.put("data", sigilData.copy());

        // 更新备份计数
        backupData.putInt("count", backupCount + 1);

        persistData.put(BACKUP_TAG, backupData);

        com.hero.sigil.HeroSigil.LOGGER.info("Created backup {} for player {}", backupCount + 1, player.getScoreboardName());
    }

    /**
     * 从备份恢复数据
     */
    public static RecoveryResult restoreFromBackup(Player player, CompoundTag persistData) {
        if (!persistData.contains(BACKUP_TAG)) {
            return new RecoveryResult(false, "无可用备份", 0);
        }

        CompoundTag backupData = persistData.getCompound(BACKUP_TAG);
        if (!backupData.contains("data")) {
            return new RecoveryResult(false, "备份数据无效", 0);
        }

        // 备份当前数据（作为新备份保存）
        createBackup(player, persistData);

        // 恢复备份数据
        CompoundTag sigilData = backupData.getCompound("data");
        persistData.put(TAG_KEY, sigilData);

        // 重新加载数据
        com.hero.sigil.buffs.HeroSigilData.onLoad(player, persistData);

        // 计算恢复的槽位数
        int restoredSlots = 0;
        for (int i = 1; i <= 3; i++) {
            if (sigilData.contains("slot_" + i)) {
                restoredSlots++;
            }
        }

        com.hero.sigil.HeroSigil.LOGGER.info("Restored data from backup for player {}", player.getScoreboardName());

        return new RecoveryResult(true, "数据已从备份恢复", restoredSlots);
    }

    /**
     * 获取备份列表
     */
    public static List<BackupRecord> getBackupList(CompoundTag persistData) {
        List<BackupRecord> records = new ArrayList<>();

        if (persistData.contains(BACKUP_TAG)) {
            CompoundTag backupData = persistData.getCompound(BACKUP_TAG);
            if (backupData.contains("data")) {
                records.add(new BackupRecord(
                    backupData.getInt("timestamp"),
                    backupData.getInt("version")
                ));
            }
        }

        return records;
    }

    /**
     * 检查 buff 状态与 NBT 数据是否一致
     */
    public static boolean checkDataConsistency(ServerPlayer player) {
        CompoundTag persistData = player.getPersistentData();
        if (!persistData.contains(TAG_KEY)) {
            return false;
        }

        CompoundTag sigilData = persistData.getCompound(TAG_KEY);
        java.util.List<com.hero.sigil.buffs.BuffEffect> buffs = com.hero.sigil.buffs.HeroSigilData.getAllBuffs();

        for (int i = 0; i < Math.min(buffs.size(), 3); i++) {
            com.hero.sigil.buffs.BuffEffect buff = buffs.get(i);
            String slotKey = "slot_" + (i + 1);

            if (sigilData.contains(slotKey)) {
                CompoundTag slotData = sigilData.getCompound(slotKey);
                boolean unlockedFromNBT = slotData.getBoolean("unlocked");
                boolean activeFromNBT = slotData.getBoolean("active");

                // 检查一致性
                if (buff.isUnlocked() != unlockedFromNBT || buff.isActive() != activeFromNBT) {
                    com.hero.sigil.HeroSigil.LOGGER.warn("Data inconsistency detected for slot {} in player {}",
                        i + 1, player.getScoreboardName());
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
        java.util.List<com.hero.sigil.buffs.BuffEffect> buffs = com.hero.sigil.buffs.HeroSigilData.getAllBuffs();
        boolean changed = false;

        for (int i = 0; i < Math.min(buffs.size(), 3); i++) {
            com.hero.sigil.buffs.BuffEffect buff = buffs.get(i);
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

                    com.hero.sigil.HeroSigil.LOGGER.info("Fixed data inconsistency for slot {} in player {}",
                        i + 1, player.getScoreboardName());
                }
            }
        }

        if (changed) {
            persistData.put(TAG_KEY, sigilData);
            com.hero.sigil.HeroSigil.LOGGER.info("Data inconsistency fixed for player {}", player.getScoreboardName());

            player.sendSystemMessage(
                Component.literal("§e§l勇者之证§r§f: 数据不一致已自动修复")
            );
        }
    }

    /**
     * 创建默认数据
     */
    public static CompoundTag createDefaultData() {
        CompoundTag sigilData = new CompoundTag();
        sigilData.putInt("dataVersion", getCurrentVersion());

        // 创建默认槽位数据
        for (int i = 1; i <= 3; i++) {
            CompoundTag slotData = new CompoundTag();
            slotData.putBoolean("unlocked", false);
            slotData.putBoolean("active", false);
            sigilData.put("slot_" + i, slotData);
        }

        return sigilData;
    }

    /**
     * 获取当前数据版本
     */
    private static int getCurrentVersion() {
        return com.hero.sigil.buffs.HeroSigilData.CURRENT_DATA_VERSION;
    }

    /**
     * 显示备份信息
     */
    public static void showBackupInfo(ServerPlayer player) {
        CompoundTag persistData = player.getPersistentData();

        if (!persistData.contains("HeroSigil")) {
            player.sendSystemMessage(
                Component.literal("§e§l勇者之证§r§f: 玩家无勇者之证数据")
            );
            return;
        }

        List<BackupRecord> backups = getBackupList(persistData);

        if (backups.isEmpty()) {
            player.sendSystemMessage(
                Component.literal("§e§l勇者之证§r§f: 无可用备份")
            );
            return;
        }

        player.sendSystemMessage(
            Component.literal("§e§l勇者之证§r§f: 可用备份 (" + backups.size() + "):")
        );

        for (BackupRecord record : backups) {
            player.sendSystemMessage(
                Component.literal("  §f备份版本 §e" + record.getVersion() + " §f- §b" + record.formatTimestamp())
            );
        }
    }
}
