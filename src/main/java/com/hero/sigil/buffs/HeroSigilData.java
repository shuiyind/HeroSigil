package com.hero.sigil.buffs;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Manages Hero Sigil buff data for a player.
 * This class handles saving/loading buff states and managing active buffs.
 */
public class HeroSigilData {

    private static final String TAG_KEY = "HeroSigil";
    
    // List of all available buff effects
    private static java.util.List<BuffEffect> ALL_BUFFS;
    
    /**
     * Initialize the list of available buff effects.
     */
    public static void initBuffs() {
        if (ALL_BUFFS == null) {
            ALL_BUFFS = BuffEffect.createAllBuffs();
        }
    }

    /**
     * Get all available buffs, initialized on first call.
     */
    public static java.util.List<BuffEffect> getAllBuffs() {
        initBuffs();
        return ALL_BUFFS;
    }

    /**
     * Save buff states to player NBT data.
     */
    public static void onSave(Player player, CompoundTag nbt) {
        CompoundTag sigilData = new CompoundTag();
        
        java.util.List<BuffEffect> buffs = getAllBuffs();
        for (int i = 0; i < Math.min(buffs.size(), 3); i++) { // Max 3 slots
            BuffEffect buff = buffs.get(i);
            String slotKey = "slot_" + (i + 1);
            
            CompoundTag slotData = new CompoundTag();
            slotData.putBoolean("unlocked", buff.isUnlocked());
            slotData.putBoolean("active", buff.isActive());
            sigilData.setTag(slotKey, slotData);
        }
        
        nbt.put(TAG_KEY, sigilData);
    }

    /**
     * Load buff states from player NBT data.
     */
    public static void onLoad(Player player, CompoundTag nbt) {
        if (nbt.contains(TAG_KEY)) {
            CompoundTag sigilData = nbt.getCompound(TAG_KEY);
            
            java.util.List<BuffEffect> buffs = getAllBuffs();
            for (int i = 0; i < Math.min(buffs.size(), 3); i++) { // Max 3 slots
                BuffEffect buff = buffs.get(i);
                String slotKey = "slot_" + (i + 1);
                
                if (sigilData.contains(slotKey)) {
                    CompoundTag slotData = sigilData.getCompound(slotKey);
                    buff.setUnlocked(slotData.getBoolean("unlocked"));
                    buff.setActive(slotData.getBoolean("active"));
                    
                    // Re-apply active buffs after loading
                    if (buff.isActive() && buff.isUnlocked()) {
                        buff.applyBuff(player);
                    }
                }
            }
        } else {
            // First time - apply default unlocks based on achievements
            applyDefaultBuffsForPlayer(player);
        }
    }

    /**
     * Get the number of unlocked slots for a player.
     */
    public static int getMaxBuffSlots(Player player) {
        return com.hero.sigil.achievement.AchievementTracker.getMaxBuffSlots(player);
    }

    /**
     * Apply default buffs based on current achievement progress.
     */
    private static void applyDefaultBuffsForPlayer(Player player) {
        java.util.List<BuffEffect> buffs = getAllBuffs();
        
        int unlockedCount = com.hero.sigil.achievement.AchievementTracker.getUnlockedCount(player);
        
        // Unlock slots based on achievements (max 3)
        for (int i = 0; i < Math.min(unlockedCount, buffs.size()); i++) {
            buffs.get(i).setUnlocked(true);
            
            // Auto-activate unlocked buffs by default
            if (!buffs.get(i).isActive()) {
                buffs.get(i).toggleActive();
            }
        }
    }

    /**
     * Sync buff states from achievement tracker to buff data.
     */
    public static void syncFromAchievements(Player player) {
        java.util.List<BuffEffect> buffs = getAllBuffs();
        
        // Update unlocked status based on achievements
        for (int i = 0; i < Math.min(buffs.size(), AchievementTracker.AchievementType.values().length); i++) {
            AchievementTracker.AchievementType achievement = AchievementTracker.AchievementType.values()[i];
            
            if (com.hero.sigil.achievement.AchievementTracker.isAchievementUnlocked(player, achievement)) {
                buffs.get(i).setUnlocked(true);
                
                // Auto-activate newly unlocked buff
                if (!buffs.get(i).isActive()) {
                    buffs.get(i).toggleActive();
                    
                    // Apply immediately for server-side players
                    if (!player.level().isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer) {
                        buffs.get(i).applyBuff(player);
                    }
                }
            }
        }
    }

