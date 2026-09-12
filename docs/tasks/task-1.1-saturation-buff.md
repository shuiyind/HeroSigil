# 任务 1.1: 饱和 Buff 效果实现

## 任务目标
实现饱和 Buff 的周期性刷新逻辑，确保非永久 buff 效果持续不中断。

## 涉及文件
- [BuffEffect.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/buffs/BuffEffect.java) - 主要修改
- [HeroSigilData.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/buffs/HeroSigilData.java) - 添加刷新计时器
- [HeroSigilItem.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/item/HeroSigilItem.java) - 优化刷新逻辑

## 当前代码分析

### BuffEffect.java 问题分析
- `applyBuff()` 方法 (第 37-55 行): 已实现但非永久 buff 需要周期性刷新
- `toggleActive()` 方法 (第 69-81 行): 停用 buff 时尝试移除效果，但使用了客户端单例
- 饱和 buff 定义 (第 111-117 行): 持续时间 30 秒，非永久

**当前饱和 buff 定义**:
```java
buffs.add(new BuffEffect(
    "saturation",
    MobEffects.SATURATION,  // 检查在 1.21.1 中是否存在
    0,                      // Amplifier level
    30,                     // Duration in seconds
    false                   // Not permanent
));
```

**问题**:
1. 如果 `MobEffects.SATURATION` 在 1.21.1 中不存在，需要使用替代效果
2. 非永久 buff 的刷新逻辑依赖 `HeroSigilItem.onPlayerTick()` 每 100 tick 调用
3. 没有精确的刷新间隔控制，可能提前或延迟刷新

### HeroSigilData.java 问题分析
- 缺少每个 buff 的最后刷新时间记录
- 无法精确控制刷新间隔

## 实现步骤

### 步骤 1: 验证 SATURATION 效果是否存在
**检查位置**: BuffEffect.java 第 111-117 行

在 Minecraft 1.21.1 中，`MobEffects.SATURATION` 可能不存在。如果编译错误，使用替代方案：

**方案 A**: 使用 `MobEffects.REGENERATION` (再生效果)
```java
// Slot 1: Regeneration - from DEFEAT_BOSS achievement
buffs.add(new BuffEffect(
    "regeneration",
    MobEffects.REGENERATION,
    0,      // Amplifier level (基础再生速度)
    30,     // Duration in seconds
    false   // Not permanent
));
```

**方案 B**: 使用 `MobEffects.ABSORPTION` (护盾效果)
```java
// Slot 1: Absorption - from DEFEAT_BOSS achievement
buffs.add(new BuffEffect(
    "absorption",
    MobEffects.ABSORPTION,
    0,      // +2 颗心护盾
    30,     // Duration in seconds
    false   // Not permanent
));
```

**建议**: 如果 `MobEffects.SATURATION` 可用，保持不变。如果不可用，使用 `MobEffects.ABSORPTION` 作为替代（更符合"饱腹感"的概念）。

### 步骤 2: 在 BuffEffect 中添加刷新间隔字段
**修改位置**: BuffEffect.java 类定义部分（第 14-24 行附近）

**当前代码**:
```java
public class BuffEffect {
    private final String id;
    private final MobEffect mobEffect;
    private final int amplifier;
    private final int durationSeconds;
    private final boolean isPermanent;
    
    private boolean unlocked = false;
    private boolean active = false;
}
```

**修改为**:
```java
public class BuffEffect {
    private final String id;
    private final MobEffect mobEffect;
    private final int amplifier;
    private final int durationSeconds;
    private final boolean isPermanent;
    
    // 添加刷新间隔字段（秒）
    private final int refreshIntervalSeconds;
    
    // 当前激活状态的最后刷新时间（tick 计数）
    private long lastRefreshTick = 0;
    
    private boolean unlocked = false;
    private boolean active = false;
}
```

**修改构造函数**:
```java
public BuffEffect(String id, MobEffect mobEffect, int amplifier, int durationSeconds, boolean isPermanent) {
    this.id = id;
    this.mobEffect = mobEffect;
    this.amplifier = amplifier;
    this.durationSeconds = durationSeconds;
    this.isPermanent = isPermanent;
    // 非永久 buff 的刷新间隔为持续时间的 80%
    this.refreshIntervalSeconds = isPermanent ? 0 : (int) (durationSeconds * 0.8);
    this.lastRefreshTick = 0;
}
```

