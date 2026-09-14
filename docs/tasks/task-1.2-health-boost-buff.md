# 任务 1.2: 生命提升 Buff 效果实现

## 任务目标
实现生命提升永久 buff 的最大生命值处理逻辑，包括激活时增加最大生命值、停用时恢复。

## 涉及文件
- [BuffEffect.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/buffs/BuffEffect.java) - 主要修改
- [HeroSigilData.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/buffs/HeroSigilData.java) - 辅助修改
- [HeroSigilItem.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/item/HeroSigilItem.java) - 辅助修改

## 当前代码分析

### BuffEffect.java 问题分析
- 生命提升 buff 定义 (第 119-126 行):
```java
buffs.add(new BuffEffect(
    "health_boost",
    MobEffects.HEALTH_BOOST,
    1,      // +4 max health (each level adds 2 hearts)
    0,      // 0 = permanent effect
    true    // Permanent while equipped
));
```

**问题**:
1. `applyBuff()` 方法没有处理永久 buff 的最大生命值变化
2. `removeBuff()` 方法没有处理永久 buff 的最大生命值恢复
3. 玩家当前生命值可能超过新的最大生命值

**Minecraft 生命提升机制**:
- `MobEffects.HEALTH_BOOST` 每个放大器等级 (+2) 增加 4 点最大生命值 (+2 颗心)
- 应用永久效果后需要调用 `player.refreshMaxHealth()` 重新计算
- 需要调整当前生命值不超过新最大值

## 实现步骤

### 步骤 1: 修改 applyBuff() 方法处理永久 buff
**修改位置**: BuffEffect.java 第 37-55 行 (在步骤 1.1 修改的基础上)

**在 applyBuff() 方法中，永久 buff 分支添加最大生命值处理**:

```java
/**
 * 应用 buff 效果到玩家
 */
public void applyBuff(Player player) {
    if (!active || !unlocked) return;
    
    // 如果是永久 buff，需要处理最大生命值
    if (isPermanent) {
        int duration = 999999;
        
        // 先移除已有的效果实例（避免重复添加）
        player.removeEffect(mobEffect);
        
        MobEffectInstance effectInstance = new MobEffectInstance(
            mobEffect, 
            duration * 20,
            amplifier,
            false,
            true
        );
        
        player.addEffect(effectInstance);
        
        // 处理永久 buff 的最大生命值变化
        if (mobEffect == MobEffects.HEALTH_BOOST) {
            // 重新计算最大生命值
            player.refreshMaxHealth();
            
            // 调整当前生命值（不超过新最大值）
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
            
            HeroSigil.LOGGER.info("Applied HEALTH_BOOST to player {}, new max health: {}", 
                player.getScoreboardName(), player.getMaxHealth());
        }
        
        HeroSigil.LOGGER.debug("Applied permanent buff {} to player {}", id, player.getScoreboardName());
        return;
    }
    
    // 非永久 buff 逻辑... (保持步骤 1.1 的修改)
}
```

### 步骤 2: 修改 removeBuff() 方法处理永久 buff
**修改位置**: BuffEffect.java 第 60-64 行

**在 removeBuff() 方法中添加永久 buff 处理**:

```java
/**
 * 从玩家移除 buff 效果
 */
public void removeBuff(Player player) {
    if (player.hasEffect(mobEffect)) {
        player.removeEffect(mobEffect);
        lastRefreshTick = 0;
        
        // 处理永久 buff 的最大生命值恢复
        if (mobEffect == MobEffects.HEALTH_BOOST) {
            // 重新计算最大生命值（移除生命提升）
            player.refreshMaxHealth();
            
            // 调整当前生命值（不超过新最大值）
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
            
            HeroSigil.LOGGER.info("Removed HEALTH_BOOST from player {}, new max health: {}", 
                player.getScoreboardName(), player.getMaxHealth());
        }
        
        HeroSigil.LOGGER.debug("Removed buff {} from player {}", id, player.getScoreboardName());
    }
}
```

### 步骤 3: 添加最大生命值变化通知
**在 applyBuff() 和 removeBuff() 中添加玩家通知**:

