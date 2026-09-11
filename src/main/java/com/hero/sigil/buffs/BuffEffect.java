package com.hero.sigil.buffs;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Represents a buff effect that can be unlocked and activated on the Hero Sigil.
 */
public class BuffEffect {

    private final String id;
    private final MobEffect mobEffect;
    private final int amplifier;
    private final int durationSeconds;
    private final boolean isPermanent;
    
    // Whether this buff has been unlocked by achievements
    private boolean unlocked = false;
    
    // Current active status (can be toggled on/off)
    private boolean active = false;

    public BuffEffect(String id, MobEffect mobEffect, int amplifier, int durationSeconds, boolean isPermanent) {
        this.id = id;
        this.mobEffect = mobEffect;
        this.amplifier = amplifier;
        this.durationSeconds = durationSeconds;
        this.isPermanent = isPermanent;
    }

    /**
     * Apply the buff effect to a player if active.
     */
    public void applyBuff(Player player) {
        if (!active || !unlocked) return;
        
        int duration = isPermanent ? 999999 : durationSeconds;
        
        // Remove existing effect if any
        player.removeEffect(mobEffect);
        
        // Apply new effect instance
        MobEffectInstance effectInstance = new MobEffectInstance(
            mobEffect, 
            duration * 20, // Convert seconds to ticks (1 second = 20 ticks)
            amplifier,     // Amplifier level (0 = base, 1 = stronger, etc.)
            false,         // Particles visible
            true           // Show in status effects GUI
        );
        
        player.addEffect(effectInstance);
    }

    /**
     * Remove the buff effect from a player.
     */
    public void removeBuff(Player player) {
        if (player.hasEffect(mobEffect)) {
            player.removeEffect(mobEffect);
        }
    }

    /**
     * Toggle active status of this buff.
     */
    public void toggleActive() {
        active = !active;
        
        // Get the player to apply/remove effect immediately
        if (active) {
            // Will be applied on next tick by HeroSigilItem.onPlayerTick()
        } else {
            Player currentPlayer = net.minecraft.client.Minecraft.getInstance().player;
            if (currentPlayer != null && !currentPlayer.level().isClientSide()) {
                removeBuff(currentPlayer);
            }
        }
    }

    public String getId() { return id; }
    public MobEffect getMobEffect() { return mobEffect; }
    public int getAmplifier() { return amplifier; }
    public int getDurationSeconds() { return durationSeconds; }
    public boolean isPermanent() { return isPermanent; }
    
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
            MobEffects.SPEED,
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
