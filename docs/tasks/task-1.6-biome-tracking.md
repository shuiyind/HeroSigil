# 任务 1.6: 群系追踪性能优化

## 任务目标
修复群系追踪的类型错误，优化 tick 事件处理频率，减少性能开销，并清理登出玩家的数据防止内存泄漏。

## 涉及文件
- [AchievementTracker.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/achievement/AchievementTracker.java) - 主要修改

## 当前代码分析

### AchievementTracker.java 问题分析

**问题 1: 错误的类型定义 (第 33 行)**
```java
// 当前错误代码
private static final Map<String, Set<ResourceKey<net.minecraft.world.level.Level>>> BIOME_VISITS = new HashMap<>();
// 应该是 ResourceKey<Biome> 而不是 ResourceKey<Level>
```

**问题 2: 每个 tick 都查询群系 (第 111-135 行)**
```java
@SubscribeEvent
public static void onPlayerTick(PlayerTickEvent.Pre event) {
    Player player = event.getPlayer();
    
    // 每个 tick 都执行群系查询，性能开销大
    if (player instanceof ServerPlayer serverPlayer) {
        String playerId = player.getStringUUID().toString();
        
        // 每次 tick 都获取群系
        ResourceKey<net.minecraft.world.level.Level> currentBiome = 
            player.level().getBiome(player.blockPosition()).is(Registries.BIOME);
        
        if (currentBiome != null) {
            BIOME_VISITS.computeIfAbsent(playerId, k -> new HashSet<>()).add(currentBiome);
            
            PlayerProgress progress = getPlayerProgress(player);
            Set<ResourceKey<net.minecraft.world.level.Level>> visitedBiomes = BIOME_VISITS.get(playerId);
            
            if (visitedBiomes.size() >= 20 && !progress.isUnlocked(AchievementType.EXPLORE_ALL_BIOMES)) {
                progress.unlock(AchievementType.EXPLORE_ALL_BIOMES);
            }
        }
    }
}
```

**问题 3: 没有清理机制**
- 玩家登出后，`BIOME_VISITS` 中的数据不会清理
- 长期游戏会导致内存积累

## 实现步骤

### 步骤 1: 修复群系类型错误
**修改位置**: AchievementTracker.java 第 33 行

**当前代码**:
```java
private static final Map<String, Set<ResourceKey<net.minecraft.world.level.Level>>> BIOME_VISITS = new HashMap<>();
```

**修改为**:
```java
// 修复类型：使用 ResourceKey<Biome> 而不是 ResourceKey<Level>
private static final Map<String, Set<ResourceKey<net.minecraft.world.level.Biome>>> BIOME_VISITS = new HashMap<>();
```

### 步骤 2: 优化 tick 事件处理频率
**修改位置**: AchievementTracker.java 第 111-135 行

**当前代码**:
```java
@SubscribeEvent
public static void onPlayerTick(PlayerTickEvent.Pre event) {
    Player player = event.getPlayer();
    
    if (player instanceof ServerPlayer serverPlayer) {
        String playerId = player.getStringUUID().toString();
        
        ResourceKey<net.minecraft.world.level.Level> currentBiome = 
            player.level().getBiome(player.blockPosition()).is(Registries.BIOME);
        
        if (currentBiome != null) {
            BIOME_VISITS.computeIfAbsent(playerId, k -> new HashSet<>()).add(currentBiome);
            
            PlayerProgress progress = getPlayerProgress(player);
            Set<ResourceKey<net.minecraft.world.level.Level>> visitedBiomes = BIOME_VISITS.get(playerId);
            
            if (visitedBiomes.size() >= 20 && !progress.isUnlocked(AchievementType.EXPLORE_ALL_BIOMES)) {
                progress.unlock(AchievementType.EXPLORE_ALL_BIOMES);
            }
        }
    }
}
```

**修改为**:
```java
@SubscribeEvent
public static void onPlayerTick(PlayerTickEvent.Pre event) {
    Player player = event.getPlayer();
    
    if (player instanceof ServerPlayer serverPlayer) {
        // 每 100 tick (5 秒) 记录一次群系，而非每个 tick
        if (player.tickCount % 100 != 0) {
            return;
        }
        
        String playerId = player.getStringUUID().toString();
        
        // 已访问 50 个群系后停止记录（超过 20 已解锁成就）
        Set<ResourceKey<net.minecraft.world.level.Biome>> visitedBiomes = BIOME_VISITS.get(playerId);
        if (visitedBiomes != null && visitedBiomes.size() >= 50) {
            return;
        }
        
        // 获取当前群系（修复类型）
        var biomeHolder = player.level().getBiome(player.blockPosition());
        var currentBiome = biomeHolder.unwrapKey().orElse(null);
        
        if (currentBiome != null) {
            BIOME_VISITS.computeIfAbsent(playerId, k -> new HashSet<>()).add(currentBiome);
            
            PlayerProgress progress = getPlayerProgress(player);
            
            // 检查是否解锁成就
            if (visitedBiomes.size() >= 20 && !progress.isUnlocked(AchievementType.EXPLORE_ALL_BIOMES)) {
                progress.unlock(AchievementType.EXPLORE_ALL_BIOMES);
            }
        }
    }
}
```

### 步骤 3: 添加玩家登出时清理
**修改位置**: AchievementTracker.java 文件中，在现有的事件处理器附近添加新的事件监听器

**添加代码**:
```java
@SubscribeEvent
public static void onPlayerLogout(PlayerEvent.LoggedOutEvent event) {
    String playerId = event.getPlayer().getStringUUID().toString();
    
    // 清理群系访问数据
    BIOME_VISITS.remove(playerId);
    
    // 清理成就进度数据
    PLAYER_PROGRESSES.remove(playerId);
}
```

**需要添加的导入**:
```java
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
```

### 步骤 4: 优化群系获取逻辑
**修改位置**: AchievementTracker.java 第 119-120 行附近

**当前代码**:
```java
ResourceKey<net.minecraft.world.level.Level> currentBiome = 
    player.level().getBiome(player.blockPosition()).is(Registries.BIOME);
```

**修改为**:
```java
// 使用 unwrapKey() 获取 ResourceKey<Biome>
var biomeHolder = player.level().getBiome(player.blockPosition());
var currentBiome = biomeHolder.unwrapKey().orElse(null);
```

## 验收标准
- [ ] `BIOME_VISITS` 的类型为 `Map<String, Set<ResourceKey<Biome>>>`
- [ ] 群系查询每 100 tick (5 秒) 执行一次
- [ ] 访问 50 个群系后停止记录
- [ ] 玩家登出时清理 `BIOME_VISITS` 和 `PLAYER_PROGRESSES`
- [ ] 访问 20 个群系后正确解锁成就
- [ ] 性能测试：群系查询开销减少 99.5%

## 注意事项
1. 使用 `biomeHolder.unwrapKey().orElse(null)` 获取 `ResourceKey<Biome>`
2. 每 100 tick 检查一次，使用 `player.tickCount % 100 == 0` 判断
3. 清理逻辑必须同时清理 `BIOME_VISITS` 和 `PLAYER_PROGRESSES`
4. 添加 `PlayerEvent.LoggedOutEvent` 导入

## 测试建议
1. 启动游戏，创建新玩家，观察群系记录频率
2. 使用 `/time set day` 快速切换时间，验证群系记录不受影响
3. 登出再登录，验证旧玩家数据已清理
4. 使用 F3 性能界面观察 TPS 变化
