# 任务 1.4: Buff 音效反馈

## 任务目标
为 Buff 解锁、激活、停用添加音效反馈，提升玩家交互体验。

## 涉及文件
- [BuffEffect.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/buffs/BuffEffect.java) - 添加音效播放
- [ModRegistries.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/registry/ModRegistries.java) - 注册音效
- [HeroSigil.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/HeroSigil.java) - 添加音效配置

## 当前代码分析

**当前状态**: 无音效反馈

**需要的音效**:
1. `buff.unlock`: Buff 解锁成功（清脆的"叮"声）
2. `buff.activate`: Buff 激活（有力的音效）
3. `buff.deactivate`: Buff 停用（下降音调）

## 实现步骤

### 步骤 1: 定义音效资源
**资源文件创建**:

需要创建以下音效文件（使用 `.ogg` 格式）：

```
src/main/resources/assets/herosigil/sounds/
├── buff_unlock.ogg
├── buff_activate.ogg
└── buff_deactivate.ogg
```

**音效建议**:
- `buff_unlock.ogg`: 清脆的"叮"声或 bell 声（高频短促）
- `buff_activate.ogg`: 有力的"嗡"声或 magical 声（中频）
- `buff_deactivate.ogg`: 下降音调或 whoosh 声（中低频）

**音效来源**:
- Freesound.org (免费音效库)
- 使用 Audacity 自制音效
- 使用 Minecraft 现有音效（如 `entity.experience_orb.pickup`）

### 步骤 2: 创建 sounds.json
**文件位置**: `src/main/resources/assets/herosigil/sounds.json`

```json
{
  "herosigil:buff_unlock": {
    "subtitle": "herosigil.subtitle.buff_unlock",
    "sounds": [
      {
        "name": "herosigil:sounds/buff_unlock",
        "stream": false
      }
    ]
  },
  "herosigil:buff_activate": {
    "subtitle": "herosigil.subtitle.buff_activate",
    "sounds": [
      {
        "name": "herosigil:sounds/buff_activate",
        "stream": false
      }
    ]
  },
  "herosigil:buff_deactivate": {
    "subtitle": "herosigil.subtitle.buff_deactivate",
    "sounds": [
      {
        "name": "herosigil:sounds/buff_deactivate",
        "stream": false
      }
    ]
  }
}
```

### 步骤 3: 创建本地化文件
**文件位置**: `src/main/resources/assets/herosigil/lang/zh_cn.json`

```json
{
  "herosigil.subtitle.buff_unlock": "Buff 解锁",
  "herosigil.subtitle.buff_activate": "Buff 激活",
  "herosigil.subtitle.buff_deactivate": "Buff 停用"
}
```

**文件位置**: `src/main/resources/assets/herosigil/lang/en_us.json`

```json
{
  "herosigil.subtitle.buff_unlock": "Buff Unlocked",
  "herosigil.subtitle.buff_activate": "Buff Activated",
  "herosigil.subtitle.buff_deactivate": "Buff Deactivated"
}
```

### 步骤 4: 注册音效
**修改位置**: ModRegistries.java 或 HeroSigil.java

**在 HeroSigil.java 中添加音效注册**:

```java
// 在 HeroSigil 类中添加音效注册表
public static final RegistryObject<SoundEvent> BUFF_UNLOCK = SOUND_EVENTS.register(
    "buff_unlock", 
    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "buff_unlock"))
);

public static final RegistryObject<SoundEvent> BUFF_ACTIVATE = SOUND_EVENTS.register(
    "buff_activate", 
    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "buff_activate"))
);

public static final RegistryObject<SoundEvent> BUFF_DEACTIVATE = SOUND_EVENTS.register(
    "buff_deactivate", 
    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "buff_deactivate"))
);

// 在 init() 方法中确保 SOUND_EVENTS 被注册
```

**或者在 ModRegistries.java 中添加**:

```java
package com.hero.sigil.registry;

import com.hero.sigil.HeroSigil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModRegistries {
    
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(
        NeoForgeRegistries.SOUND_EVENTS, HeroSigil.MODID);
    
    public static final DeferredHolder<SoundEvent, SoundEvent> BUFF_UNLOCK = SOUND_EVENTS.register(
        "buff_unlock", 
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HeroSigil.MODID, "buff_unlock"))
    );
    
    public static final DeferredHolder<SoundEvent, SoundEvent> BUFF_ACTIVATE = SOUND_EVENTS.register(
        "buff_activate", 
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HeroSigil.MODID, "buff_activate"))
    );
    
    public static final DeferredHolder<SoundEvent, SoundEvent> BUFF_DEACTIVATE = SOUND_EVENTS.register(
        "buff_deactivate", 
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HeroSigil.MODID, "buff_deactivate"))
    );
    
    public static void init() {
        SOUND_EVENTS.register(HeroSigil.eventBus);
    }
}
```

**在 HeroSigil.java 的 init() 方法中调用**:
```java
public static void init() {
    // ... 其他注册 ...
    ModRegistries.init(); // 确保音效被注册
}
```

### 步骤 5: 在 BuffEffect 中添加音效播放
**修改位置**: BuffEffect.java 的 toggleActive() 方法

