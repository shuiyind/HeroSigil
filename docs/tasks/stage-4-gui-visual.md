# 阶段四：GUI 视觉优化 - 工作提示词

## 📌 任务概述
优化 GUI 视觉效果，改进 buff 图标显示、槽位状态视觉、tooltip 信息展示。

## 🎯 子代理需要处理的任务

### 任务 2.1: GUI 视觉优化
**涉及文件**:
- `BuffSlotWidget.java` - 主要修改（图标、状态、tooltip）
- `HeroSigilScreen.java` - 辅助修改
- 本地化文件 `zh_cn.json` / `en_us.json`

**关键修改点**:

#### 步骤 1: 优化 Buff 图标显示
- 修改 `drawBuffIcon()` 方法
- 添加金色外环（激活状态）
- 添加脉冲动画效果（使用 `getGameTime()`）
- 实现类型化图标（心形/闪电/盾牌）
- 添加 `drawHeartIcon()`、`drawLightningIcon()`、`drawShieldIcon()` 方法

#### 步骤 2: 优化槽位状态视觉效果
- 修改 `renderWidget()` 方法
- 激活状态：金色边框 + 脉冲动画
- 解锁未激活：绿色边框 + 顶部边框
- 锁定状态：深灰背景 + X 标记/锁图标
- 添加 `drawAnimatedBorder()`、`drawSimpleBorder()`、`drawLockIcon()` 方法

#### 步骤 3: 改进 Tooltip 信息展示
- 修改 `getTooltipLines()` 方法
- 分层显示（名称 → 状态 → 详细信息）
- 显示 buff 名称、持续时间、放大器、剩余时间
- 显示成就完成进度
- 添加 `getAchievementKey()`、`showAchievementProgress()` 方法

#### 步骤 4: 添加本地化支持
- 在 `zh_cn.json` 中添加中文翻译
- 在 `en_us.json` 中添加英文翻译
- 包含所有 tooltip 和成就名称

**验收标准**:
- [ ] Buff 图标使用类型化图标（心形、闪电、盾牌）
- [ ] 激活状态有脉冲动画效果
- [ ] 锁定状态有明确的锁图标
- [ ] Tooltip 显示 buff 名称、持续时间、进度信息
- [ ] 中英文本地化完整
- [ ] 视觉效果区分度高

## 🔧 执行步骤

### 步骤 1: 阅读任务文档
仔细阅读 `task-2.1-gui-visual.md`，特别是"修改为"部分的代码示例

### 步骤 2: 阅读当前代码
阅读涉及的源文件，理解现有代码结构：
- [BuffSlotWidget.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/gui/widget/BuffSlotWidget.java)
- [HeroSigilScreen.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/gui/screen/HeroSigilScreen.java)

### 步骤 3: 按顺序实现
1. 先实现步骤 1（Buff 图标）
2. 再实现步骤 2（槽位状态）
3. 然后实现步骤 3（Tooltip）
4. 最后实现步骤 4（本地化）

### 步骤 4: 验证与测试
- [ ] 编译检查通过
- [ ] 打开 GUI，检查 buff 图标是否正确显示
- [ ] 切换 buff 状态，观察视觉效果变化
- [ ] 悬停在槽位上，检查 tooltip 信息
- [ ] 验证中英文本地化

## ⚠️ 注意事项
1. 使用 `minecraft.level().getGameTime()` 实现动画
2. 图标绘制使用简化的几何图形（fillCircle、fillTriangle、fillRectangle、strokeLine）
3. Tooltip 信息分层显示（名称 → 状态 → 详细信息）
4. 脉冲动画使用 `Math.sin()` 实现
5. 保持代码简洁，避免过度设计
6. 本地化字符串使用 `%s` 占位符
