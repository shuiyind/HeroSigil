package com.hero.sigil.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet to toggle a buff slot (activate/deactivate) from client to server.
 */
public class CPacketToggleBuffSlot {

    private final int playerId;
    private final int slotIndex; // 0, 1, or 2

    public CPacketToggleBuffSlot(int pPlayerId, int pSlotIndex) {
        this.playerId = pPlayerId;
        this.slotIndex = pSlotIndex;
    }

    /**
     * Encode packet data for network transmission.
     */
    public void toBytes(net.minecraft.network.FriendlyByteBuf pBuffer) {
        pBuffer.writeInt(this.playerId);
        pBuffer.writeInt(this.slotIndex);
    }

    /**
     * Decode packet data from network reception.
     */
    public static CPacketToggleBuffSlot fromBytes(net.minecraft.network.FriendlyByteBuf pBuffer) {
        int playerId = pBuffer.readInt();
        int slotIndex = pBuffer.readInt();
        return new CPacketToggleBuffSlot(playerId, slotIndex);
    }

    /**
     * Handle the packet on the receiving side (server).
     */
    public static void handle(CPacketToggleBuffSlot pPacket, IPayloadContext pContext) {
        // Queue work to main thread
        pContext.enqueueWork(() -> {
            net.minecraft.server.level.ServerPlayer player =
                (net.minecraft.server.level.ServerPlayer) pContext.player();

            if (player != null && pPacket.slotIndex >= 0 && pPacket.slotIndex < 3) {
                // Toggle the buff slot on server side
                com.hero.sigil.buffs.HeroSigilData.activateSlot(player, pPacket.slotIndex);

                // Sync updated state to client
                BuffSlotSyncPacket syncPacket = new BuffSlotSyncPacket(
                    player.getId(),
                    com.hero.sigil.buffs.HeroSigilData.getBuffStatesArray(player)
                );
                HeroSigilNetworkManager.sendToPlayer(syncPacket, (ServerPlayer) player);
            }
        });
    }

    /**
     * Get the player ID for this packet.
     */
    public int getPlayerId() {
        return this.playerId;
    }

    /**
     * Get the slot index for this packet (0, 1, or 2).
     */
    public int getSlotIndex() {
        return this.slotIndex;
    }
}
