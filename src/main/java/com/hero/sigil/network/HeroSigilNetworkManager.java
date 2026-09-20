package com.hero.sigil.network;

import com.hero.sigil.HeroSigil;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Network manager for Hero Sigil mod.
 */
@EventBusSubscriber(modid = HeroSigil.MODID, bus = EventBusSubscriber.Bus.MOD)
public class HeroSigilNetworkManager {

    /**
     * Register all network handlers.
     */
    @SubscribeEvent
    public static void registerHandlers(RegisterPayloadHandlersEvent pEvent) {
        PayloadRegistrar registrar = pEvent.registrar("1").optional();

        // Register buff slot sync packet (bidirectional: server <-> client)
        registrar.playBidirectional(
            BuffSlotSyncPacket.TYPE,
            BuffSlotSyncPacket.STREAM_CODEC,
            BuffSlotSyncPacket::handle
        );

        // Register open GUI packet (server -> client)
        registrar.playToClient(
            CPacketOpenHeroSigilGUI.TYPE,
            CPacketOpenHeroSigilGUI.STREAM_CODEC,
            CPacketOpenHeroSigilGUI::handle
        );

        // Register toggle buff slot packet (client -> server)
        registrar.playToServer(
            CPacketToggleBuffSlot.TYPE,
            CPacketToggleBuffSlot.STREAM_CODEC,
            CPacketToggleBuffSlot::handle
        );
    }

    /**
     * Send a packet to a specific player.
     */
    public static <T extends CustomPacketPayload> void sendToPlayer(T packet, net.minecraft.server.level.ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    /**
     * Send buff slot sync packet to a specific player.
     */
    public static void sendBuffSlotSync(net.minecraft.world.entity.player.Player pPlayer) {
        if (pPlayer instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            BuffSlotSyncPacket packet = BuffSlotSyncPacket.createForPlayer(pPlayer);

            sendToPlayer(packet, serverPlayer);
        }
    }

    /**
     * Open the Hero Sigil GUI for a player.
     */
    public static void openHeroSigilGUI(net.minecraft.server.level.ServerPlayer pPlayer) {
        CPacketOpenHeroSigilGUI packet = new CPacketOpenHeroSigilGUI(pPlayer.getId());
        sendToPlayer(packet, pPlayer);
    }

}
