# 任务 1.5: Boss 识别优化

## 任务目标
重构 Boss 识别逻辑，支持所有 Boss 类型（末影龙、凋灵、监守者等），添加击杀计数和中文通知。

## 涉及文件
- [AchievementTracker.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/achievement/AchievementTracker.java) - 主要修改

## 当前代码分析

### AchievementTracker.java 问题分析

**问题 1: Boss 识别逻辑不完整 (第 138-183 行)**
```java
@SubscribeEvent
public static void onLivingDeath(LivingDeathEvent event) {
    if (event.getSource().getDirectEntity() instanceof Player player) {
        PlayerProgress progress = getPlayerProgress(player);
        
        // 仅检测 WitherBoss 和 EnderDragon
        if (event.getEntity() instanceof WitherBoss) {
            isBoss = true;
            progress.incrementProgress(AchievementType.DEFEAT_BOSS, 1);
        } else if (event.getEntity() instanceof EnderDragon) {
            isBoss = true;
            progress.incrementProgress(AchievementType.DEFEAT_BOSS, 1);
            
            if (!progress.isUnlocked(AchievementType.DEFEAT_ENDER_DRAGON)) {
                progress.unlock(AchievementType.DEFEAT_ENDER_DRAGON);
            }
        } else if (isBoss && event.getEntity().getType() != EntityType.ENDER_DRAGON) {
            // 通用 Boss 检测
            progress.incrementProgress(AchievementType.DEFEAT_BOSS, 1);
            
            if (!progress.isUnlocked(AchievementType.DEFEAT_BOSS)) {
                progress.unlock(AchievementType.DEFEAT_BOSS);
            }
        }
    }
}
```

**问题 2: `isBossEntity()` 方法已实现但未使用 (第 188-193 行)**
```java
private static boolean isBossEntity(net.minecraft.world.entity.Entity entity) {
    return switch (entity.getType().getDefaultRegistryName().getPath()) {
        case "ender_dragon", "wither" -> true;
        default -> false;
    };
}
```

**问题 3: 无 Boss 击杀计数和通知**
- 没有记录击杀数量
- 没有聊天通知

## 实现步骤

### 步骤 1: 扩展 isBossEntity() 方法
**修改位置**: AchievementTracker.java 第 188-193 行

**当前代码**:
```java
private static boolean isBossEntity(net.minecraft.world.entity.Entity entity) {
    return switch (entity.getType().getDefaultRegistryName().getPath()) {
        case "ender_dragon", "wither" -> true;
        default -> false;
    };
}
```

**修改为**:
```java
/**
 * 检查实体是否为 Boss 类型
 */
private static boolean isBossEntity(net.minecraft.world.entity.Entity entity) {
    // 方法 1: 检查实体类型是否为已知的 Boss
    if (entity instanceof WitherBoss || entity instanceof EnderDragon) {
        return true;
    }
    
    // 方法 2: 检查实体是否带有 BossDisplayData
    BossDisplayData bossDisplay = entity.getType().getBossDisplayData();
    if (bossDisplay != null) {
        return true;
    }
    
    // 方法 3: 检查实体 ID 路径
    var registryName = entity.getType().getDefaultRegistryName();
    if (registryName != null) {
        String path = registryName.getPath();
        return switch (path) {
            case "ender_dragon", "wither", "warden", "elder_guardian" -> true;
            default -> false;
        };
    }
    
    return false;
}
```

### 步骤 2: 重构 onLivingDeath() 方法
**修改位置**: AchievementTracker.java 第 137-183 行

**当前代码**:
```java
@SubscribeEvent
public static void onLivingDeath(LivingDeathEvent event) {
    if (event.getSource().getDirectEntity() instanceof Player player) {
        PlayerProgress progress = getPlayerProgress(player);
        
        boolean isBoss = event.getEntity().getType().getDefaultRegistryName().getPath().contains("boss") ||
                        event.getEntity().hasCustomName() && 
                        event.getEntity().getDisplayName().getString().contains("Boss");
        
        if (event.getEntity() instanceof WitherBoss) {
            isBoss = true;
            progress.incrementProgress(AchievementType.DEFEAT_BOSS, 1);
            if (!progress.isUnlocked(AchievementType.DEFEAT_ENDER_DRAGON)) {
                // In a full implementation, we'd track which bosses have been defeated
            }
        } else if (event.getEntity() instanceof EnderDragon) {
            isBoss = true;
            progress.incrementProgress(AchievementType.DEFEAT_BOSS, 1);
            if (!progress.isUnlocked(AchievementType.DEFEAT_ENDER_DRAGON)) {
                progress.unlock(AchievementType.DEFEAT_ENDER_DRAGON);
                if (progress.progressValues.getOrDefault(AchievementType.DEFEAT_BOSS, 0) < 1) {
                    progress.incrementProgress(AchievementType.DEFEAT_BOSS, 1);
                }
            }
        } else if (isBoss && event.getEntity().getType() != EntityType.ENDER_DRAGON) {
            progress.incrementProgress(AchievementType.DEFEAT_BOSS, 1);
            if (!progress.isUnlocked(AchievementType.DEFEAT_BOSS)) {
                progress.unlock(AchievementType.DEFEAT_BOSS);
            }
        }
    }
}
```

