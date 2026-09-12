# 任务 1.7: 成就进度通知

## 任务目标
实现成就完成通知和群系探索进度提示，让玩家清楚了解当前进度。

## 涉及文件
- [AchievementTracker.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/achievement/AchievementTracker.java) - 主要修改
- [HeroSigilData.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/buffs/HeroSigilData.java) - 辅助修改

## 当前代码分析

### AchievementTracker.java 问题分析

**notifyPlayerUnlock() 方法** (第 75-78 行):
```java
private void notifyPlayerUnlock(AchievementType achievement) {
    // TODO: Send packet to client to show notification
}
```

**问题**:
1. 成就解锁时没有通知玩家
2. 群系探索没有进度提示
3. 缺少成就完成动画效果

## 实现步骤

### 步骤 1: 实现成就解锁通知
**修改 PlayerProgress 类**:

**修改位置**: AchievementTracker.java 第 59-100 行 (PlayerProgress 类)

**添加 Player 引用和修改通知方法**:

```java
public static class PlayerProgress {
    private final Map<AchievementType, Boolean> unlockedAchievements = new HashMap<>();
    private final Map<AchievementType, Integer> progressValues = new HashMap<>();
    private int bossKillCount = 0;
    
    // 保存玩家引用用于通知
    private Player currentPlayer;
    
    public void unlock(AchievementType achievement, Player player) {
        this.currentPlayer = player;
        if (!isUnlocked(achievement)) {
            unlockedAchievements.put(achievement, true);
            notifyPlayerUnlock(achievement);
        }
    }
    
    // 修改 unlock() 方法签名，添加 Player 参数
    public void unlock(AchievementType achievement) {
        if (!isUnlocked(achievement)) {
            unlockedAchievements.put(achievement, true);
            notifyPlayerUnlock(achievement);
        }
    }
```

**修改 notifyPlayerUnlock() 方法**:

```java
/**
 * 通知玩家成就已解锁
 */
private void notifyPlayerUnlock(AchievementType achievement) {
    String achievementName = switch (achievement) {
        case DEFEAT_BOSS -> "击败 Boss";
        case EXPLORE_ALL_BIOMES -> "探索世界";
        case DEFEAT_ENDER_DRAGON -> "击败末影龙";
        case BUILD_REDSTONE_MACHINE -> "建造红石机器";
        case COMPLETE_COLLECTION -> "完成收集";
        default -> "未知成就";
    };
    
    // 发送中文通知
    if (currentPlayer != null) {
        currentPlayer.sendSystemMessage(
            net.minecraft.network.chat.Component.literal("§6§l成就解锁§r§f: " + achievementName + " - 新的 buff 槽位已解锁！")
        );
        
        // 播放成就完成粒子效果（金色粒子）
        currentPlayer.level().addParticle(
            net.minecraft.core.particles.ParticleTypes.NOTE,
            currentPlayer.getX(), currentPlayer.getY() + 1.5, currentPlayer.getZ(),
            1.0, 0.0, 0.0
        );
    }
}
```

### 步骤 2: 修改所有调用 unlock() 的地方
**修改位置**: AchievementTracker.java 中所有调用 `progress.unlock()` 的地方

**在 onLivingDeath() 方法中**:
```java
// 修改前:
progress.unlock(AchievementType.DEFEAT_BOSS);

// 修改为:
progress.unlock(AchievementType.DEFEAT_BOSS, player);
```

**在 onPlayerTick() 方法中**:
```java
// 修改前:
progress.unlock(AchievementType.EXPLORE_ALL_BIOMES);

// 修改为:
progress.unlock(AchievementType.EXPLORE_ALL_BIOMES, player);
```

### 步骤 3: 添加群系探索进度提示
**在 onPlayerTick() 方法中添加进度提示**:

**修改位置**: AchievementTracker.java 第 111-135 行 (onPlayerTick 方法)

