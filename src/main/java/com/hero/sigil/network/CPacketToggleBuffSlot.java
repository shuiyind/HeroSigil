package com.hero.sigil.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet to toggle a buff slot (activate/deactivate) from client to server.
 */
public class CPacketToggleBuffSlot implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CPacketToggleBuffSlot> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("herosigil", "toggle_buff_slot"));

    public static final StreamCodec<FriendlyByteBuf, CPacketToggleBuffSlot> STREAM_CODEC = StreamCodec.of(
        (FriendlyByteBuf buf, CPacketToggleBuffSlot packet) -> {
            buf.writeInt(packet.playerId);
            buf.writeInt(packet.slotIndex);
        },
        buf -> new CPacketToggleBuffSlot(buf.readInt(), buf.readInt())
    );

    private final int playerId;
    private final int slotIndex; // 0, 1, or 2

    public CPacketToggleBuffSlot(int pPlayerId, int pSlotIndex) {
        this.playerId = pPlayerId;
        this.slotIndex = pSlotIndex;
    }

    @Override
    public Type<CPacketToggleBuffSlot> type() {
        return TYPE;
    }

    /**
     * Handle the packet on the receiving side (server).
     */
    public static void handle(CPacketToggleBuffSlot pPacket, IPayloadContext pContext) {
        // Queue work to main thread
        pContext.enqueueWork(() -> {
            if (!(pContext.player() instanceof ServerPlayer player)) {
                return;
            }

            if (pPacket.slotIndex >= 0 && pPacket.slotIndex < 3) {
                // Toggle the buff slot on server side
                com.hero.sigil.buffs.HeroSigilData.activateSlot(player, pPacket.slotIndex);

                // Sync updated state to client
                BuffSlotSyncPacket syncPacket = new BuffSlotSyncPacket(
                    player.getId(),
                    com.hero.sigil.buffs.HeroSigilData.getBuffStatesArray(player)
                );
                HeroSigilNetworkManager.sendToPlayer(syncPacket, player);
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
