package com.hero.sigil.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * Widget for displaying a buff slot in the Hero Sigil GUI.
 */
public class BuffSlotWidget extends AbstractButton {

    private final int slotId;
    private boolean unlocked = false;
    private boolean active = false;
    private net.minecraft.client.gui.components.Tooltip tooltip;

    public BuffSlotWidget(int pSlotId, int pX, int pY, int pWidth, int pHeight) {
        super(pX, pY, pWidth, pHeight, Component.empty());
        this.slotId = pSlotId;
    }

    @Override
    public void onClick(double pMouseX, double pMouseY) {
        if (this.unlocked && !this.active) {
            // Slot is unlocked but not active - activate it

            Minecraft.getInstance().getSoundSource().play(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK_PRESS,
                net.minecraft.client.resources.sounds.SimpleSoundInstance.defaultVolume()
            );

            this.setActive(true);

            // Send packet to server to toggle buff slot
            if (Minecraft.getInstance().player != null) {
                com.hero.sigil.network.CPacketToggleBuffSlot packet =
                    new com.hero.sigil.network.CPacketToggleBuffSlot(
                        Minecraft.getInstance().player.getId(),
                        this.slotId
                    );
                net.neoforged.neoforge.network.NetworkHooks.sendToServer(packet);
            }
        } else if (this.unlocked && this.active) {
            // Slot is active - deactivate it

            Minecraft.getInstance().getSoundSource().play(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK_RELEASE,
                net.minecraft.client.resources.sounds.SimpleSoundInstance.defaultVolume()
            );

            this.setActive(false);

            // Send packet to server to toggle buff slot
            if (Minecraft.getInstance().player != null) {
                com.hero.sigil.network.CPacketToggleBuffSlot packet =
                    new com.hero.sigil.network.CPacketToggleBuffSlot(
                        Minecraft.getInstance().player.getId(),
                        this.slotId
                    );
                net.neoforged.neoforge.network.NetworkHooks.sendToServer(packet);
            }
        }
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        Minecraft minecraft = Minecraft.getInstance();

        // Draw background based on state
        if (this.unlocked) {
            if (this.active) {
                // Active state - highlight with gold color
                guiGraphics.fill(this.getX(), this.getY(),
                    this.getX() + this.getWidth(), this.getY() + this.getHeight(),
                    0x80FFD700); // Gold highlight for active slots

                // Draw inner glow effect
                guiGraphics.fill(this.getX() + 2, this.getY() + 2,
                    this.getX() + this.getWidth() - 2, this.getY() + this.getHeight() - 2,
                    0x40FFD700); // Lighter gold inner glow

                // 绘制脉冲动画边框
                drawAnimatedBorder(guiGraphics, minecraft);
            } else {
                // Unlocked but inactive - green background
                guiGraphics.fill(this.getX(), this.getY(),
                    this.getX() + this.getWidth(), this.getY() + this.getHeight(),
                    0xFF2E8B57); // 中海绿色 - 解锁但未激活状态
            }
        } else {
            // Locked slot - darker appearance with lock icon
            guiGraphics.fill(this.getX(), this.getY(),
                this.getX() + this.getWidth(), this.getY() + this.getHeight(),
                0xFF2A2A2A); // Dark gray for locked slots

            // 绘制矢量锁图标 (替代 emoji)
            float centerX = this.getX() + this.getWidth() / 2.0f;
            float centerY = this.getY() + this.getHeight() / 2.0f;
            float lockSize = Math.min(this.getWidth(), this.getHeight()) / 3.0f;
            drawLockIcon(guiGraphics, centerX, centerY, lockSize);
        }

        // 绘制边框
        drawSimpleBorder(guiGraphics, this.unlocked);

        // Draw buff icon if active and unlocked
        if (this.active && this.unlocked) {
            drawBuffIcon(guiGraphics, minecraft);
        }

        // Draw tooltip on hover
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

    private void drawBuffIcon(GuiGraphics guiGraphics, Minecraft minecraft) {
        // 获取 Buff 类型
        java.util.List<com.hero.sigil.buffs.BuffEffect> buffs =
            com.hero.sigil.buffs.HeroSigilData.getAllBuffs();

        if (this.slotId >= 0 && this.slotId < Math.min(buffs.size(), 3)) {
            com.hero.sigil.buffs.BuffEffect buff = buffs.get(this.slotId);

            // 获取中心点坐标和半径
            float centerX = this.getX() + this.getWidth() / 2;
            float centerY = this.getY() + this.getHeight() / 2;
            float radius = Math.min(this.getWidth(), this.getHeight()) / 3 - 2;

            // 脉冲动画效果 - 基于帧时间计算 alpha 值
            long gameTime = minecraft.getFrameTime();
            float pulseAlpha = 0.3f + 0.1f * (float) Math.sin(gameTime / 200.0);
            int goldColorWithAlpha = (int) (0xFFD4AF37 & 0x00FFFFFF) | (int) (0xFF * (0.5f + pulseAlpha * 0.5f)) << 24;

            // 绘制带脉冲效果的金色外环
            guiGraphics.fillCircle(centerX, centerY, radius + 1, goldColorWithAlpha);

            // 绘制内部填充色
            int fillColor = getBuffColor(this.slotId);
            guiGraphics.fillCircle(centerX, centerY, radius - 1, fillColor);

            // 根据 Buff 类型绘制专属图标
            com.hero.sigil.achievement.AchievementTracker.AchievementType achievement =
                com.hero.sigil.achievement.AchievementTracker.AchievementType.values()[this.slotId];
            drawBuffTypeIcon(guiGraphics, centerX, centerY, radius, achievement);
        }
    }

    /**
     * 根据 Achievement 类型绘制专属图标
     */
    private void drawBuffTypeIcon(GuiGraphics guiGraphics, float centerX, float centerY,
                                   float radius, com.hero.sigil.achievement.AchievementTracker.AchievementType type) {
        float size = radius * 0.6f;

        switch (type) {
            case HEALTH_BOOST:
                // 绘制心形 - 红色
                int heartColor = 0xFFE74C3C;
                guiGraphics.fillCircle(centerX - size * 0.3f, centerY - size * 0.2f, size * 0.4f, heartColor);
                guiGraphics.fillCircle(centerX + size * 0.3f, centerY - size * 0.2f, size * 0.4f, heartColor);
                guiGraphics.fillTriangle(centerX - size * 0.5f, centerY,
                                        centerX + size * 0.5f, centerY,
                                        centerX, centerY + size * 0.7f, heartColor);
                break;

            case SPEED_BOOST:
                // 绘制闪电 - 蓝色
                int lightningColor = 0xFF3498DB;
                guiGraphics.fillTriangle(centerX - size * 0.1f, centerY - size * 0.7f,
                                        centerX + size * 0.1f, centerY - size * 0.7f,
                                        centerX - size * 0.05f, centerY - size * 0.3f, lightningColor);
                guiGraphics.fillTriangle(centerX - size * 0.05f, centerY - size * 0.3f,
                                        centerX + size * 0.15f, centerY - size * 0.3f,
                                        centerX - size * 0.1f, centerY, lightningColor);
                guiGraphics.fillTriangle(centerX - size * 0.1f, centerY,
                                        centerX + size * 0.1f, centerY,
                                        centerX, centerY + size * 0.6f, lightningColor);
                break;

            case SATURATION:
            case REGENERATION:
                // 绘制盾牌 - 绿色
                int shieldColor = 0xFF2ECC71;
                guiGraphics.fillCircle(centerX, centerY - size * 0.2f, size * 0.5f, shieldColor);
                guiGraphics.fillTriangle(centerX - size * 0.5f, centerY - size * 0.2f,
                                        centerX + size * 0.5f, centerY - size * 0.2f,
                                        centerX, centerY + size * 0.6f, shieldColor);
                break;

            default:
                // 默认绘制星形 - 金色
                int starColor = 0xFFFFD700;
                guiGraphics.fillCircle(centerX, centerY, size * 0.4f, starColor);
        }
    }

    /**
     * 绘制心形图标 (生命提升 Buff)
     */
    private void drawHeartIcon(GuiGraphics guiGraphics, float centerX, float centerY, float radius) {
        float size = radius * 0.7f;
        int heartColor = 0xFFE74C3C; // 红色

        // 心形由两个圆形和一个三角形组成
        // 左上半圆
        guiGraphics.fillCircle(centerX - size * 0.3f, centerY - size * 0.3f, size * 0.5f, heartColor);
        // 右上半圆
        guiGraphics.fillCircle(centerX + size * 0.3f, centerY - size * 0.3f, size * 0.5f, heartColor);
        // 下半部分 (三角形)
        guiGraphics.fillTriangle(centerX - size * 0.5f, centerY - size * 0.1f,
                                centerX + size * 0.5f, centerY - size * 0.1f,
                                centerX, centerY + size * 0.6f, heartColor);
    }

    /**
     * 绘制闪电图标 (加速 Buff)
     */
    private void drawLightningIcon(GuiGraphics guiGraphics, float centerX, float centerY, float radius) {
        float size = radius * 0.8f;
        int lightningColor = 0xFF3498DB; // 蓝色

        // 闪电图标 - 由多个三角形拼接而成
        // 上部
        guiGraphics.fillTriangle(centerX - size * 0.1f, centerY - size * 0.7f,
                                centerX + size * 0.1f, centerY - size * 0.7f,
                                centerX, centerY - size * 0.3f, lightningColor);
        // 中部
        guiGraphics.fillTriangle(centerX - size * 0.15f, centerY - size * 0.2f,
                                centerX + size * 0.15f, centerY - size * 0.2f,
                                centerX, centerY + size * 0.1f, lightningColor);
        // 下部
        guiGraphics.fillTriangle(centerX - size * 0.2f, centerY + size * 0.15f,
                                centerX + size * 0.2f, centerY + size * 0.15f,
                                centerX, centerY + size * 0.5f, lightningColor);
    }

    /**
     * 绘制盾形图标 (饱和/再生 Buff)
     */
    private void drawShieldIcon(GuiGraphics guiGraphics, float centerX, float centerY, float radius) {
        float size = radius * 0.7f;
        int shieldColor = 0xFF2ECC71; // 绿色

        // 盾牌形状 - 由一个圆形和一个三角形组成
        // 上部圆形
        guiGraphics.fillCircle(centerX, centerY - size * 0.2f, size * 0.6f, shieldColor);
        // 下部三角形
        guiGraphics.fillTriangle(centerX - size * 0.6f, centerY - size * 0.2f,
                                centerX + size * 0.6f, centerY - size * 0.2f,
                                centerX, centerY + size * 0.6f, shieldColor);
    }

    /**
     * 绘制脉冲动画边框 (激活状态)
     */
    private void drawAnimatedBorder(GuiGraphics guiGraphics, Minecraft minecraft) {
        long gameTime = minecraft.getFrameTime();
        float pulseAlpha = 0.5f + 0.3f * (float) Math.sin(gameTime / 150.0);

        // 金色边框带脉冲 alpha 效果
        int borderColor = (int) (0xFFD700 & 0x00FFFFFF) | (int) (0xFF * pulseAlpha) << 24;

        // 绘制外层脉冲光晕 (在槽位外侧 1 像素)
        guiGraphics.strokeLine(this.getX() - 1, this.getY() - 1,
            this.getX() + this.getWidth() + 1, this.getY() - 1,
            borderColor);
        guiGraphics.strokeLine(this.getX() - 1, this.getY() + this.getHeight() + 1,
            this.getX() + this.getWidth() + 1, this.getY() + this.getHeight() + 1,
            borderColor);
        guiGraphics.strokeLine(this.getX() - 1, this.getY(),
            this.getX() - 1, this.getY() + this.getHeight(),
            borderColor);
        guiGraphics.strokeLine(this.getX() + this.getWidth() + 1, this.getY(),
            this.getX() + this.getWidth() + 1, this.getY() + this.getHeight(),
            borderColor);
    }

    /**
     * 绘制简单边框 (非激活状态)
     */
    private void drawSimpleBorder(GuiGraphics guiGraphics, boolean unlocked) {
        int borderColor = unlocked ? 0xFFFFFFFF : 0xFF444444;

        guiGraphics.strokeLine(this.getX(), this.getY(),
            this.getX() + this.getWidth(), this.getY(),
            borderColor);
        guiGraphics.strokeLine(this.getX(), this.getY() + this.getHeight(),
            this.getX() + this.getWidth(), this.getY() + this.getHeight(),
            borderColor);
        guiGraphics.strokeLine(this.getX(), this.getY(),
            this.getX(), this.getY() + this.getHeight(),
            borderColor);
        guiGraphics.strokeLine(this.getX() + this.getWidth(), this.getY(),
            this.getX() + this.getWidth(), this.getY() + this.getHeight(),
            borderColor);
    }

    /**
     * 绘制矢量锁图标 (替代 emoji)
     */
    private void drawLockIcon(GuiGraphics guiGraphics, float centerX, float centerY, float size) {
        int lockColor = 0xFF888888;

        // 锁体 (矩形)
        int lockWidth = (int) (size * 0.8f);
        int lockHeight = (int) (size * 0.6f);
        guiGraphics.fill(centerX - lockWidth / 2, centerY - lockHeight / 4,
                        centerX + lockWidth / 2, centerY + lockHeight / 4,
                        lockColor);

        // 锁环 (半圆形)
        int ringRadius = (int) (size * 0.3f);
        guiGraphics.fillCircle(centerX, centerY - lockHeight / 4, ringRadius, lockColor);
    }

    /**
     * 获取成就的本地化键名
     */
    private String getAchievementKey() {
        if (this.slotId >= 0 && this.slotId < com.hero.sigil.achievement.AchievementTracker.AchievementType.values().length) {
            com.hero.sigil.achievement.AchievementTracker.AchievementType achievement =
                com.hero.sigil.achievement.AchievementTracker.AchievementType.values()[this.slotId];
            return "achievement.herosigil." + achievement.name().toLowerCase();
        }
        return "";
    }

    /**
     * 显示成就完成进度
     */
    private java.util.List<net.minecraft.network.chat.Component> showAchievementProgress() {
        java.util.ArrayList<net.minecraft.network.chat.Component> progressLines = new java.util.ArrayList<>();

        if (this.slotId >= 0 && this.slotId < com.hero.sigil.achievement.AchievementTracker.AchievementType.values().length) {
            com.hero.sigil.achievement.AchievementTracker.AchievementType achievement =
                com.hero.sigil.achievement.AchievementTracker.AchievementType.values()[this.slotId];

            // 根据成就类型显示对应进度
            switch (achievement) {
                case DEFEAT_BOSS:
                    int bossKills = com.hero.sigil.achievement.AchievementTracker.getInstance().getBossKillCount();
                    progressLines.add(Component.translatable("tooltip.herosigil.progress",
                        bossKills, 10)); // 假设需要击杀 10 个 Boss
                    break;

                case EXPLORE_ALL_BIOMES:
                    int exploredBiomes = com.hero.sigil.achievement.AchievementTracker.getInstance().getExploredBiomeCount();
                    int totalBiomes = com.hero.sigil.achievement.AchievementTracker.getInstance().getTotalBiomeCount();
                    if (totalBiomes > 0) {
                        progressLines.add(Component.translatable("tooltip.herosigil.progress",
                            exploredBiomes, totalBiomes));
                    }
                    break;

                default:
                    // 其他成就不显示进度
                    break;
            }
        }

        return progressLines;
    }

    private int getBuffColor(int slotId) {
        // Different colors for different buff types
        switch (slotId) {
            case 0: return 0xFF3CB371; // Green - Saturation/Health Boost
            case 1: return 0xFF4682B4; // Blue - Speed
            case 2: return 0xFFFF6347; // Red - Strength
            default: return 0xFFFFFFFF;
        }
    }

    @Override
    public net.minecraft.client.gui.components.Tooltip getTooltip() {
        if (this.tooltip == null) {
            Component baseComponent;

            if (!this.unlocked) {
                baseComponent = Component.translatable("tooltip.herosigil.slot.locked", this.slotId + 1);
            } else if (!this.active) {
                baseComponent = Component.translatable("tooltip.herosigil.slot.unlocked", this.slotId + 1);
            } else {
                // Get active buff name from HeroSigilData
                java.util.List<com.hero.sigil.buffs.BuffEffect> buffs =
                    com.hero.sigil.buffs.HeroSigilData.getAllBuffs();

                String buffName = "";
                if (this.slotId >= 0 && this.slotId < Math.min(buffs.size(), 3)) {
                    com.hero.sigil.buffs.BuffEffect buff = buffs.get(this.slotId);
                    // Get the localized name from MobEffect's translation key
                    net.minecraft.world.effect.MobEffect effect = buff.getMobEffect();
                    if (effect != null) {
                        baseComponent = Component.translatable(effect.getDescriptionId()).withStyle(net.minecraft.network.chat.Style.EMPTY.withBold(true));
                    } else {
                        baseComponent = Component.translatable("tooltip.herosigil.slot.active",
                            this.slotId + 1, "Unknown Buff");
                    }
                } else {
                    baseComponent = Component.translatable("tooltip.herosigil.slot.active",
                        this.slotId + 1, "Unknown Buff");
                }
            }

            this.tooltip = net.minecraft.client.gui.components.Tooltip.create(
                baseComponent.getVisualOrder(),
                getTooltipLines()
            );
        }

        return this.tooltip;
    }

    private java.util.List<net.minecraft.network.chat.Component> getTooltipLines() {
        java.util.ArrayList<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();

        if (!this.unlocked) {
            // Show achievement requirement for unlocked slots
            com.hero.sigil.achievement.AchievementTracker.AchievementType[] achievements =
                com.hero.sigil.achievement.AchievementTracker.AchievementType.values();

            if (this.slotId < achievements.length) {
                com.hero.sigil.achievement.AchievementType achievement = achievements[this.slotId];
                lines.add(Component.translatable("tooltip.herosigil.requirement",
                    Component.translatable(getAchievementKey())));

                // 显示成就进度
                java.util.List<net.minecraft.network.chat.Component> progressLines = showAchievementProgress();
                lines.addAll(progressLines);
            } else {
                lines.add(Component.translatable("tooltip.herosigil.click_to_select"));
            }
        } else if (!this.active) {
            lines.add(Component.translatable("tooltip.herosigil.click_to_activate"));

            // Show what buff will be activated
            java.util.List<com.hero.sigil.buffs.BuffEffect> buffs =
                com.hero.sigil.buffs.HeroSigilData.getAllBuffs();

            if (this.slotId >= 0 && this.slotId < Math.min(buffs.size(), 3)) {
                com.hero.sigil.buffs.BuffEffect buff = buffs.get(this.slotId);
                net.minecraft.world.effect.MobEffect effect = buff.getMobEffect();
                if (effect != null) {
                    lines.add(Component.translatable(effect.getDescriptionId()).withStyle(net.minecraft.network.chat.Style.EMPTY.withBold(true)));

                    // 显示放大器等级
                    int amplifier = buff.getAmplifier();
                    if (amplifier > 0) {
                        lines.add(Component.translatable("tooltip.herosigil.amplifier", amplifier + 1));
                    }
                }
            }
        } else {
            // Show current active status and duration info
            java.util.List<com.hero.sigil.buffs.BuffEffect> buffs =
                com.hero.sigil.buffs.HeroSigilData.getAllBuffs();

            if (this.slotId >= 0 && this.slotId < Math.min(buffs.size(), 3)) {
                com.hero.sigil.buffs.BuffEffect buff = buffs.get(this.slotId);

                lines.add(Component.translatable("tooltip.herosigil.active_status",
                    Component.literal("✓").withStyle(net.minecraft.network.chat.Style.EMPTY.withBold(true))));

                if (!buff.isPermanent()) {
                    // 显示剩余时间
                    long currentTick = net.minecraft.client.Minecraft.getInstance().player.tickCount;
                    long remainingTime = buff.getRemainingSeconds(currentTick);
                    lines.add(Component.translatable("tooltip.herosigil.remaining_time", remainingTime));

                    // 显示刷新间隔
                    long refreshInterval = buff.getRefreshIntervalSeconds();
                    lines.add(Component.translatable("tooltip.herosigil.refresh_interval", refreshInterval));
                } else {
                    lines.add(Component.translatable("tooltip.herosigil.permanent_info"));
                }

                // 显示停用提示
                lines.add(Component.translatable("tooltip.herosigil.click_to_deactivate"));
            }
        }

        return lines;
    }

    @Override
    public net.minecraft.client.gui.components.Tooltip getNarration() {
        return super.getNarration();
    }

    /**
     * Set the unlocked state of this buff slot.
     */
    public void setUnlocked(boolean pUnlocked) {
        if (this.unlocked != pUnlocked) {
            this.unlocked = pUnlocked;
            // Reset tooltip cache when state changes
            this.tooltip = null;
        }
    }

    /**
     * Check if this buff slot is unlocked.
     */
    public boolean isUnlocked() {
        return this.unlocked;
    }

    /**
     * Set the active state of this buff slot.
     */
    public void setActive(boolean pActive) {
        if (this.active != pActive) {
            this.active = pActive;
            // Reset tooltip cache when state changes
            this.tooltip = null;

            // Play sound for visual feedback
            Minecraft minecraft = Minecraft.getInstance();
            if (pActive) {
                minecraft.getSoundSource().play(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK_PRESS,
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.defaultVolume()
                );
            } else {
                minecraft.getSoundSource().play(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK_RELEASE,
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.defaultVolume()
                );
            }
        }
    }

    /**
     * Check if this buff slot is currently active.
     */
    public boolean isActive() {
        return this.active;
    }

    /**
     * Update internal state (can be called to refresh UI).
     */
    public void updateState() {
        // Reset tooltip cache on state change
        if (this.tooltip != null) {
            this.tooltip = net.minecraft.client.gui.components.Tooltip.create(
                getTooltip().getVisualOrder(),
                getTooltipLines()
            );
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
        // No special narration needed for this widget
    }
}