    /**
     * Get all current buff states as an array for network synchronization.
     */
    public static int[] getBuffStatesArray(Player player) {
        java.util.List<BuffEffect> buffs = getAllBuffs();
        int[] states = new int[3]; // Max 3 slots
        
        for (int i = 0; i < Math.min(buffs.size(), 3); i++) {
            BuffEffect buff = buffs.get(i);
            
            // Encode state: bit 0 = unlocked, bit 1 = active, bits 8-15 = buff type index
            int state = (buff.isUnlocked() ? 0x1 : 0) | 
                       (buff.isActive() ? 0x2 : 0);
            
            states[i] = state;
        }
        
        return states;
    }

    /**
     * Activate a specific buff slot.
     */
    public static void activateSlot(Player player, int slotIndex) {
        java.util.List<BuffEffect> buffs = getAllBuffs();
        
        if (slotIndex >= 0 && slotIndex < Math.min(buffs.size(), 3)) {
            BuffEffect buff = buffs.get(slotIndex);
            
            // Check if unlocked by achievements
            AchievementTracker.AchievementType achievement = 
                AchievementTracker.AchievementType.values()[slotIndex];
            
            if (com.hero.sigil.achievement.AchievementTracker.isAchievementUnlocked(player, achievement) || 
                buff.isUnlocked()) {
                
                boolean wasActive = buff.isActive();
                buff.toggleActive();
                
                // If deactivating, remove the effect
                if (!buff.isActive() && !wasActive) {
                    player.removeEffect(buff.getMobEffect());
                } else if (buff.isActive() && !wasActive) {
                    // Apply new effect when activating
                    buff.applyBuff(player);
                    
                    // Sync to client
                    syncToClient(player);
                }
            }
        }
    }

    /**
     * Send updated buff states to the player's client.
     */
    public static void syncToClient(Player player) {
        int[] states = getBuffStatesArray(player);
        
        com.hero.sigil.network.HeroSigilNetworkManager.sendToPlayer(
            new com.hero.sigil.network.BuffSlotSyncPacket(player.getId(), states), 
            (net.minecraft.server.level.ServerPlayer) player
        );
    }

    /**
     * Reset all buff data for a player.
     */
    public static void reset(Player player) {
        java.util.List<BuffEffect> buffs = getAllBuffs();
        
        // Remove all active effects from player
        for (BuffEffect buff : buffs) {
            if (!player.level().isClientSide()) {
                buff.removeBuff(player);
            }
            buff.setUnlocked(false);
            buff.setActive(false);
        }
    }

    /**
     * Apply all active buffs to the player. Called periodically by HeroSigilItem.onPlayerTick()
     */
    public static void applyAllBuffs(Player player) {
        java.util.List<BuffEffect> buffs = getAllBuffs();
        
        for (BuffEffect buff : buffs) {
            if (buff.isActive() && buff.isUnlocked()) {
                buff.applyBuff(player);
            }
        }
    }

    /**
     * Get the current unlocked status of all slots.
     */
    public static boolean[] getSlotStatus(Player player) {
        java.util.List<BuffEffect> buffs = getAllBuffs();
        boolean[] statuses = new boolean[3]; // Max 3 slots
        
        for (int i = 0; i < Math.min(buffs.size(), 3); i++) {
            statuses[i] = buffs.get(i).isUnlocked();
        }
        
        return statuses;
    }

    /**
     * Check if a specific slot is unlocked.
     */
    public static boolean isSlotUnlocked(Player player, int slotIndex) {
        java.util.List<BuffEffect> buffs = getAllBuffs();
        
        if (slotIndex >= 0 && slotIndex < Math.min(buffs.size(), 3)) {
            return buffs.get(slotIndex).isUnlocked();
        }
        
        return false;
    }

    /**
     * Check if a specific slot is active.
     */
    public static boolean isSlotActive(Player player, int slotIndex) {
        java.util.List<BuffEffect> buffs = getAllBuffs();
        
        if (slotIndex >= 0 && slotIndex < Math.min(buffs.size(), 3)) {
            return buffs.get(slotIndex).isActive();
        }
        
        return false;
    }

    /**
     * Get the MobEffects that are currently active for a player.
     */
    public static java.util.List<MobEffectInstance> getActiveEffects(Player player) {
        java.util.List<BuffEffect> buffs = getAllBuffs();
        java.util.ArrayList<MobEffectInstance> effects = new java.util.ArrayList<>();
        
        for (BuffEffect buff : buffs) {
            if (buff.isActive() && buff.isUnlocked()) {
                // Get the current effect instance from the player
                MobEffectInstance existingEffect = player.getEffect(buff.getMobEffect());
                if (existingEffect != null) {
                    effects.add(existingEffect);
                }
            }
        }
        
        return effects;
    }

