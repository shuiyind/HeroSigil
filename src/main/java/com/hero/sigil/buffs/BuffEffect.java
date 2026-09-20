package com.hero.sigil.buffs;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * Represents a buff effect that can be unlocked and activated on the Hero Sigil.
 */
public class BuffEffect {

    private final String id;
    private final Holder<MobEffect> mobEffect;
    private final int amplifier;
    private final int durationSeconds;
    private final boolean isPermanent;

    // 刷新间隔（秒）：非永久 buff 需要周期性刷新
    private final int refreshIntervalSeconds;

    // 当前激活状态的最后刷新时间（tick 计数）
    private long lastRefreshTick = 0;

    // Whether this buff has been unlocked by achievements
    private boolean unlocked = false;

    // Current active status (can be toggled on/off)
    private boolean active = false;

    // 速度来源追踪（仅用于 SPEED buff，用于冲突处理）
    private boolean appliedByThisBuff = false;

    public BuffEffect(String id, Holder<MobEffect> mobEffect, int amplifier, int durationSeconds, boolean isPermanent) {
        this.id = id;
        this.mobEffect = mobEffect;
        this.amplifier = amplifier;
        this.durationSeconds = durationSeconds;
        this.isPermanent = isPermanent;
        // 非永久 buff 的刷新间隔为持续时间的 80%
        this.refreshIntervalSeconds = isPermanent ? 0 : (int) (durationSeconds * 0.8);
        this.lastRefreshTick = 0;
        this.appliedByThisBuff = false;
    }

    /**
     * 应用 buff 效果到玩家
     */
    public void applyBuff(Player player) {
        if (!active || !unlocked) return;

        // 如果是永久 buff，直接应用
        if (isPermanent) {
            int duration = 999999;

            // 先移除已有的效果实例（避免重复添加）
            player.removeEffect(mobEffect);

            MobEffectInstance effectInstance = new MobEffectInstance(
                mobEffect,
                duration * 20, // Convert seconds to ticks (1 second = 20 ticks)
                amplifier,     // Amplifier level (0 = base, 1 = stronger, etc.)
                false,         // Particles visible
                true           // Show in status effects GUI
            );

            player.addEffect(effectInstance);

            // 处理永久 buff 的最大生命值变化
            if (mobEffect == MobEffects.HEALTH_BOOST) {
                // 重新计算最大生命值

                // 调整当前生命值（不超过新最大值）
                if (player.getHealth() > player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }

                // 通知玩家最大生命值变化
                int heartsAdded = amplifier * 2; // 每个放大器 +2 颗心
                player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("§a§l勇者之证§r§f: 最大生命值 +" + heartsAdded + " 颗心！")
                );

                // 播放生命值增长粒子效果
                player.level().addParticle(
                    net.minecraft.core.particles.ParticleTypes.HEART,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    0.0, 0.0, 0.0
                );

                com.hero.sigil.HeroSigil.LOGGER.info("Applied HEALTH_BOOST to player {}, new max health: {}",
                    player.getScoreboardName(), player.getMaxHealth());
            }

            com.hero.sigil.HeroSigil.LOGGER.debug("Applied permanent buff {} to player {}", id, player.getScoreboardName());
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

        // 加速 buff 冲突处理：不覆盖玩家已有的更强加速效果
        if (mobEffect == MobEffects.MOVEMENT_SPEED) {
            MobEffectInstance existingSpeedEffect = player.getEffect(mobEffect);
            if (existingSpeedEffect != null && existingSpeedEffect.getAmplifier() > amplifier) {
                com.hero.sigil.HeroSigil.LOGGER.debug("Skipping SPEED buff application, stronger effect already active");
                return;
            }
        }

        // 应用新的效果实例
        int duration = durationSeconds;
        player.removeEffect(mobEffect);

        MobEffectInstance effectInstance = new MobEffectInstance(
            mobEffect,
            duration * 20, // Convert seconds to ticks (1 second = 20 ticks)
            amplifier,     // Amplifier level (0 = base, 1 = stronger, etc.)
            false,         // Particles visible
            true           // Show in status effects GUI
        );

        player.addEffect(effectInstance);

        // 记录刷新时间
        lastRefreshTick = currentTick;

        // 加速 buff 特殊处理：添加视觉反馈
        if (mobEffect == MobEffects.MOVEMENT_SPEED) {
            appliedByThisBuff = true;

            // 播放速度粒子效果（淡蓝色粒子）
            player.level().addParticle(
                net.minecraft.core.particles.ParticleTypes.CLOUD,
                player.getX(), player.getY() + 0.5, player.getZ(),
                0.0, 0.0, 0.0
            );

            // 通知玩家
            player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal("§b§l勇者之证§r§f: 加速效果已激活！")
            );
        }

