# 任务 1.10: 数据损坏恢复

## 任务目标
实现 NBT 数据完整性检测和自动恢复机制，确保玩家进度不因数据损坏而丢失。

## 涉及文件
- [HeroSigilData.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/buffs/HeroSigilData.java) - 主要修改
- [CapabilityEvents.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/persistence/CapabilityEvents.java) - 添加修复命令

## 当前代码分析

### HeroSigilData.java 问题分析

**当前 onLoad() 方法** (由任务 1.8/1.9 修改):
```java
public static void onLoad(Player player, CompoundTag nbt) {
    if (nbt.contains(TAG_KEY)) {
        CompoundTag sigilData = nbt.getCompound(TAG_KEY);
        
        int dataVersion = sigilData.getInt(VERSION_TAG);
        if (dataVersion == 0) {
            dataVersion = 1;
            sigilData.putInt(VERSION_TAG, 1);
        }
        
        if (dataVersion < CURRENT_DATA_VERSION) {
            sigilData = migrateData(dataVersion, sigilData, player);
        }
        
        // 加载 buff 状态...
        java.util.List<BuffEffect> buffs = getAllBuffs();
        for (int i = 0; i < Math.min(buffs.size(), 3); i++) {
            BuffEffect buff = buffs.get(i);
            String slotKey = "slot_" + (i + 1);
            
            if (sigilData.contains(slotKey)) {
                CompoundTag slotData = sigilData.getCompound(slotKey);
                buff.setUnlocked(slotData.getBoolean("unlocked"));
                buff.setActive(slotData.getBoolean("active"));
                
                if (buff.isActive() && buff.isUnlocked()) {
                    buff.applyBuff(player);
                }
            }
        }
    } else {
        applyDefaultBuffsForPlayer(player);
    }
}
```

**问题**:
1. 没有数据完整性检查
2. NBT 数据可能因游戏崩溃、模组更新等损坏
3. 损坏的数据可能导致 buff 状态异常
4. 没有恢复机制

## 实现步骤

### 步骤 1: 添加数据校验方法
**在 HeroSigilData.java 中添加校验方法**:

```java
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
```

### 步骤 2: 添加备份机制
**在 HeroSigilData.java 中添加备份方法**:

```java
private static final String BACKUP_TAG = "HeroSigil_backup";
private static final int MAX_BACKUPS = 3;

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
```

### 步骤 3: 修改 onLoad() 方法集成校验和恢复
**修改 onLoad() 方法**:

```java
public static void onLoad(Player player, CompoundTag nbt) {
    if (nbt.contains(TAG_KEY)) {
        CompoundTag sigilData = nbt.getCompound(TAG_KEY);
        
        // 获取并处理版本号
        int dataVersion = sigilData.getInt(VERSION_TAG);
        if (dataVersion == 0) {
            dataVersion = 1;
            sigilData.putInt(VERSION_TAG, 1);
        }
        
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
                    sigilData.setTag("slot_" + i, slotData);
                }
                
                nbt.put(TAG_KEY, sigilData);
            } else {
                // 重新加载恢复后的数据
                sigilData = nbt.getCompound(TAG_KEY);
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
        for (int i = 0; i < Math.min(buffs.size(), 3); i++) {
            BuffEffect buff = buffs.get(i);
            String slotKey = "slot_" + (i + 1);
            
            if (sigilData.contains(slotKey)) {
                CompoundTag slotData = sigilData.getCompound(slotKey);
                buff.setUnlocked(slotData.getBoolean("unlocked"));
                buff.setActive(slotData.getBoolean("active"));
                
                if (buff.isActive() && buff.isUnlocked()) {
                    buff.applyBuff(player);
                }
            }
        }
        
        HeroSigil.LOGGER.info("Successfully loaded Hero Sigil data for player {}", player.getScoreboardName());
    } else {
        applyDefaultBuffsForPlayer(player);
    }
}
```

### 步骤 4: 添加数据一致性检查
**添加方法检查 buff 状态与 NBT 数据是否一致**:

```java
/**
 * 检查 buff 状态与 NBT 数据是否一致
 * @return 如果一致返回 true，否则返回 false
 */
public static boolean checkDataConsistency(Player player) {
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
public static void fixDataInconsistency(Player player) {
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
```

### 步骤 5: 在 CapabilityEvents.java 中添加修复命令
**在 CapabilityEvents.java 中添加命令注册**:

```java
@SubscribeEvent
public static void onRegisterCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
    com.mojang.brigadier.CommandDispatcher<net.minecraft.server.commands.ServerCommandSource> dispatcher = 
        event.getDispatcher();
    
    // /herosigil repair - 修复数据不一致
    dispatcher.register(
        com.mojang.brigadier.Command.literal("herosigil")
            .then(com.mojang.brigadier.Command.literal("repair")
                .executes(context -> {
                    var player = context.getSource().getPlayer();
                    if (player == null) {
                        return 0;
                    }
                    
                    com.hero.sigil.buffs.HeroSigilData.fixDataInconsistency(player);
                    return 1;
                })
            )
    );
    
    // /herosigil validate - 验证数据完整性
    dispatcher.register(
        com.mojang.brigadier.Command.literal("herosigil")
            .then(com.mojang.brigadier.Command.literal("validate")
                .executes(context -> {
                    var player = context.getSource().getPlayer();
                    if (player == null) {
                        return 0;
                    }
                    
                    CompoundTag persistData = player.getPersistentData();
                    if (persistData.contains("HeroSigil")) {
                        CompoundTag sigilData = persistData.getCompound("HeroSigil");
                        if (com.hero.sigil.buffs.HeroSigilData.validateData(sigilData)) {
                            player.sendSystemMessage(
                                net.minecraft.network.chat.Component.literal("§a§l勇者之证§r§f: 数据完整性检查通过")
                            );
                        } else {
                            player.sendSystemMessage(
                                net.minecraft.network.chat.Component.literal("§c§l勇者之证§r§f: 数据不完整，使用 /herosigil repair 修复")
                            );
                        }
                    } else {
                        player.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal("§e§l勇者之证§r§f: 玩家无勇者之证数据")
                        );
                    }
                    
                    return 1;
                })
            )
    );
}
```

## 验收标准
- [ ] `validateData()` 方法正确检查数据完整性
- [ ] 损坏数据时自动从备份恢复
- [ ] 无备份时创建默认数据
- [ ] 备份机制正常工作（最多 3 个备份）
- [ ] 数据一致性检查正确
- [ ] 自动修复功能正常
- [ ] `/herosigil repair` 和 `/herosigil validate` 命令可用
- [ ] 玩家收到数据恢复通知

## 注意事项
1. 备份数据保存在 `HeroSigil_backup` 键下
2. 最多保留 3 个备份
3. 恢复时先备份当前数据，再恢复旧备份
4. 数据修复使用 `fixDataInconsistency()` 方法
5. 命令使用 Brigadier 注册

## 测试建议
1. 手动修改 NBT 数据（如删除 slot_1），验证自动恢复
2. 使用 `/herosigil validate` 检查数据完整性
3. 使用 `/herosigil repair` 修复数据不一致
4. 检查备份文件是否正确创建
5. 查看日志中的恢复和修复记录
