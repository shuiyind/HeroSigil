package com.hero.sigil.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.RegisterPayloadHandlersCallback;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet for synchronizing buff slot states between client and server.
 */
@EventBusSubscriber(modid = "herosigil", bus = EventBusSubscriber.Bus.GAME)
public class BuffSlotSyncPacket {
    
    public static final String TYPE_ID = "buff_slot_sync";
    
    private final int playerId;
    private final int[] buffStates;

    public BuffSlotSyncPacket(int pPlayerId, int[] pbuffStates) {
        this.playerId = pPlayerId;
        this.buffStates = java.util.Arrays.copyOf(pbuffStates, 3);
    }

    /**
     * Register the payload handler.
     */
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersCallback event) {
        // The packet type registration will be handled by NeoForge automatically
    }

    /**
     * Encode the packet data for network transmission.
     */
    public void toBytes(FriendlyByteBuf pBuffer) {
        pBuffer.writeInt(this.playerId);
        
        for (int i = 0; i < 3; i++) {
            if (i < this.buffStates.length) {
                pBuffer.writeInt(this.buffStates[i]);
            } else {
                pBuffer.writeInt(0);
            }
        }
    }

    /**
     * Decode the packet data from network reception.
     */
    public static BuffSlotSyncPacket fromBytes(FriendlyByteBuf pBuffer) {
        int playerId = pBuffer.readInt();
        int[] buffStates = new int[3];
        
        for (int i = 0; i < 3; i++) {
            buffStates[i] = pBuffer.readInt();
        }
        
        return new BuffSlotSyncPacket(playerId, buffStates);
    }

    /**
     * Handle the packet on the receiving side.
     */
    public static void handle(BuffSlotSyncPacket pPacket, IPayloadContext pContext) {
        // Queue work to main thread
        pContext.enqueueWork(() -> {
            updateClientBuffStates(pPacket.playerId, pPacket.buffStates);
        });
        
        // Acknowledge receipt (optional)
    }

    private static void updateClientBuffStates(int pPlayerId, int[] pbuffStates) {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        
        if (minecraft.player != null && minecraft.player.getId() == pPlayerId) {
            // Update the current player's buff slots in the GUI menu
            if (minecraft.screen instanceof com.hero.sigil.gui.screen.HeroSigilScreen screen) {
                var menu = screen.getMenu();
                
                for (int i = 0; i < Math.min(3, pbuffStates.length); i++) {
                    int state = pbuffStates[i];
                    boolean unlocked = (state & 0x1) != 0; // Bit 0: unlocked flag
                    boolean active = (state & 0x2) != 0;   // Bit 1: active flag
                    
                    updateBuffSlotInScreen(screen, i, unlocked, active);
                }
            }
        }
    }

    private static void updateBuffSlotInScreen(com.hero.sigil.gui.screen.HeroSigilScreen pScreen, 
                                               int pSlotIndex, boolean pUnlocked, 
                                               boolean pActive) {
        // TODO: Get the specific buff slot widget and update its state
        switch (pSlotIndex) {
            case 0:
                if (pScreen.buffSlot1 != null) {
                    pScreen.buffSlot1.setUnlocked(pUnlocked);
                    pScreen.buffSlot1.setActive(pActive);
                    pScreen.buffSlot1.updateState();
                }
                break;
            case 1:
                if (pScreen.buffSlot2 != null) {
                    pScreen.buffSlot2.setUnlocked(pUnlocked);
                    pScreen.buffSlot2.setActive(pActive);
                    pScreen.buffSlot2.updateState();
                }
                break;
            case 2:
                if (pScreen.buffSlot3 != null) {
                    pScreen.buffSlot3.setUnlocked(pUnlocked);
                    pScreen.buffSlot3.setActive(pActive);
                    pScreen.buffSlot3.updateState();
                }
                break;
        }
    }

    /**
     * Get the player ID for this packet.
     */
    public int getPlayerId() {
        return this.playerId;
    }

    /**
     * Get the buff states array (length 3).
     */
    public int[] getBuffStates() {
        return java.util.Arrays.copyOf(this.buffStates, 3);
    }

    /**
     * Create a packet from player and current state.
     */
    public static BuffSlotSyncPacket createForPlayer(net.minecraft.world.entity.player.Player pPlayer) {
        int[] buffStates = new int[3];
        
        // Get buff states from HeroSigilData
        System.arraycopy(com.hero.sigil.buffs.HeroSigilData.getBuffStatesArray(pPlayer), 0, buffStates, 0, Math.min(3, com.hero.sigil.buffs.HeroSigilData.getAllBuffs().size()));
        
        return new BuffSlotSyncPacket(pPlayer.getId(), buffStates);
    }

}