```java
@SubscribeEvent
public static void onPlayerTick(PlayerTickEvent.Pre event) {
    Player player = event.getPlayer();
    
    if (player instanceof ServerPlayer serverPlayer) {
        // 每 100 tick (5 秒) 记录一次群系
        if (player.tickCount % 100 != 0) {
            return;
        }
        
        String playerId = player.getStringUUID().toString();
        
        // 已访问 50 个群系后停止记录
        Set<ResourceKey<net.minecraft.world.level.Biome>> visitedBiomes = BIOME_VISITS.get(playerId);
        if (visitedBiomes != null && visitedBiomes.size() >= 50) {
            return;
        }
        
        var biomeHolder = player.level().getBiome(player.blockPosition());
        var currentBiome = biomeHolder.unwrapKey().orElse(null);
        
        if (currentBiome != null) {
            boolean isNewBiome = visitedBiomes == null || !visitedBiomes.contains(currentBiome);
            
            if (isNewBiome) {
                BIOME_VISITS.computeIfAbsent(playerId, k -> new HashSet<>()).add(currentBiome);
            }
            
            PlayerProgress progress = getPlayerProgress(player);
            
            // 每访问 5 个新群系，发送一次进度提示
            if (isNewBiome && visitedBiomes != null && visitedBiomes.size() % 5 == 0 
                && visitedBiomes.size() < 20) {
                int progressCount = visitedBiomes.size();
                int percentage = (int) ((double) progressCount / 20 * 100);
                
                player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                        "§b§l群系探索§r§f: " + progressCount + "/20 (" + percentage + "%)"
                    )
                );
            }
            
            // 检查是否解锁成就
            if (visitedBiomes != null && visitedBiomes.size() >= 20 
                && !progress.isUnlocked(AchievementType.EXPLORE_ALL_BIOMES)) {
                progress.unlock(AchievementType.EXPLORE_ALL_BIOMES, player);
            }
        }
    }
}
```

### 步骤 4: 添加成就完成动画
**添加成就完成时的视觉效果**:

```java
/**
 * 播放成就完成动画
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
    
    // 播放音效（如果可用）
    player.level().playSound(
        null,
        player.getBlockX(),
        player.getBlockY(),
        player.getBlockZ(),
        net.minecraft.sounds.SoundEvents.ACHIEVEMENT_GRANTED,
        net.minecraft.sounds.SoundSource.PLAYERS,
        1.0f,
        1.0f
    );
}
```

**在 notifyPlayerUnlock() 中调用**:
```java
private void notifyPlayerUnlock(AchievementType achievement) {
    // ... 前面的代码 ...
    
    if (currentPlayer != null) {
        // 发送通知
        currentPlayer.sendSystemMessage(
            net.minecraft.network.chat.Component.literal("§6§l成就解锁§r§f: " + achievementName + " - 新的 buff 槽位已解锁！")
        );
        
        // 播放粒子效果
        playAchievementCompleteAnimation(currentPlayer, achievement);
    }
}
```

### 步骤 5: 添加成就历史记录
**在 PlayerProgress 中添加历史记录**:

```java
public static class PlayerProgress {
    private final Map<AchievementType, Boolean> unlockedAchievements = new HashMap<>();
    private final Map<AchievementType, Integer> progressValues = new HashMap<>();
    private int bossKillCount = 0;
    private Player currentPlayer;
    
    // 添加成就历史
    private final List<AchievementType> achievementHistory = new ArrayList<>();
    
    public void unlock(AchievementType achievement, Player player) {
        this.currentPlayer = player;
        if (!isUnlocked(achievement)) {
            unlockedAchievements.put(achievement, true);
            achievementHistory.add(achievement);
            notifyPlayerUnlock(achievement);
        }
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
            String name = switch (achievement) {
                case DEFEAT_BOSS -> "击败 Boss";
                case EXPLORE_ALL_BIOMES -> "探索世界";
                case DEFEAT_ENDER_DRAGON -> "击败末影龙";
                case BUILD_REDSTONE_MACHINE -> "建造红石机器";
                case COMPLETE_COLLECTION -> "完成收集";
                default -> "未知成就";
            };
            player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal("  §a✓§r §f" + name)
            );
        }
    }
}
```

**在 AchievementTracker 中添加查看命令**:

```java
/**
 * 显示玩家的成就历史
 */
public static void showAchievements(Player player) {
    PlayerProgress progress = PLAYER_PROGRESSES.get(player.getStringUUID().toString());
    if (progress != null) {
        progress.showAchievementHistory(player);
    } else {
        player.sendSystemMessage(
            net.minecraft.network.chat.Component.literal("§e§l勇者之证§r§f: 尚未检测到您的成就数据")
        );
    }
}
```

## 验收标准
- [ ] 成就解锁时发送中文通知
- [ ] 成就解锁时播放金色粒子效果
- [ ] 每 5 个新群系发送进度提示
- [ ] 进度提示格式正确 (X/20, Y%)
- [ ] 成就历史记录功能正常
- [ ] `/herosigil achievements` 命令可用

## 注意事项
1. 通知使用 `§6` (金色) 表示成就
2. 进度提示使用 `§b` (浅蓝色)
3. 粒子效果使用 `ParticleTypes.NOTE` (音符粒子)
4. 音效使用 `SoundEvents.ACHIEVEMENT_GRANTED`
5. 进度提示避免过于频繁（每 5 个群系一次）

## 测试建议
1. 完成成就，验证通知和粒子效果
2. 探索不同群系，观察进度提示
3. 使用命令查看成就历史
4. 检查通知消息格式和颜色
