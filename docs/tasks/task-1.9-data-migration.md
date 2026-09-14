# 任务 1.9: 数据迁移逻辑

## 任务目标
实现 NBT 数据迁移框架，支持未来版本升级时平滑迁移旧玩家数据。

## 涉及文件
- [HeroSigilData.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/buffs/HeroSigilData.java) - 主要修改

## 当前代码分析

### HeroSigilData.java 问题分析

**前提条件**: 任务 1.8 (NBT 版本管理) 已完成，`onLoad()` 方法中包含版本检测和迁移调用。

**当前迁移方法** (由任务 1.8 创建):
```java
private static CompoundTag migrateData(int oldVersion, CompoundTag data, Player player) {
    HeroSigil.LOGGER.info("Migrating Hero Sigil data from version {} to {}", oldVersion, CURRENT_DATA_VERSION);
    
    // 当前版本为 1，暂无迁移逻辑
    
    data.putInt(VERSION_TAG, CURRENT_DATA_VERSION);
    
    HeroSigil.LOGGER.info("Data migration completed successfully");
    return data;
}
```

**需要实现的迁移场景**:
1. 版本 1 -> 版本 2: 添加新 buff 槽位时，迁移旧槽位数据
2. 版本 2 -> 版本 3: 修改 buff 类型时，重新映射 buff ID
3. 未来版本升级时的数据兼容性

## 实现步骤

### 步骤 1: 完善 migrateData() 方法框架
**修改位置**: HeroSigilData.java 中 migrateData() 方法

**实现多版本迁移框架**:

```java
/**
 * 根据旧版本迁移数据到当前版本
 */
private static CompoundTag migrateData(int oldVersion, CompoundTag data, Player player) {
    HeroSigil.LOGGER.info("Migrating Hero Sigil data from version {} to {} for player {}", 
        oldVersion, CURRENT_DATA_VERSION, player.getScoreboardName());
    
    // 按版本逐步迁移（确保每个中间版本都被正确处理）
    if (oldVersion < CURRENT_DATA_VERSION) {
        // 从旧版本逐步升级到当前版本
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
    switch (fromVersion) {
        case 1:
            return migrateFromV1ToV2(data, player);
        case 2:
            return migrateFromV2ToV3(data, player);
        default:
            HeroSigil.LOGGER.warn("Unknown migration path from version {}", fromVersion);
            return data;
    }
}
```

### 步骤 2: 实现 v1 -> v2 迁移 (添加新 buff 槽位)
**添加迁移方法**:

```java
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
                data.setTag(slotKey, slotData);
                
                HeroSigil.LOGGER.info("Created new empty buff slot {} for player {}", i, player.getScoreboardName());
            }
        }
    }
    
    // 更新版本号
    data.putInt(VERSION_TAG, 2);
    
    HeroSigil.LOGGER.info("Migration v1 -> v2 completed for player {}", player.getScoreboardName());
    return data;
}
```

### 步骤 3: 实现 v2 -> v3 迁移 (Buff ID 重新映射)
**添加迁移方法**:

```java
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
```

### 步骤 4: 添加迁移日志和错误处理
**完善迁移方法的日志记录**:

```java
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
```

### 步骤 5: 添加迁移回滚支持
**添加回滚方法**:

```java
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
```

### 步骤 6: 在 onLoad() 中集成迁移逻辑
**修改 onLoad() 方法** (由任务 1.8 创建，需要完善):

```java
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
        
        if (dataVersion < CURRENT_DATA_VERSION) {
            sigilData = migrateData(dataVersion, sigilData, player);
        } else if (dataVersion > CURRENT_DATA_VERSION) {
            HeroSigil.LOGGER.warn("Player data version {} is newer than current version {}, may be incompatible", 
                dataVersion, CURRENT_DATA_VERSION);
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

## 验收标准
- [ ] `migrateData()` 方法正确调用逐版本迁移
- [ ] `migrateFromV1ToV2()` 方法创建新 buff 槽位
- [ ] `migrateFromV2ToV3()` 方法支持 buff ID 映射
- [ ] 迁移过程有详细的日志记录
- [ ] 迁移失败时有异常处理
- [ ] 支持数据回滚
- [ ] 新版本号数据向后兼容

## 注意事项
1. 迁移必须按顺序执行（v1->v2->v3，不能跳过）
2. 每个迁移方法必须更新版本号
3. 迁移失败时返回原始数据，不丢失玩家进度
4. 日志记录迁移开始、结束、持续时间
5. 回滚方法用于测试和数据修复

## 测试建议
1. 修改 `CURRENT_DATA_VERSION = 2`，启动游戏，验证 v1->v2 迁移
2. 检查新槽位是否正确创建
3. 查看日志中的迁移详细信息
4. 测试回滚功能
5. 模拟迁移失败场景，验证数据不丢失
