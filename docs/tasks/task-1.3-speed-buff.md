# 任务 1.3: 加速 Buff 效果实现

## 任务目标
实现加速 buff 的周期性刷新逻辑，添加速度变化视觉反馈，处理 buff 冲突。

## 涉及文件
- [BuffEffect.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/buffs/BuffEffect.java) - 主要修改
- [HeroSigilData.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/buffs/HeroSigilData.java) - 辅助修改
- [HeroSigilItem.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/item/HeroSigilItem.java) - 辅助修改

## 当前代码分析

### BuffEffect.java 问题分析
- 加速 buff 定义 (第 128-135 行):
```java
buffs.add(new BuffEffect(
    "speed",
    MobEffects.SPEED,
    1,      // +45% speed (amplifier level 0 = 20%, level 1 = 45%)
    30,     // Duration in seconds
    false   // Not permanent - needs periodic refresh
));
```

**Minecraft 加速机制**:
- `MobEffects.SPEED` 放大器 0 = 20% 加速
- 放大器 1 = 45% 加速 (20% + 25%)
- 非永久 buff 需要周期性刷新

**问题**:
1. 非永久 buff 的刷新逻辑已在任务 1.1 中实现，但加速 buff 有额外需求
2. 需要添加速度变化的视觉反馈（粒子效果）
3. 需要处理 buff 冲突（玩家已有其他速度来源时）

## 实现步骤

### 步骤 1: 添加速度来源追踪
**修改位置**: BuffEffect.java 类定义部分

**在 BuffEffect 类中添加速度来源字段**:

```java
public class BuffEffect {
    private final String id;
    private final MobEffect mobEffect;
    private final int amplifier;
    private final int durationSeconds;
    private final boolean isPermanent;
    private final int refreshIntervalSeconds;
    private long lastRefreshTick = 0;
    
    private boolean unlocked = false;
    private boolean active = false;
    
    // 添加速度来源追踪（仅用于 SPEED buff）
    private boolean appliedByThisBuff = false;
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
    this.refreshIntervalSeconds = isPermanent ? 0 : (int) (durationSeconds * 0.8);
    this.lastRefreshTick = 0;
    this.appliedByThisBuff = false;
}
```

### 步骤 2: 修改 applyBuff() 方法处理加速 buff
**修改位置**: BuffEffect.java 第 37-55 行 (在任务 1.1 修改的基础上)

**为加速 buff 添加特殊处理**:

```java
/**
 * 应用 buff 效果到玩家
 */
public void applyBuff(Player player) {
    if (!active || !unlocked) return;
    
    if (isPermanent) {
        // 永久 buff 逻辑（保持任务 1.1 的修改）
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
        
        if (mobEffect == MobEffects.HEALTH_BOOST) {
            player.refreshMaxHealth();
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
            int heartsAdded = amplifier * 2;
            player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal("§a§l勇者之证§r§f: 最大生命值 +" + heartsAdded + " 颗心！")
            );
            player.level().addParticle(
                net.minecraft.core.particles.ParticleTypes.HEART,
                player.getX(), player.getY() + 1.0, player.getZ(),
                0.0, 0.0, 0.0
            );
        }
        
        HeroSigil.LOGGER.debug("Applied permanent buff {} to player {}", id, player.getScoreboardName());
        return;
    }
    
    // 非永久 buff 逻辑
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
    
    // 加速 buff 特殊处理：添加视觉反馈
    if (mobEffect == MobEffects.SPEED) {
        appliedByThisBuff = true;
        
        // 播放速度粒子效果（淡蓝色粒子）
        player.level().addParticle(
            net.minecraft.core.particles.ParticleTypes.SLEEP,
            player.getX(), player.getY() + 0.5, player.getZ(),
            0.0, 0.0, 0.0
        );
        
        // 通知玩家
        player.sendSystemMessage(
            net.minecraft.network.chat.Component.literal("§b§l勇者之证§r§f: 加速效果已激活！")
        );
    }
    
    HeroSigil.LOGGER.debug("Applied/refreshed buff {} to player {} (interval: {}s)", 
        id, player.getScoreboardName(), refreshIntervalSeconds);
}
```