```java
// 在 applyBuff() 的永久 buff 分支中添加:
if (mobEffect == MobEffects.HEALTH_BOOST) {
    // 通知玩家最大生命值变化
    int heartsAdded = amplifier * 2; // 每个放大器 +2 颗心
    player.sendSystemMessage(
        net.minecraft.network.chat.Component.literal("§a§l勇者之证§r§f: 最大生命值 +" + heartsAdded + " 颗心！")
    );
}

// 在 removeBuff() 的永久 buff 分支中添加:
if (mobEffect == MobEffects.HEALTH_BOOST) {
    // 通知玩家最大生命值恢复
    int heartsRemoved = amplifier * 2;
    player.sendSystemMessage(
        net.minecraft.network.chat.Component.literal("§c§l勇者之证§r§f: 最大生命值 -" + heartsRemoved + " 颗心")
    );
}
```

### 步骤 4: 实现生命值变化动画效果
**在 applyBuff() 和 removeBuff() 中添加粒子效果**:

```java
// 在 applyBuff() 的永久 buff 分支中添加:
if (mobEffect == MobEffects.HEALTH_BOOST) {
    // 播放生命值增长粒子效果
    player.level().addParticle(
        net.minecraft.core.particles.ParticleTypes.HEART,
        player.getX(), player.getY() + 1.0, player.getZ(),
        0.0, 0.0, 0.0
    );
}

// 在 removeBuff() 的永久 buff 分支中添加:
if (mobEffect == MobEffects.HEALTH_BOOST) {
    // 播放生命值减少粒子效果（红色）
    player.level().addParticle(
        net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR,
        player.getX(), player.getY() + 1.0, player.getZ(),
        0.0, 0.0, 0.0
    );
}
```

### 步骤 5: 修改 toggleActive() 方法
**修改位置**: BuffEffect.java 第 69-81 行

**确保 toggleActive() 方法正确处理永久 buff**:

```java
/**
 * 切换 buff 激活状态
 */
public void toggleActive() {
    boolean wasActive = active;
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

### 步骤 6: 在 HeroSigilData 中添加永久 buff 同步
**修改位置**: HeroSigilData.java 第 112-133 行 (syncFromAchievements 方法)

**在激活永久 buff 时立即应用**:

```java
public static void syncFromAchievements(Player player) {
    java.util.List<BuffEffect> buffs = getAllBuffs();
    
    for (int i = 0; i < Math.min(buffs.size(), AchievementTracker.AchievementType.values().length); i++) {
        AchievementTracker.AchievementType achievement = AchievementTracker.AchievementType.values()[i];
        
        if (com.hero.sigil.achievement.AchievementTracker.isAchievementUnlocked(player, achievement)) {
            buffs.get(i).setUnlocked(true);
            
            if (!buffs.get(i).isActive()) {
                buffs.get(i).toggleActive();
                
                // 对永久 buff 立即应用
                if (buffs.get(i).isPermanent() && !player.level().isClientSide() && 
                    player instanceof net.minecraft.server.level.ServerPlayer) {
                    buffs.get(i).applyBuff(player);
                }
            }
        }
    }
}
```

## 验收标准
- [ ] 生命提升 buff 激活后玩家最大生命值 +4
- [ ] 当前生命值正确调整（不超过新最大值）
- [ ] 停用 buff 后最大生命值恢复
- [ ] 当前生命值正确调整（不超过恢复后的最大值）
- [ ] 玩家收到中文通知（最大生命值变化）
- [ ] 激活/停用时有粒子效果
- [ ] 日志记录最大生命值变化

## 注意事项
1. `player.refreshMaxHealth()` 是重新计算最大生命值的关键方法
2. 应用/移除效果后必须调用 `refreshMaxHealth()`
3. 调整当前生命值时不能超过新的最大生命值
4. 通知消息使用 `§a` (绿色) 表示增加，`§c` (红色) 表示减少
5. 粒子效果使用 `ParticleTypes.HEART` (粉色爱心) 和 `ParticleTypes.DAMAGE_INDICATOR` (红色)

## 测试建议
1. 装备饰品并激活生命提升 buff，观察最大生命值变化
2. 使用 `/health` 命令测试生命值调整
3. 停用 buff 后验证最大生命值恢复
4. 检查日志中的最大生命值变化记录
5. 观察粒子效果是否正确播放
