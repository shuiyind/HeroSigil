package com.hero.sigil.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet to open the Hero Sigil GUI from server to client.
 */
public class CPacketOpenHeroSigilGUI {

    private final int playerId;

    public CPacketOpenHeroSigilGUI(int pPlayerId) {
        this.playerId = pPlayerId;
    }

    /**
     * Encode packet data for network transmission.
     */
    public void toBytes(FriendlyByteBuf pBuffer) {
        pBuffer.writeInt(this.playerId);
    }

    /**
     * Decode packet data from network reception.
     */
    public static CPacketOpenHeroSigilGUI fromBytes(FriendlyByteBuf pBuffer) {
        int playerId = pBuffer.readInt();
        return new CPacketOpenHeroSigilGUI(playerId);
    }

    /**
     * Handle the packet on the receiving side (client).
     */
    public static void handle(CPacketOpenHeroSigilGUI pPacket, IPayloadContext pContext) {
        // Queue work to main thread
        pContext.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();

            if (minecraft.player != null && minecraft.player.getId() == pPacket.playerId) {
                // Open the Hero Sigil GUI screen on client side

                net.minecraft.client.gui.screens.Screen screen =
                    new com.hero.sigil.gui.screen.HeroSigilScreen(
                        new com.hero.sigil.gui.menu.HeroSigilMenu(0, minecraft.player.getInventory()),
                        minecraft.player.getInventory(),
                        net.minecraft.network.chat.Component.translatable("container.herosigil.title"));

                minecraft.setScreen(screen);
            }
        });
    }

    /**
     * Get the player ID for this packet.
     */
    public int getPlayerId() {
        return this.playerId;
    }
}
