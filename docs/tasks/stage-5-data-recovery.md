# 阶段五：数据恢复 - 工作提示词

## 📌 任务概述
实现数据恢复机制，支持从备份或损坏的 NBT 数据中恢复玩家进度。

## ⚠️ 当前状态说明

**已完成的基础功能** (在 `HeroSigilData.java` 中):
- ✅ `validateData()` - 数据完整性检查
- ✅ `createBackup()` - 创建数据备份
- ✅ `restoreFromBackup()` - 从备份恢复
- ✅ `migrateData()` - 数据迁移
- ✅ `rollbackData()` - 数据回滚

**待补充的功能**:
- ❌ 独立的 `DataRecovery.java` 类
- ❌ `/herosigil recover` 命令
- ❌ 自动恢复触发机制

## 🎯 子代理需要处理的任务

### 任务 1.10: 数据恢复（补充实现）
**涉及文件**:
- `DataRecovery.java` - 恢复逻辑（**新文件**）
- `HeroSigilItem.java` - 恢复命令入口
- `AchievementTracker.java` - 命令注册
- `HeroSigilData.java` - 复用已有方法

**关键修改点**:

#### 步骤 1: 创建 DataRecovery 类
**位置**: `src/main/java/com/hero/sigil/data/DataRecovery.java`

**功能**:
- 封装 `HeroSigilData` 中的备份/恢复方法
- 添加恢复历史记录
- 添加恢复结果反馈（成功/失败/已恢复字段）

**代码结构**:
```java
package com.hero.sigil.data;

public class DataRecovery {
    // 封装已有功能，提供统一的恢复接口
    // 添加恢复历史记录
    // 添加恢复结果反馈
}
```

#### 步骤 2: 添加恢复命令
**位置**: `AchievementTracker.java` 的 `onRegisterCommands()` 方法

**命令格式**:
- `/herosigil recover` - 恢复当前玩家数据
- `/herosigil recover <player>` - 恢复指定玩家数据（管理员）
- `/herosigil backup` - 手动创建备份
- `/herosigil history` - 查看恢复历史

**功能**:
- 执行数据恢复
- 显示恢复结果（成功/失败/已恢复 X 个 buff 槽位）
- 显示备份信息

#### 步骤 3: 添加自动恢复机制
**位置**: `HeroSigilData.onLoad()` 方法（已有，需优化）

**功能**:
- 检测数据版本不匹配时自动迁移
- 迁移失败时创建备份
- 提示玩家手动恢复

**验收标准**:
- [ ] DataRecovery 类已创建
- [ ] 恢复命令正常工作
- [ ] 备份命令正常工作
- [ ] 历史记录功能正常
- [ ] 自动恢复机制正常

## 🔧 执行步骤

### 步骤 1: 阅读任务文档
仔细阅读 `task-1.10-data-recovery.md`

### 步骤 2: 阅读当前代码
阅读涉及的源文件，理解现有代码结构：
- [HeroSigilData.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/buffs/HeroSigilData.java) - 已有备份/恢复方法
- [AchievementTracker.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/achievement/AchievementTracker.java) - 命令注册入口
- [HeroSigilItem.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/item/HeroSigilItem.java) - 命令入口

### 步骤 3: 按顺序实现
1. 先实现 `DataRecovery` 类（封装已有功能 + 历史记录）
2. 再实现恢复命令（注册到 `onRegisterCommands()`）
3. 最后优化自动恢复机制

### 步骤 4: 验证与测试
- [ ] 编译检查通过
- [ ] `/herosigil recover` 命令正常
- [ ] `/herosigil backup` 命令正常
- [ ] `/herosigil history` 命令正常
- [ ] 自动恢复机制正常

## ⚠️ 注意事项
1. 复用 `HeroSigilData` 中已有的 `validateData()`、`createBackup()`、`restoreFromBackup()` 方法
2. 不要重复实现已有功能
3. 恢复命令使用 Brigadier API
4. 添加充分的日志记录（INFO/DEBUG）
5. 确保数据恢复的原子性
6. 遵循内存优化原则，避免不必要的数据复制