**修改为**:
```java
@SubscribeEvent
public static void onLivingDeath(LivingDeathEvent event) {
    if (event.getSource().getDirectEntity() instanceof Player player) {
        PlayerProgress progress = getPlayerProgress(player);
        
        // 使用 isBossEntity() 方法检测 Boss
        if (isBossEntity(event.getEntity())) {
            String bossName = event.getEntity().getDisplayName().getString();
            
            // 增加 Boss 击杀计数
            progress.incrementProgress(AchievementType.DEFEAT_BOSS, 1);
            
            // 解锁 DEFEAT_BOSS 成就（如果尚未解锁）
            if (!progress.isUnlocked(AchievementType.DEFEAT_BOSS)) {
                progress.unlock(AchievementType.DEFEAT_BOSS);
            }
            
            // 特殊处理末影龙
            if (event.getEntity() instanceof EnderDragon) {
                if (!progress.isUnlocked(AchievementType.DEFEAT_ENDER_DRAGON)) {
                    progress.unlock(AchievementType.DEFEAT_ENDER_DRAGON);
                    
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
```

### 步骤 3: 在 PlayerProgress 中添加击杀计数
**修改位置**: AchievementTracker.java 第 59-100 行 (PlayerProgress 类)

**当前代码**:
```java
public static class PlayerProgress {
    private final Map<AchievementType, Boolean> unlockedAchievements = new HashMap<>();
    private final Map<AchievementType, Integer> progressValues = new HashMap<>();
    // ...
}
```

**修改为**:
```java
public static class PlayerProgress {
    private final Map<AchievementType, Boolean> unlockedAchievements = new HashMap<>();
    private final Map<AchievementType, Integer> progressValues = new HashMap<>();
    
    // 添加 Boss 击杀计数
    private int bossKillCount = 0;
    
    // 在 unlock() 方法中添加通知
    public void unlock(AchievementType achievement) {
        if (!isUnlocked(achievement)) {
            unlockedAchievements.put(achievement, true);
            notifyPlayerUnlock(achievement);
        }
    }
    
    // 添加 Boss 击杀计数方法
    public void incrementBossKillCount() {
        bossKillCount++;
    }
    
    public int getBossKillCount() {
        return bossKillCount;
    }
    
    // ... 其他方法保持不变
}
```

### 步骤 4: 修复 notifyPlayerUnlock() 方法
**修改位置**: AchievementTracker.java 第 75-78 行

**当前代码**:
```java
private void notifyPlayerUnlock(AchievementType achievement) {
    // TODO: Send packet to client to show notification
}
```

**修改为**:
```java
private void notifyPlayerUnlock(AchievementType achievement) {
    // 获取玩家引用（需要通过外部传入或静态变量）
    // 简化版：使用聊天通知
    // 注意：此方法在 PlayerProgress 内部，需要传入 Player 引用
}
```

**注意**: 由于 `notifyPlayerUnlock()` 在 `PlayerProgress` 内部，需要修改方法签名传入 `Player` 参数：

```java
private void notifyPlayerUnlock(AchievementType achievement, Player player) {
    String achievementName = switch (achievement) {
        case DEFEAT_BOSS -> "击败 Boss";
        case EXPLORE_ALL_BIOMES -> "探索世界";
        case DEFEAT_ENDER_DRAGON -> "击败末影龙";
        case BUILD_REDSTONE_MACHINE -> "建造红石机器";
        case COMPLETE_COLLECTION -> "完成收集";
        default -> "未知成就";
    };
    
    player.sendSystemMessage(
        net.minecraft.network.chat.Component.literal("§6§l成就解锁§r§f: " + achievementName + " - 新的 buff 槽位已解锁！")
    );
}
```

同时修改 `unlock()` 方法调用：
```java
public void unlock(AchievementType achievement, Player player) {
    if (!isUnlocked(achievement)) {
        unlockedAchievements.put(achievement, true);
        notifyPlayerUnlock(achievement, player);
    }
}
```

## 验收标准
- [ ] `isBossEntity()` 方法检测末影龙、凋灵、监守者
- [ ] `onLivingDeath()` 方法使用 `isBossEntity()` 进行 Boss 检测
- [ ] 击败末影龙时显示中文通知
- [ ] 击败其他 Boss 时显示击杀通知
- [ ] Boss 击杀计数正确累加
- [ ] 成就解锁时显示通知

## 注意事项
1. 使用 `instanceof` 检查实体类型是最可靠的方法
2. 检查 `BossDisplayData` 可以捕获自定义 Boss
3. 中文通知使用 `§` 颜色代码格式化
4. 通知消息需要区分"首次解锁"和"重复击杀"

## 测试建议
1. 使用 `/summon minecraft:ender_dragon` 生成末影龙并击杀，验证通知
2. 使用 `/summon minecraft:wither` 生成凋灵并击杀，验证通知
3. 检查 NBT 数据中 Boss 击杀计数是否正确
4. 验证成就解锁通知正确显示