### 步骤 3: 修改 applyBuff() 方法
**修改位置**: BuffEffect.java 第 37-55 行

**当前代码**:
```java
public void applyBuff(Player player) {
    if (!active || !unlocked) return;
    
    int duration = isPermanent ? 999999 : durationSeconds;
    
    player.removeEffect(mobEffect);
    
    MobEffectInstance effectInstance = new MobEffectInstance(
        mobEffect, 
        duration * 20,
        amplifier,
        false,
        true
    );
    
    player.addEffect(effectInstance);
}
```

**修改为**:
```java
/**
 * 应用 buff 效果到玩家
 */
public void applyBuff(Player player) {
    if (!active || !unlocked) return;
    
    // 如果是永久 buff，直接应用
    if (isPermanent) {
        int duration = 999999;
        player.removeEffect(mobEffect);
        
        MobEffectInstance effectInstance = new MobEffectInstance(
            mobEffect, 
            duration * 20,
            amplifier,
            false,
            true
        );
        
        player.addEffect(effectInstance);
        HeroSigil.LOGGER.debug("Applied permanent buff {} to player {}", id, player.getScoreboardName());
        return;
    }
    
    // 非永久 buff: 检查是否需要刷新
    long currentTick = player.tickCount;
    long refreshIntervalTicks = refreshIntervalSeconds * 20;
    
    // 如果距离上次刷新时间充足，跳过本次刷新
    if (lastRefreshTick > 0 && (currentTick - lastRefreshTick) < refreshIntervalTicks) {
        return;
    }
    
    // 检查玩家是否已有该效果，且剩余时间充足
    MobEffectInstance existingEffect = player.getEffect(mobEffect);
    if (existingEffect != null) {
        int remainingTicks = existingEffect.getDuration();
        if (remainingTicks > refreshIntervalTicks * 0.8) {
            // 剩余时间充足，跳过刷新
            return;
        }
    }
    
    // 应用新的效果实例
    int duration = durationSeconds;
    player.removeEffect(mobEffect);
    
    MobEffectInstance effectInstance = new MobEffectInstance(
        mobEffect, 
        duration * 20,
        amplifier,
        false,
        true
    );
    
    player.addEffect(effectInstance);
    
    // 记录刷新时间
    lastRefreshTick = currentTick;
    
    HeroSigil.LOGGER.debug("Applied/refreshed buff {} to player {} (interval: {}s)", 
        id, player.getScoreboardName(), refreshIntervalSeconds);
}
```

### 步骤 4: 修改 removeBuff() 方法
**修改位置**: BuffEffect.java 第 60-64 行

**当前代码**:
```java
public void removeBuff(Player player) {
    if (player.hasEffect(mobEffect)) {
        player.removeEffect(mobEffect);
    }
}
```

**修改为**:
```java
/**
 * 从玩家移除 buff 效果
 */
public void removeBuff(Player player) {
    if (player.hasEffect(mobEffect)) {
        player.removeEffect(mobEffect);
        lastRefreshTick = 0; // 重置刷新计时器
        HeroSigil.LOGGER.debug("Removed buff {} from player {}", id, player.getScoreboardName());
    }
}
```

### 步骤 5: 在 toggleActive() 中添加音效提示日志
**修改位置**: BuffEffect.java 第 69-81 行

**当前代码**:
```java
public void toggleActive() {
    active = !active;
    
    if (active) {
        // Will be applied on next tick by HeroSigilItem.onPlayerTick()
    } else {
        Player currentPlayer = net.minecraft.client.Minecraft.getInstance().player;
        if (currentPlayer != null && !currentPlayer.level().isClientSide()) {
            removeBuff(currentPlayer);
        }
    }
}
```