**当前代码**:
```java
public void toggleActive() {
    active = !active;
    
    if (active) {
        HeroSigil.LOGGER.info("Buff {} activated for player {}", id, ...);
    } else {
        Player currentPlayer = net.minecraft.client.Minecraft.getInstance().player;
        if (currentPlayer != null && !currentPlayer.level().isClientSide()) {
            removeBuff(currentPlayer);
        }
        HeroSigil.LOGGER.info("Buff {} deactivated for player {}", id, ...);
    }
}
```

**修改为**:
```java
/**
 * 切换 buff 激活状态
 */
public void toggleActive() {
    boolean wasActive = active;
    active = !active;
    
    if (active) {
        // 播放激活音效
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null && !player.level().isClientSide()) {
            player.level().playSound(
                null,
                player.getBlockX(),
                player.getBlockY(),
                player.getBlockZ(),
                com.hero.sigil.registry.ModRegistries.BUFF_ACTIVATE.get(),
                net.minecraft.sounds.SoundSource.PLAYERS,
                1.0f,
                1.0f
            );
        }
        
        HeroSigil.LOGGER.info("Buff {} activated for player {}", id, 
            player != null ? player.getScoreboardName() : "unknown");
    } else {
        // 播放停用音效
        Player currentPlayer = net.minecraft.client.Minecraft.getInstance().player;
        if (currentPlayer != null && !currentPlayer.level().isClientSide()) {
            player.level().playSound(
                null,
                currentPlayer.getBlockX(),
                currentPlayer.getBlockY(),
                currentPlayer.getBlockZ(),
                com.hero.sigil.registry.ModRegistries.BUFF_DEACTIVATE.get(),
                net.minecraft.sounds.SoundSource.PLAYERS,
                1.0f,
                1.0f
            );
            
            removeBuff(currentPlayer);
        }
        
        HeroSigil.LOGGER.info("Buff {} deactivated for player {}", id, 
            currentPlayer != null ? currentPlayer.getScoreboardName() : "unknown");
    }
}
```

### 步骤 6: 在成就解锁时播放音效
**修改位置**: AchievementTracker.java 的 notifyPlayerUnlock() 方法

**添加音效播放**:
```java
private void notifyPlayerUnlock(AchievementType achievement) {
    String achievementName = switch (achievement) {
        case DEFEAT_BOSS -> "击败 Boss";
        case EXPLORE_ALL_BIOMES -> "探索世界";
        case DEFEAT_ENDER_DRAGON -> "击败末影龙";
        case BUILD_REDSTONE_MACHINE -> "建造红石机器";
        case COMPLETE_COLLECTION -> "完成收集";
        default -> "未知成就";
    };
    
    if (currentPlayer != null) {
        currentPlayer.sendSystemMessage(
            net.minecraft.network.chat.Component.literal("§6§l成就解锁§r§f: " + achievementName + " - 新的 buff 槽位已解锁！")
        );
        
        // 播放解锁音效
        currentPlayer.level().playSound(
            null,
            currentPlayer.getBlockX(),
            currentPlayer.getBlockY(),
            currentPlayer.getBlockZ(),
            com.hero.sigil.registry.ModRegistries.BUFF_UNLOCK.get(),
            net.minecraft.sounds.SoundSource.PLAYERS,
            1.0f,
            1.0f
        );
        
        // 播放粒子效果
        playAchievementCompleteAnimation(currentPlayer, achievement);
    }
}
```

### 步骤 7: 添加音效配置选项（可选）
**在 HeroSigil.java 中添加客户端配置**:

```java
// 添加音效配置字段
public static class ClientConfig {
    public static boolean soundEnabled = true;
    public static float soundVolume = 1.0f;
}

// 在需要播放音效的地方检查配置
if (HeroSigil.ClientConfig.soundEnabled) {
    player.level().playSound(
        null,
        player.getBlockX(),
        player.getBlockY(),
        player.getBlockZ(),
        com.hero.sigil.registry.ModRegistries.BUFF_ACTIVATE.get(),
        net.minecraft.sounds.SoundSource.PLAYERS,
        HeroSigil.ClientConfig.soundVolume,
        1.0f
    );
}
```

## 验收标准
- [ ] 3 个音效文件正确创建并放置
- [ ] sounds.json 文件正确定义音效
- [ ] 本地化文件正确定义音效标题
- [ ] 音效注册表正确注册
- [ ] Buff 解锁时播放解锁音效
- [ ] Buff 激活时播放激活音效
- [ ] Buff 停用时播放停用音效
- [ ] 音效音量可配置（可选）

## 注意事项
1. 音效文件使用 `.ogg` 格式
2. 音效文件放在 `assets/herosigil/sounds/` 目录
3. `sounds.json` 必须放在 `assets/herosigil/` 目录
4. 使用 `SoundSource.PLAYERS` 作为音效源
5. 音量默认为 1.0f

## 测试建议
1. 获取 buff 解锁音效
2. 激活/停用 buff，验证音效播放
3. 调整游戏音量，验证音效音量变化
4. 检查音效文件是否正确加载（查看游戏日志）

## 音效资源推荐
- **Freesound.org**: 搜索 "bell", "magical", "whoosh"
- **Audacity**: 自由录制和编辑音效
- **Minecraft 现有音效**: `entity.experience_orb.pickup`, `ui.button.click`
