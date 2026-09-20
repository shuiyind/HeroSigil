package com.hero.sigil.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet to open the Hero Sigil GUI from server to client.
 */
public class CPacketOpenHeroSigilGUI implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CPacketOpenHeroSigilGUI> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("herosigil", "open_herosigil_gui"));

    public static final StreamCodec<FriendlyByteBuf, CPacketOpenHeroSigilGUI> STREAM_CODEC = StreamCodec.of(
        (FriendlyByteBuf buf, CPacketOpenHeroSigilGUI packet) -> buf.writeInt(packet.playerId),
        buf -> new CPacketOpenHeroSigilGUI(buf.readInt())
    );

    private final int playerId;

    public CPacketOpenHeroSigilGUI(int pPlayerId) {
        this.playerId = pPlayerId;
    }

    @Override
    public Type<CPacketOpenHeroSigilGUI> type() {
        return TYPE;
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
