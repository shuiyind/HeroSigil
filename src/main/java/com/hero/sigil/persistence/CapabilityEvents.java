package com.hero.sigil.persistence;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Event handlers for Hero Sigil data persistence.
 */
@EventBusSubscriber(modid = "herosigil", bus = EventBusSubscriber.Bus.GAME)
public class CapabilityEvents {

    private static final String TAG_KEY = "HeroSigil";

    /**
     * Save player data when they disconnect.
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.LoggedOutEvent event) {
        if (event.getPlayer() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            CompoundTag persistData = serverPlayer.getPersistentData();
            
            // Save buff states and achievement progress to persistent data
            com.hero.sigil.buffs.HeroSigilData.onSave(serverPlayer, persistData);
            
            com.hero.sigil.HeroSigil.LOGGER.debug(
                "Saved Hero Sigil data for player: {}", 
                serverPlayer.getScoreboardName()
            );
        }
    }

    /**
     * Load player data when they connect.
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            CompoundTag persistData = serverPlayer.getPersistentData();
            
            // Load buff states and achievement progress from persistent data
            com.hero.sigil.buffs.HeroSigilData.onLoad(serverPlayer, persistData);
            
            com.hero.sigil.HeroSigil.LOGGER.info(
                "Loaded Hero Sigil data for player: {}", 
                serverPlayer.getScoreboardName()
            );
        }
    }

    /**
     * Handle player death - preserve or reset data based on config.
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            net.minecraft.world.entity.player.Player original = event.getOriginal();
            
            if (original instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                CompoundTag origPersistData = serverPlayer.getPersistentData();
                
                // Get the saved Hero Sigil data from original player
                if (origPersistData.contains(TAG_KEY)) {
                    CompoundTag sigilNbt = origPersistData.getCompound(TAG_KEY);
                    
                    // Copy to new player's persistent data
                    net.minecraft.server.level.ServerPlayer newPlayer = 
                        (net.minecraft.server.level.ServerPlayer) event.getEntity();
                    CompoundTag newPersistData = newPlayer.getPersistentData();
                    
                    if (!newPersistData.contains(TAG_KEY)) {
                        newPersistData.put(TAG_KEY, sigilNbt.copy());
                        
                        // Load the data into buff system
                        com.hero.sigil.buffs.HeroSigilData.onLoad(
                            event.getEntity(), 
                            newPersistData
                        );
                    }
                }
            }
        }
    }

}
