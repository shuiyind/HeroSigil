# 阶段一：核心 Buff 机制实现 - 工作提示词

## 📌 任务概述
实现三大 Buff（饱和/生命提升/加速）的核心机制，包括周期性刷新、最大生命值处理、速度变化视觉反馈。

## 🎯 子代理需要处理的任务

### 任务 1.1: 饱和 Buff 效果实现
**涉及文件**:
- `BuffEffect.java` - 添加刷新间隔字段、修改 applyBuff/removeBuff 方法
- `HeroSigilData.java` - 添加刷新计时器
- `HeroSigilItem.java` - 优化刷新逻辑

**关键修改点**:
1. 在 `BuffEffect` 类中添加 `refreshIntervalSeconds` 和 `lastRefreshTick` 字段
2. 修改构造函数，计算刷新间隔 = 持续时间 × 0.8
3. 修改 `applyBuff()` 方法，添加周期性刷新逻辑
4. 修改 `removeBuff()` 方法，重置刷新计时器
5. 在 `HeroSigilItem.onPlayerTick()` 中实现独立刷新

**验收标准**:
- [ ] 饱和 buff（或替代效果）正确应用到玩家
- [ ] 非永久 buff 在合适的时间点自动刷新
- [ ] 刷新间隔为持续时间的 80%
- [ ] 已有充足剩余时间时跳过刷新

### 任务 1.2: 生命提升 Buff 效果实现
**涉及文件**:
- `BuffEffect.java` - 处理永久 buff 的最大生命值变化
- `HeroSigilData.java` - 永久 buff 同步
- `HeroSigilItem.java` - 辅助修改

**关键修改点**:
1. 在 `applyBuff()` 的永久 buff 分支中添加 `refreshMaxHealth()` 调用
2. 在 `removeBuff()` 中添加最大生命值恢复逻辑
3. 添加当前生命值调整（不超过新最大值）
4. 添加玩家通知（中文消息）
5. 添加粒子效果（HEART/DAMAGE_INDICATOR）

**验收标准**:
- [ ] 生命提升 buff 激活后玩家最大生命值 +4
- [ ] 当前生命值正确调整
- [ ] 停用 buff 后最大生命值恢复
- [ ] 玩家收到中文通知
- [ ] 激活/停用时有粒子效果

### 任务 1.3: 加速 Buff 效果实现
**涉及文件**:
- `BuffEffect.java` - 添加速度来源追踪、修改 applyBuff/removeBuff 方法
- `HeroSigilData.java` - 添加冲突处理方法
- `HeroSigilItem.java` - 辅助修改

**关键修改点**:
1. 在 `BuffEffect` 类中添加 `appliedByThisBuff` 字段
2. 修改 `applyBuff()` 方法，添加加速 buff 特殊处理
3. 添加速度变化的视觉反馈（粒子效果）
4. 实现 buff 冲突处理（不覆盖更强的加速效果）
5. 在 `HeroSigilData` 中添加 `canApplyBuff()` 冲突检测方法

**验收标准**:
- [ ] 加速 buff 激活后玩家速度 +45%
- [ ] 非永久 buff 每 24 秒自动刷新
- [ ] 激活时播放淡蓝色粒子效果
- [ ] 停用播放云朵粒子效果
- [ ] 已有更强加速效果时不覆盖

## 🔧 执行步骤

### 步骤 1: 阅读任务文档
仔细阅读 `task-1.1-saturation-buff.md`、`task-1.2-health-boost-buff.md`、`task-1.3-speed-buff.md`

### 步骤 2: 阅读当前代码
阅读涉及的源文件，理解现有代码结构：
- [BuffEffect.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/buffs/BuffEffect.java)
- [HeroSigilData.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/buffs/HeroSigilData.java)
- [HeroSigilItem.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/item/HeroSigilItem.java)

### 步骤 3: 按顺序实现
1. 先实现任务 1.1（基础刷新机制）
2. 再实现任务 1.2（生命提升永久 buff）
3. 最后实现任务 1.3（加速 buff 冲突处理）

### 步骤 4: 验证与测试
- [ ] 编译检查通过
- [ ] 刷新逻辑正确
- [ ] 最大生命值处理正确
- [ ] 速度变化视觉反馈正常
- [ ] 冲突处理正确

## ⚠️ 注意事项
1. 所有修改基于任务文档中的"当前代码分析"部分
2. 按照"修改为"部分的代码示例进行实现
3. 保持代码风格一致，添加中文注释
4. 使用 `HeroSigil.LOGGER` 进行日志记录
5. 通知消息使用 Minecraft 颜色代码（§a/§c/§b 等）
6. 确保非永久 buff 的刷新间隔为持续时间的 80%