        com.hero.sigil.HeroSigil.LOGGER.debug("Applied/refreshed buff {} to player {} (interval: {}s)",
            id, player.getScoreboardName(), refreshIntervalSeconds);
    }

    /**
     * 从玩家移除 buff 效果
     */
    public void removeBuff(Player player) {
        if (player.hasEffect(mobEffect)) {
            player.removeEffect(mobEffect);
            lastRefreshTick = 0; // 重置刷新计时器

            // 处理永久 buff 的最大生命值恢复
            if (mobEffect == MobEffects.HEALTH_BOOST) {
                // 重新计算最大生命值（移除生命提升）

                // 调整当前生命值（不超过新最大值）
                if (player.getHealth() > player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }

                // 通知玩家最大生命值恢复
                int heartsRemoved = amplifier * 2;
                player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("§c§l勇者之证§r§f: 最大生命值 -" + heartsRemoved + " 颗心")
                );

                // 播放生命值减少粒子效果（红色）
                player.level().addParticle(
                    net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    0.0, 0.0, 0.0
                );

                com.hero.sigil.HeroSigil.LOGGER.info("Removed HEALTH_BOOST from player {}, new max health: {}",
                    player.getScoreboardName(), player.getMaxHealth());
            }

            // 加速 buff 特殊处理
            if (mobEffect == MobEffects.MOVEMENT_SPEED) {
                appliedByThisBuff = false;

                // 播放速度移除粒子效果（云朵粒子）
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

            com.hero.sigil.HeroSigil.LOGGER.debug("Removed buff {} from player {}", id, player.getScoreboardName());
        }
    }

    /**
     * 切换 buff 激活状态
     */
    public void toggleActive() {
        boolean wasActive = active;
        active = !active;
        Player currentPlayer = net.minecraft.client.Minecraft.getInstance().player;

        if (active) {
            // 播放激活音效
            if (currentPlayer != null && !currentPlayer.level().isClientSide()) {
                currentPlayer.level().playSound(
                    null,
                    currentPlayer.getBlockX(),
                    currentPlayer.getBlockY(),
                    currentPlayer.getBlockZ(),
                    com.hero.sigil.registry.ModRegistries.BUFF_ACTIVATE.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    1.0f,
                    1.0f
                );
            }
            com.hero.sigil.HeroSigil.LOGGER.info("Buff {} activated for player {}", id,
                currentPlayer != null ? currentPlayer.getScoreboardName() : "unknown");
        } else {
            // 播放停用音效
            if (currentPlayer != null && !currentPlayer.level().isClientSide() && wasActive) {
                currentPlayer.level().playSound(
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
            com.hero.sigil.HeroSigil.LOGGER.info("Buff {} deactivated for player {}", id,
                currentPlayer != null ? currentPlayer.getScoreboardName() : "unknown");
        }
    }

    public String getId() { return id; }
    public Holder<MobEffect> getMobEffect() { return mobEffect; }
    public int getAmplifier() { return amplifier; }
    public int getDurationSeconds() { return durationSeconds; }
    public boolean isPermanent() { return isPermanent; }

    /**
     * 获取刷新间隔（秒）
     */
    public long getRefreshIntervalSeconds() {
        return refreshIntervalSeconds;
    }

    /**
     * 计算剩余时间（秒）
     */
    public long getRemainingSeconds(long currentTick) {
        if (isPermanent) {
            return -1; // 永久 buff
        }
        long elapsedTicks = currentTick - lastRefreshTick;
        long remainingTicks = refreshIntervalSeconds * 20 - elapsedTicks;
        if (remainingTicks < 0) {
            return 0;
        }
        return remainingTicks / 20;
    }

    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) {
        if (this.active != active) {
            // If toggling from inactive to active, apply immediately
            Player currentPlayer = net.minecraft.client.Minecraft.getInstance().player;
            if (currentPlayer != null && !currentPlayer.level().isClientSide()) {
                removeBuff(currentPlayer);
            }
        }
        this.active = active;
    }

    /**
     * 检查此 buff 是否已应用（用于冲突处理）
     */
    public boolean isAppliedByThisBuff() {
        return appliedByThisBuff;
    }

    /**
     * Create all available buff effects for the Hero Sigil.
     */
    public static java.util.List<BuffEffect> createAllBuffs() {
        java.util.ArrayList<BuffEffect> buffs = new java.util.ArrayList<>();

        // Slot 1: Saturation - from DEFEAT_BOSS achievement
        buffs.add(new BuffEffect(
            "saturation",
            MobEffects.SATURATION,
            0,      // Amplifier level
            30,     // Duration in seconds (re-applied periodically)
            false   // Not permanent
        ));

        // Slot 2: Health Boost - from EXPLORE_ALL_BIOMES achievement
        buffs.add(new BuffEffect(
            "health_boost",
            MobEffects.HEALTH_BOOST,
            1,      // +4 max health (each level adds 2 hearts)
            0,      // 0 = permanent effect
            true    // Permanent while equipped
        ));

        // Slot 3: Speed - from DEFEAT_ENDER_DRAGON achievement
        buffs.add(new BuffEffect(
            "speed",
            MobEffects.MOVEMENT_SPEED,
            1,      // +25% speed (amplifier level 0 = 20%, level 1 = 45%)
            30,     // Duration in seconds (re-applied periodically)
            false   // Not permanent - needs periodic refresh
        ));

        return buffs;
    }

    /**
     * Get a buff effect by ID.
     */
    public static BuffEffect getBuffById(String id) {
        for (BuffEffect buff : createAllBuffs()) {
            if (buff.getId().equals(id)) {
                return buff;
            }
        }
        return null;
    }

    /**
     * Get the default unlocked buffs based on achievement progress.
     */
    public static void applyDefaultUnlocks(Player player) {
        // For now, unlock all slots as a starting point for testing
        // In production, this would check AchievementTracker.isAchievementUnlocked()

        java.util.List<BuffEffect> buffs = createAllBuffs();
        for (int i = 0; i < Math.min(buffs.size(), HeroSigilData.getMaxBuffSlots(player)); i++) {
            buffs.get(i).setUnlocked(true);
        }
    }

    /**
     * Check if a specific buff type is unlocked.
     */
    public static boolean isBuffTypeUnlocked(Player player, String buffId) {
        BuffEffect buff = getBuffById(buffId);
        return buff != null && buff.isUnlocked();
    }
}