    /**
     * Get a list of all unlocked and active buff names for display.
     */
    public static java.util.List<String> getActiveBuffNames(Player player) {
        java.util.List<BuffEffect> buffs = getAllBuffs();
        java.util.ArrayList<String> activeNames = new java.util.ArrayList<>();
        
        for (BuffEffect buff : buffs) {
            if (buff.isActive() && buff.isUnlocked()) {
                // Get the localized name from the MobEffect's translation key
                String displayName = buff.getMobEffect().getDescriptionId();
                activeNames.add(displayName);
            }
        }
        
        return activeNames;
    }

    /**
     * Check if any buffs are currently active for a player.
     */
    public static boolean hasActiveBuffs(Player player) {
        java.util.List<BuffEffect> buffs = getAllBuffs();
        
        for (BuffEffect buff : buffs) {
            if (buff.isActive() && buff.isUnlocked()) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Count the number of active buffs.
     */
    public static int getActiveBuffsCount(Player player) {
        java.util.List<BuffEffect> buffs = getAllBuffs();
        int count = 0;
        
        for (BuffEffect buff : buffs) {
            if (buff.isActive() && buff.isUnlocked()) {
                count++;
            }
        }
        
        return count;
    }

    /**
     * Get the current tick-based state of all active buffs. Used for periodic refresh.
     */
    public static int[] getBuffTickState(Player player) {
        java.util.List<BuffEffect> buffs = getAllBuffs();
        int[] states = new int[3]; // Max 3 slots
        
        for (int i = 0; i < Math.min(buffs.size(), 3); i++) {
            BuffEffect buff = buffs.get(i);
            
            // Store: bit 0 = has effect, bits 1-5 = remaining ticks (simplified)
            int tickState = 0x0;
            if (!player.level().isClientSide()) {
                MobEffectInstance currentEffect = player.getEffect(buff.getMobEffect());
                if (currentEffect != null && buff.isActive() && buff.isUnlocked()) {
                    tickState |= 0x1; // Mark as having effect
                    
                    // Store remaining ticks (scaled down for storage efficiency)
                    int remainingTicks = Math.min(currentEffect.getDuration(), 320); // Cap at 16 seconds
                    states[i] = tickState | (remainingTicks << 8);
                } else {
                    states[i] = 0;
                }
            }
        }
        
        return states;
    }

    /**
     * Force unlock a buff slot for testing purposes.
     */
    public static void forceUnlockSlot(Player player, int slotIndex) {
        java.util.List<BuffEffect> buffs = getAllBuffs();
        
        if (slotIndex >= 0 && slotIndex < Math.min(buffs.size(), 3)) {
            buffs.get(slotIndex).setUnlocked(true);
            
            // Sync to client
            syncToClient(player);
        }
    }

    /**
     * Force activate a buff slot for testing purposes.
     */
    public static void forceActivateSlot(Player player, int slotIndex) {
        java.util.List<BuffEffect> buffs = getAllBuffs();
        
        if (slotIndex >= 0 && slotIndex < Math.min(buffs.size(), 3)) {
            BuffEffect buff = buffs.get(slotIndex);
            
            // Ensure unlocked first
            buff.setUnlocked(true);
            buff.toggleActive();
            
            // Apply immediately
            if (!player.level().isClientSide()) {
                buff.applyBuff(player);
                
                // Sync to client
                syncToClient(player);
            }
        }
    }

    /**
     * Get the current state of all buffs for debugging purposes.
     */
    public static java.util.Map<String, Object> getDebugState(Player player) {
        java.util.List<BuffEffect> buffs = getAllBuffs();
        java.util.HashMap<String, Object> debugInfo = new java.util.HashMap<>();
        
        for (int i = 0; i < Math.min(buffs.size(), 3); i++) {
            BuffEffect buff = buffs.get(i);
            
            // Get current effect instance if any
            MobEffectInstance currentEffect = !player.level().isClientSide() ? 
                player.getEffect(buff.getMobEffect()) : null;
            
            java.util.HashMap<String, Object> slotInfo = new java.util.HashMap<>();
            slotInfo.put("id", buff.getId());
            slotInfo.put("unlocked", buff.isUnlocked());
            slotInfo.put("active", buff.isActive());
            slotInfo.put("hasEffect", currentEffect != null);
            
            if (currentEffect != null) {
                slotInfo.put("durationTicks", currentEffect.getDuration());
                slotInfo.put("amplifier", currentEffect.getAmplifier());
            }
            
            debugInfo.put("slot_" + (i + 1), slotInfo);
        }
        
        return debugInfo;
    }

}
