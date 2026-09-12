# 阶段二：游戏机制扩展 - 工作提示词

## 📌 任务概述
实现 Boss 识别、生物群系追踪、成就通知三大游戏机制，增强玩家交互体验。

## 🎯 子代理需要处理的任务

### 任务 1.5: Boss 识别系统
**涉及文件**:
- `AchievementTracker.java` - 添加 Boss 击杀统计
- `BossIdentification.java` - Boss 识别逻辑
- `HeroSigilData.java` - 数据同步

**关键修改点**:
1. 添加 Boss 实体类型列表（凋灵、末影龙、灾厄巡守者等）
2. 实现 Boss 击杀事件监听
3. 添加 Boss 标识显示（血条增强/名称高亮）
4. 实现 Boss 识别进度追踪

**验收标准**:
- [ ] 正确识别 Boss 类型
- [ ] 击杀 Boss 后更新进度
- [ ] Boss 标识显示正确

### 任务 1.6: 生物群系追踪系统
**涉及文件**:
- `BiomeTracker.java` - 群系追踪逻辑
- `AchievementTracker.java` - 群系访问统计
- `HeroSigilData.java` - 数据同步

**关键修改点**:
1. 添加生物群系访问记录（使用 Set 存储已访问群系 ID）
2. 实现玩家移动时的群系检测
3. 添加群系访问进度显示
4. 实现 20 种群系解锁的判定

**验收标准**:
- [ ] 正确记录玩家访问的群系
- [ ] 进度统计准确
- [ ] 解锁条件正确判定

### 任务 1.7: 成就通知系统
**涉及文件**:
- `AchievementNotifier.java` - 通知逻辑
- `AchievementTracker.java` - 成就状态查询
- 本地化文件 `zh_cn.json` / `en_us.json`

**关键修改点**:
1. 实现成就解锁通知（系统消息 + 音效）
2. 添加通知队列管理（避免消息堆积）
3. 实现通知冷却（30 秒内不重复通知同一成就）
4. 添加中英文本地化支持

**验收标准**:
- [ ] 成就解锁时正确发送通知
- [ ] 通知包含成就名称和描述
- [ ] 通知冷却机制工作正常
- [ ] 中英文本地化完整

## 🔧 执行步骤

### 步骤 1: 阅读任务文档
仔细阅读 `task-1.5-boss-identification.md`、`task-1.6-biome-tracking.md`、`task-1.7-achievement-notifications.md`

### 步骤 2: 阅读当前代码
阅读涉及的源文件，理解现有代码结构：
- [AchievementTracker.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/achievement/AchievementTracker.java)
- 其他相关新文件

### 步骤 3: 按顺序实现
1. 先实现任务 1.5（Boss 识别）
2. 再实现任务 1.6（群系追踪）
3. 最后实现任务 1.7（成就通知）

### 步骤 4: 验证与测试
- [ ] 编译检查通过
- [ ] Boss 识别逻辑正确
- [ ] 群系追踪准确
- [ ] 成就通知正常

## ⚠️ 注意事项
1. Boss 列表应包含常见 Boss 类型
2. 群系追踪应使用服务端权威数据
3. 通知系统应避免消息刷屏
4. 添加适当的日志记录
5. 确保中英文本地化完整
6. 遵循小步重构原则，每完成一个任务就验证