### 步骤 3: 修改 removeBuff() 方法处理加速 buff
**修改位置**: BuffEffect.java 第 60-64 行

**为加速 buff 添加特殊处理**:

```java
/**
 * 从玩家移除 buff 效果
 */
public void removeBuff(Player player) {
    if (player.hasEffect(mobEffect)) {
        player.removeEffect(mobEffect);
        lastRefreshTick = 0;
        
        // 加速 buff 特殊处理
        if (mobEffect == MobEffects.SPEED) {
            appliedByThisBuff = false;
            
            // 播放速度移除粒子效果
            player.level().addParticle(
                net.minecraft.core.particles.ParticleTypes.CLOUD,
                player.getX(), player.getY() + 0.5, player.getZ(),
                0.0, 0.0, 0.0
            );
            
            // 通知玩家
            player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal("§c§l勇者之证§r§f: 加速效果已移除")
            );
        }
        
        HeroSigil.LOGGER.debug("Removed buff {} from player {}", id, player.getScoreboardName());
    }
}
```

### 步骤 4: 实现 buff 冲突处理
**在 applyBuff() 方法中添加冲突检测**:

```java
// 在 applyBuff() 方法中，应用效果前添加冲突检测:

// 加速 buff 冲突处理
if (mobEffect == MobEffects.SPEED) {
    MobEffectInstance existingEffect = player.getEffect(mobEffect);
    if (existingEffect != null && existingEffect.getAmplifier() > amplifier) {
        // 玩家已有更强的加速效果，暂不覆盖
        HeroSigil.LOGGER.debug("Skipping SPEED buff application, stronger effect already active");
        return;
    }
}
```

### 步骤 5: 添加 getter 方法
**在 BuffEffect 类末尾添加**:

```java
/**
 * 检查此 buff 是否已应用（用于冲突处理）
 */
public boolean isAppliedByThisBuff() {
    return appliedByThisBuff;
}
```

### 步骤 6: 在 HeroSigilData 中添加 buff 冲突检测
**在 HeroSigilData.java 中添加冲突处理方法**:

```java
/**
 * 检查指定 buff 是否可以应用（考虑冲突）
 */
public static boolean canApplyBuff(Player player, BuffEffect buff) {
    if (buff.getMobEffect() == MobEffects.SPEED) {
        MobEffectInstance existingEffect = player.getEffect(buff.getMobEffect());
        if (existingEffect != null) {
            // 如果已有更强的加速效果，返回 false
            return existingEffect.getAmplifier() <= buff.getAmplifier();
        }
    }
    return true;
}
```

## 验收标准
- [ ] 加速 buff 激活后玩家速度 +45%
- [ ] 非永久 buff 每 24 秒自动刷新
- [ ] 激活时播放淡蓝色粒子效果
- [ ] 停用播放云朵粒子效果
- [ ] 玩家收到中文通知
- [ ] 已有更强加速效果时不覆盖
- [ ] 日志记录速度变化

## 注意事项
1. 加速器效果放大器 1 = 45% 加速
2. 粒子效果使用 `ParticleTypes.SLEEP` (淡蓝色) 和 `ParticleTypes.CLOUD` (白色)
3. 通知消息使用 `§b` (浅蓝色) 表示加速
4. 冲突处理：不覆盖玩家已有的更强加速效果

## 测试建议
1. 装备饰品并激活加速 buff，观察速度变化
2. 使用 `/effect @s minecraft:speed 999 2` 生成更强的加速效果，验证不覆盖
3. 停用 buff 后观察速度恢复
4. 查看日志中的刷新记录
5. 观察粒子效果是否正确播放