**修改为**:
```java
/**
 * 切换 buff 激活状态
 */
public void toggleActive() {
    active = !active;
    
    if (active) {
        HeroSigil.LOGGER.info("Buff {} activated for player {}", id, 
            net.minecraft.client.Minecraft.getInstance().player != null ? 
            net.minecraft.client.Minecraft.getInstance().player.getScoreboardName() : "unknown");
    } else {
        Player currentPlayer = net.minecraft.client.Minecraft.getInstance().player;
        if (currentPlayer != null && !currentPlayer.level().isClientSide()) {
            removeBuff(currentPlayer);
        }
        HeroSigil.LOGGER.info("Buff {} deactivated for player {}", id, 
            currentPlayer != null ? currentPlayer.getScoreboardName() : "unknown");
    }
}
```

### 步骤 6: 在 HeroSigilItem.onPlayerTick() 中优化刷新逻辑
**修改位置**: HeroSigilItem.java 第 92-115 行

**当前代码**:
```java
public static void onPlayerTick(Player player) {
    if (player.level().isClientSide() || !(player instanceof net.minecraft.server.level.ServerPlayer)) {
        return;
    }
    
    ItemStack sigilStack = getEquippedSigil(player);
    if (sigilStack.isEmpty()) {
        return;
    }
    
    int tickCount = player.tickCount;
    if (tickCount % BUFF_REFRESH_INTERVAL == 0) {
        com.hero.sigil.buffs.HeroSigilData.applyAllBuffs(player);
        
        com.hero.sigil.network.BuffSlotSyncPacket packet = new com.hero.sigil.network.BuffSlotSyncPacket(
            player.getId(), 
            com.hero.sigil.buffs.HeroSigilData.getBuffStatesArray(player)
        );
        com.hero.sigil.network.HeroSigilNetworkManager.sendToPlayer(packet, (net.minecraft.server.level.ServerPlayer) player);
    }
}
```

**修改为**:
```java
public static void onPlayerTick(Player player) {
    if (player.level().isClientSide() || !(player instanceof net.minecraft.server.level.ServerPlayer)) {
        return;
    }
    
    ItemStack sigilStack = getEquippedSigil(player);
    if (sigilStack.isEmpty()) {
        return;
    }
    
    // 对每个激活的 buff 独立刷新
    java.util.List<com.hero.sigil.buffs.BuffEffect> buffs = com.hero.sigil.buffs.HeroSigilData.getAllBuffs();
    boolean needsSync = false;
    
    for (com.hero.sigil.buffs.BuffEffect buff : buffs) {
        if (buff.isActive() && buff.isUnlocked()) {
            buff.applyBuff(player);
            if (buff.getDurationSeconds() > 0) { // 非永久 buff
                needsSync = true;
            }
        }
    }
    
    // 仅在必要时同步状态
    if (needsSync) {
        com.hero.sigil.network.BuffSlotSyncPacket packet = new com.hero.sigil.network.BuffSlotSyncPacket(
            player.getId(), 
            com.hero.sigil.buffs.HeroSigilData.getBuffStatesArray(player)
        );
        com.hero.sigil.network.HeroSigilNetworkManager.sendToPlayer(packet, (net.minecraft.server.level.ServerPlayer) player);
    }
}
```

## 验收标准
- [ ] 饱和 buff (或替代效果) 正确应用到玩家
- [ ] 非永久 buff 在合适的时间点自动刷新
- [ ] 刷新间隔为持续时间的 80% (例如 30 秒 buff 每 24 秒刷新)
- [ ] 已有充足剩余时间时跳过刷新
- [ ] 停用 buff 时效果立即移除
- [ ] 应用/移除 buff 时有日志记录
- [ ] 刷新逻辑优化，减少不必要的网络同步

## 注意事项
1. `MobEffects.SATURATION` 在 1.21.1 中可能不存在，需要备选方案
2. 刷新间隔 = 持续时间 × 0.8，避免效果中断
3. 永久 buff 不需要周期性刷新
4. 日志使用 `HeroSigil.LOGGER`，级别为 `DEBUG` 或 `INFO`

## 测试建议
1. 获取饱和 buff 后，观察效果是否持续不中断
2. 使用 `/effect clear` 清除效果后，验证 buff 重新应用
3. 停用 buff 后，使用 `/effect` 命令检查效果是否移除
4. 查看日志文件，验证刷新和同步逻辑
