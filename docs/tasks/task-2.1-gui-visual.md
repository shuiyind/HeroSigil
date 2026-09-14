# 任务 2.1: GUI 视觉优化

## 任务目标
优化 GUI 视觉效果，改进 buff 图标显示、槽位状态视觉、tooltip 信息展示。

## 涉及文件
- [BuffSlotWidget.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/gui/widget/BuffSlotWidget.java) - 主要修改
- [HeroSigilScreen.java](file:///c:/Users/shuiy/Desktop/CodeSYD/HeroSigil/src/main/java/com/hero/sigil/gui/screen/HeroSigilScreen.java) - 辅助修改

## 当前代码分析

### BuffSlotWidget.java 问题分析

**问题 1: Buff 图标不够直观 (第 155-192 行)**
```java
// 当前使用圆形 + 首字母显示
guiGraphics.fillCircle(centerX, centerY, radius + 1, 0xFF4682B4);
guiGraphics.fillCircle(centerX, centerY, radius - 1, fillColor);
guiGraphics.drawString(minecraft.font, firstLetter, ...);
```

**问题 2: 槽位状态视觉区分度不够 (第 70-113 行)**
- 锁定状态：深灰色背景 + X 标记
- 解锁状态：灰色背景 + 顶部边框
- 激活状态：金色高亮 + 内部发光

**问题 3: Tooltip 信息不完整 (第 204-293 行)**
- 缺少 buff 效果持续时间倒计时
- 缺少成就完成进度信息
- 中文本地化支持不完善

## 实现步骤

### 步骤 1: 优化 Buff 图标显示
**修改位置**: BuffSlotWidget.java 第 155-192 行 (drawBuffIcon 方法)

**当前代码**:
```java
private void drawBuffIcon(GuiGraphics guiGraphics, Minecraft minecraft) {
    java.util.List<com.hero.sigil.buffs.BuffEffect> buffs = 
        com.hero.sigil.buffs.HeroSigilData.getAllBuffs();
    
    if (this.slotId >= 0 && this.slotId < Math.min(buffs.size(), 3)) {
        com.hero.sigil.buffs.BuffEffect buff = buffs.get(this.slotId);
        
        int centerX = this.getX() + this.getWidth() / 2;
        int centerY = this.getY() + this.getHeight() / 2;
        int radius = Math.min(this.getWidth(), this.getHeight()) / 3 - 2;
        
        guiGraphics.fillCircle(centerX, centerY, radius + 1, 0xFF4682B4);
        int fillColor = getBuffColor(this.slotId);
        guiGraphics.fillCircle(centerX, centerY, radius - 1, fillColor);
        
        String firstLetter = "";
        if (!minecraft.level().isClientSide()) {
            MobEffect effect = buff.getMobEffect();
            if (effect != null && effect.getDescriptionId() != null && effect.getDescriptionId().length() > 4) {
                firstLetter = effect.getDescriptionId().substring(0, 1).toUpperCase();
            }
        }
        
        guiGraphics.drawString(minecraft.font, 
            firstLetter.isEmpty() ? "?" : firstLetter, 
            centerX - minecraft.font.width(firstLetter) / 2, 
            centerY - minecraft.font.height("") / 2 + 1, 
            0xFFFFFFFF, false);
    }
}
```

**修改为**:
```java
/**
 * 绘制 buff 图标（使用更直观的视觉效果）
 */
private void drawBuffIcon(GuiGraphics guiGraphics, Minecraft minecraft) {
    java.util.List<com.hero.sigil.buffs.BuffEffect> buffs = 
        com.hero.sigil.buffs.HeroSigilData.getAllBuffs();
    
    if (this.slotId >= 0 && this.slotId < Math.min(buffs.size(), 3)) {
        com.hero.sigil.buffs.BuffEffect buff = buffs.get(this.slotId);
        
        int centerX = this.getX() + this.getWidth() / 2;
        int centerY = this.getY() + this.getHeight() / 2;
        int radius = Math.min(this.getWidth(), this.getHeight()) / 3 - 2;
        
        // 绘制外环（金色边框表示激活）
        guiGraphics.fillCircle(centerX, centerY, radius + 1, 0xFFD4AF37); // 金色
        
        // 绘制内圆（根据 buff 类型着色）
        int fillColor = getBuffColor(this.slotId);
        guiGraphics.fillCircle(centerX, centerY, radius - 1, fillColor);
        
        // 添加脉冲动画效果（使用 tickCount 实现闪烁）
        long pulsePhase = minecraft.level().getGameTime();
        int alpha = (int) (128 + 127 * Math.sin(pulsePhase * 0.1));
        guiGraphics.fillCircle(centerX, centerY, radius - 3, 0x00FFFFFF | (alpha << 24));
        
        // 绘制 buff 类型图标（使用不同的形状表示不同类型的 buff）
        drawBuffTypeIcon(guiGraphics, minecraft, buff, centerX, centerY, radius);
    }
}

/**
 * 绘制 buff 类型图标
 */
private void drawBuffTypeIcon(GuiGraphics guiGraphics, Minecraft minecraft, 
                              com.hero.sigil.buffs.BuffEffect buff, int centerX, int centerY, int radius) {
    MobEffect effect = buff.getMobEffect();
    if (effect == null) return;
    
    // 根据 buff 类型绘制不同图标
    if (effect == net.minecraft.world.effect.MobEffects.HEALTH_BOOST) {
        // 生命提升：绘制心形
        drawHeartIcon(guiGraphics, centerX, centerY, radius);
    } else if (effect == net.minecraft.world.effect.MobEffects.SPEED) {
        // 加速：绘制闪电图标
        drawLightningIcon(guiGraphics, centerX, centerY, radius);
    } else if (effect == net.minecraft.world.effect.MobEffects.SATURATION 
               || effect == net.minecraft.world.effect.MobEffects.REGENERATION) {
        // 饱和/再生：绘制盾牌图标
        drawShieldIcon(guiGraphics, centerX, centerY, radius);
    } else {
        // 默认：显示 buff 名称首字母
        String name = effect.getDescriptionId();
        if (name != null && name.length() > 4) {
            String firstLetter = name.substring(0, 1).toUpperCase();
            guiGraphics.drawString(minecraft.font, firstLetter, 
                centerX - minecraft.font.width(firstLetter) / 2, 
                centerY - minecraft.font.height("") / 2, 
                0xFFFFFFFF, false);
        }
    }
}

/**
 * 绘制心形图标（生命提升）
 */
private void drawHeartIcon(GuiGraphics guiGraphics, int centerX, int centerY, int radius) {
    // 简化的心形绘制（使用两个圆形 + 一个矩形）
    int halfRadius = radius / 2;
    int yOffset = halfRadius / 3;
    
    // 左半圆
    guiGraphics.fillCircle(centerX - halfRadius / 2, centerY - yOffset, halfRadius / 2, 0xFFFF69B4);
    // 右半圆
    guiGraphics.fillCircle(centerX + halfRadius / 2, centerY - yOffset, halfRadius / 2, 0xFFFF69B4);
    // 三角形底部
    guiGraphics.fillTriangle(centerX - halfRadius, centerY, 
        centerX + halfRadius, centerY, 
        centerX, centerY + halfRadius * 2, 0xFFFF69B4);
}

/**
 * 绘制闪电图标（加速）
 */
private void drawLightningIcon(GuiGraphics guiGraphics, int centerX, int centerY, int radius) {
    int size = radius / 2;
    // 简化的闪电图形
    guiGraphics.fillTriangle(centerX - size / 2, centerY - size, 
        centerX + size / 2, centerY - size / 2, 
        centerX - size / 4, centerY, 
        0xFFFFFF00);
    guiGraphics.fillTriangle(centerX - size / 4, centerY, 
        centerX + size / 2, centerY + size, 
        centerX + size / 4, centerY + size / 2, 
        0xFFFFFF00);
}

/**
 * 绘制盾牌图标（饱和/再生）
 */
private void drawShieldIcon(GuiGraphics guiGraphics, int centerX, int centerY, int radius) {
    int size = radius / 2;
    // 简化的盾牌图形（使用矩形和三角形组合）
    guiGraphics.fillRectangle(centerX - size, centerY - size, size * 2, size, 0xFF32CD32);
    guiGraphics.fillTriangle(centerX - size, centerY - size, 
        centerX + size, centerY - size, 
        centerX, centerY + size, 0xFF32CD32);
}
```

### 步骤 2: 优化槽位状态视觉效果
**修改位置**: BuffSlotWidget.java 第 70-113 行 (renderWidget 方法)

**改进视觉区分度**:

```java
@Override
public void renderWidget(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
    Minecraft minecraft = Minecraft.getInstance();
    
    // 绘制背景基于状态（增强视觉区分）
    if (this.unlocked) {
        if (this.active) {
            // 激活状态 - 金色边框 + 脉冲动画
            int pulseAlpha = (int) (80 + 40 * Math.sin(minecraft.level().getGameTime() * 0.15));
            guiGraphics.fill(this.getX(), this.getY(), 
                this.getX() + this.getWidth(), this.getY() + this.getHeight(), 
                0x80FFD700); // 金色高亮
            
            // 内发光效果
            guiGraphics.fill(this.getX() + 2, this.getY() + 2, 
                this.getX() + this.getWidth() - 2, this.getY() + this.getHeight() - 2, 
                0x40FFD700);
            
            // 动态边框（脉冲效果）
            int borderColor = 0xFFD4AF37 | (pulseAlpha << 24);
            drawAnimatedBorder(guiGraphics, borderColor, minecraft.level().getGameTime());
        } else {
            // 解锁但未激活 - 绿色边框 + 稳定高亮
            guiGraphics.fill(this.getX(), this.getY(), 
                this.getX() + this.getWidth(), this.getY() + this.getHeight(), 
                0xFF2E8B57); // 中海绿色
            
            // 顶部边框指示可点击
            guiGraphics.strokeLine(this.getX(), this.getY(), 
                this.getX() + this.getWidth(), this.getY(), 
                0x8090EE90);
        }
    } else {
        // 锁定状态 - 深灰背景 + X 标记 + 暗淡效果
        guiGraphics.fill(this.getX(), this.getY(), 
            this.getX() + this.getWidth(), this.getY() + this.getHeight(), 
            0xFF2A2A2A);
        
        // 添加对角线 X 标记
        int centerX = this.getX() + this.getWidth() / 2;
        int centerY = this.getY() + this.getHeight() / 2;
        int size = Math.min(this.getWidth(), this.getHeight()) / 3;
        
        guiGraphics.strokeLine(centerX - size, centerY - size, 
            centerX + size, centerY + size, 0xFF888888);
        guiGraphics.strokeLine(centerX + size, centerY - size, 
            centerX - size, centerY + size, 0xFF888888);
    }

    // 绘制边框
    int borderColor = this.unlocked ? 0xFFFFFFFF : 0xFF444444;
    drawSimpleBorder(guiGraphics, borderColor);

    // 绘制 buff 图标或锁定指示器
    if (this.active && this.unlocked) {
        drawBuffIcon(guiGraphics, minecraft);
    } else if (!this.unlocked) {
        // 显示锁定指示器（更明显的锁图标）
        drawLockIcon(guiGraphics, minecraft);
    }

    // 悬停时显示 tooltip
    if (this.isHovered()) {
        net.minecraft.client.gui.components.Tooltip tooltip = getTooltip();
        if (tooltip != null) {
            guiGraphics.renderTooltip(
                minecraft.font, 
                this.getTooltip().getVisualOrder(), 
                pMouseX - this.getX(), 
                pMouseY - this.getY()
            );
        }
    }
}

/**
 * 绘制动画边框（脉冲效果）
 */
private void drawAnimatedBorder(GuiGraphics guiGraphics, int color, long gameTime) {
    int pulseOffset = (int) (Math.sin(gameTime * 0.1) * 2);
    int x = this.getX() - pulseOffset;
    int y = this.getY() - pulseOffset;
    int width = this.getWidth() + pulseOffset * 2;
    int height = this.getHeight() + pulseOffset * 2;
    
    guiGraphics.strokeLine(x, y, x + width, y, color);
    guiGraphics.strokeLine(x, y + height, x + width, y + height, color);
    guiGraphics.strokeLine(x, y, x, y + height, color);
    guiGraphics.strokeLine(x + width, y, x + width, y + height, color);
}

/**
 * 绘制简单边框
 */
private void drawSimpleBorder(GuiGraphics guiGraphics, int color) {
    guiGraphics.strokeLine(this.getX(), this.getY(), 
        this.getX() + this.getWidth(), this.getY(), color);
    guiGraphics.strokeLine(this.getX(), this.getY() + this.getHeight(), 
        this.getX() + this.getWidth(), this.getY() + this.getHeight(), color);
    guiGraphics.strokeLine(this.getX(), this.getY(), 
        this.getX(), this.getY() + this.getHeight(), color);
    guiGraphics.strokeLine(this.getX() + this.getWidth(), this.getY(), 
        this.getX() + this.getWidth(), this.getY() + this.getHeight(), color);
}

/**
 * 绘制锁图标（更明显）
 */
private void drawLockIcon(GuiGraphics guiGraphics, Minecraft minecraft) {
    int centerX = this.getX() + this.getWidth() / 2;
    int centerY = this.getY() + this.getHeight() / 2;
    
    // 绘制简化的锁形状
    int lockWidth = 8;
    int lockHeight = 6;
    
    // 锁体
    guiGraphics.fillRectangle(centerX - lockWidth / 2, centerY - lockHeight / 2 + 2, 
        lockWidth, lockHeight - 2, 0xFF888888);
    // 锁扣
    guiGraphics.strokeLine(centerX - 3, centerY - lockHeight / 2 + 2, 
        centerX + 3, centerY - lockHeight / 2 + 2, 0xFF888888);
    
    // 或者使用文本图标
    guiGraphics.drawString(minecraft.font, "🔒", 
        centerX - 4, centerY - 6, 0xFF888888, false);
}
```

### 步骤 3: 改进 Tooltip 信息展示
**修改位置**: BuffSlotWidget.java 第 244-293 行 (getTooltipLines 方法)

**当前代码**:
```java
private java.util.List<net.minecraft.network.chat.Component> getTooltipLines() {
    java.util.ArrayList<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
    
    if (!this.unlocked) {
        // 显示成就要求
        com.hero.sigil.achievement.AchievementTracker.AchievementType[] achievements = 
            com.hero.sigil.achievement.AchievementTracker.AchievementType.values();
        
        if (this.slotId < achievements.length) {
            com.hero.sigil.achievement.AchievementType achievement = achievements[this.slotId];
            lines.add(Component.translatable("tooltip.herosigil.requirement", 
                Component.translatable(achievement.name().toLowerCase())));
        }
    } else if (!this.active) {
        lines.add(Component.translatable("tooltip.herosigil.click_to_activate"));
        // ...
    }
    // ...
}
```

**修改为**:
```java
/**
 * 获取 tooltip 信息行（增强信息展示）
 */
private java.util.List<net.minecraft.network.chat.Component> getTooltipLines() {
    java.util.ArrayList<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
    
    java.util.List<com.hero.sigil.buffs.BuffEffect> buffs = 
        com.hero.sigil.buffs.HeroSigilData.getAllBuffs();
    
    if (this.slotId >= 0 && this.slotId < Math.min(buffs.size(), 3)) {
        com.hero.sigil.buffs.BuffEffect buff = buffs.get(this.slotId);
        net.minecraft.world.effect.MobEffect effect = buff.getMobEffect();
        
        // 显示 buff 名称
        if (effect != null) {
            lines.add(Component.translatable(effect.getDescriptionId()).withStyle(net.minecraft.ChatFormatting.BOLD));
        }
        
        // 根据状态显示不同信息
        if (!this.unlocked) {
            // 未解锁：显示成就要求
            lines.add(Component.empty());
            lines.add(Component.translatable("tooltip.herosigil.requirement", 
                Component.translatable("achievement.herosigil." + getAchievementKey(this.slotId)).withStyle(net.minecraft.ChatFormatting.GOLD)));
            
            // 显示完成进度（如果有）
            showAchievementProgress(lines, this.slotId);
            
        } else if (!this.active) {
            // 已解锁但未激活：显示激活提示和 buff 信息
            lines.add(Component.empty());
            lines.add(Component.translatable("tooltip.herosigil.click_to_activate").withStyle(net.minecraft.ChatFormatting.GREEN));
            
            // 显示 buff 持续时间
            if (!buff.isPermanent()) {
                lines.add(Component.translatable("tooltip.herosigil.duration_info", 
                    buff.getDurationSeconds()).withStyle(net.minecraft.ChatFormatting.AQUA));
            } else {
                lines.add(Component.translatable("tooltip.herosigil.permanent_info").withStyle(net.minecraft.ChatFormatting.GOLD));
            }
            
            // 显示 buff 放大器
            if (buff.getAmplifier() > 0) {
                lines.add(Component.translatable("tooltip.herosigil.amplifier", buff.getAmplifier()).withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
            }
            
        } else {
            // 已激活：显示当前状态和剩余时间
            lines.add(Component.empty());
            lines.add(Component.translatable("tooltip.herosigil.active_status", 
                Component.literal("✓").withStyle(net.minecraft.ChatFormatting.GREEN)));
            
            if (!buff.isPermanent()) {
                // 显示剩余时间（如果可获取）
                lines.add(Component.translatable("tooltip.herosigil.remaining_time", "??").withStyle(net.minecraft.ChatFormatting.YELLOW));
                lines.add(Component.translatable("tooltip.herosigil.refresh_interval", 
                    (int) (buff.getDurationSeconds() * 0.8)).withStyle(net.minecraft.ChatFormatting.GRAY));
            } else {
                lines.add(Component.translatable("tooltip.herosigil.permanent").withStyle(net.minecraft.ChatFormatting.GOLD));
            }
            
            // 显示停用提示
            lines.add(Component.translatable("tooltip.herosigil.click_to_deactivate").withStyle(net.minecraft.ChatFormatting.RED));
        }
    }
    
    return lines;
}

/**
 * 获取成就 key（用于本地化）
 */
private String getAchievementKey(int slotId) {
    switch (slotId) {
        case 0: return "defeat_boss";
        case 1: return "explore_all_biomes";
        case 2: return "defeat_ender_dragon";
        default: return "unknown";
    }
}

/**
 * 显示成就完成进度
 */
private void showAchievementProgress(java.util.List<net.minecraft.network.chat.Component> lines, int slotId) {
    switch (slotId) {
        case 0: // 击败 Boss
            lines.add(Component.translatable("tooltip.herosigil.progress", 
                com.hero.sigil.achievement.AchievementTracker.getBossKillCount(), 
                1).withStyle(net.minecraft.ChatFormatting.GRAY));
            break;
        case 1: // 探索世界
            if (Minecraft.getInstance().player != null) {
                int visitedBiomes = com.hero.sigil.achievement.AchievementTracker.getBiomeVisitCount(
                    Minecraft.getInstance().player);
                lines.add(Component.translatable("tooltip.herosigil.progress", 
                    visitedBiomes, 20).withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            break;
        case 2: // 击败末影龙
            lines.add(Component.translatable("tooltip.herosigil.progress", 
                com.hero.sigil.achievement.AchievementTracker.isAchievementUnlocked(
                    Minecraft.getInstance().player, 
                    com.hero.sigil.achievement.AchievementTracker.AchievementType.DEFEAT_ENDER_DRAGON) ? 1 : 0, 
                1).withStyle(net.minecraft.ChatFormatting.GRAY));
            break;
    }
}
```

### 步骤 4: 添加本地化支持
**修改位置**: 更新 zh_cn.json 和 en_us.json

**在 zh_cn.json 中添加**:
```json
{
  "tooltip.herosigil.requirement": "需要完成成就: %s",
  "tooltip.herosigil.click_to_activate": "点击激活",
  "tooltip.herosigil.click_to_deactivate": "点击停用",
  "tooltip.herosigil.duration_info": "持续时间: %s 秒",
  "tooltip.herosigil.permanent_info": "永久效果",
  "tooltip.herosigil.amplifier": "放大器等级: %s",
  "tooltip.herosigil.active_status": "激活状态: %s",
  "tooltip.herosigil.remaining_time": "剩余时间: %s",
  "tooltip.herosigil.refresh_interval": "刷新间隔: %s 秒",
  "tooltip.herosigil.permanent": "永久生效",
  "tooltip.herosigil.progress": "进度: %s/%s",
  "achievement.herosigil.defeat_boss": "击败 Boss",
  "achievement.herosigil.explore_all_biomes": "探索世界",
  "achievement.herosigil.defeat_ender_dragon": "击败末影龙"
}
```

**在 en_us.json 中添加**:
```json
{
  "tooltip.herosigil.requirement": "Requires achievement: %s",
  "tooltip.herosigil.click_to_activate": "Click to activate",
  "tooltip.herosigil.click_to_deactivate": "Click to deactivate",
  "tooltip.herosigil.duration_info": "Duration: %s seconds",
  "tooltip.herosigil.permanent_info": "Permanent effect",
  "tooltip.herosigil.amplifier": "Amplifier level: %s",
  "tooltip.herosigil.active_status": "Active status: %s",
  "tooltip.herosigil.remaining_time": "Remaining time: %s",
  "tooltip.herosigil.refresh_interval": "Refresh interval: %s seconds",
  "tooltip.herosigil.permanent": "Permanent",
  "tooltip.herosigil.progress": "Progress: %s/%s",
  "achievement.herosigil.defeat_boss": "Defeat Boss",
  "achievement.herosigil.explore_all_biomes": "Explore World",
  "achievement.herosigil.defeat_ender_dragon": "Defeat Ender Dragon"
}
```

## 验收标准
- [ ] Buff 图标使用类型化图标（心形、闪电、盾牌）
- [ ] 激活状态有脉冲动画效果
- [ ] 锁定状态有明确的锁图标
- [ ] Tooltip 显示 buff 名称、持续时间、进度信息
- [ ] 中英文本地化完整
- [ ] 视觉效果区分度高

## 注意事项
1. 使用 `minecraft.level().getGameTime()` 实现动画
2. 图标绘制使用简化的几何图形
3. Tooltip 信息分层显示（名称 → 状态 → 详细信息）

## 测试建议
1. 打开 GUI，检查 buff 图标是否正确显示
2. 切换 buff 状态，观察视觉效果变化
3. 悬停在槽位上，检查 tooltip 信息
4. 验证中英文本地化
