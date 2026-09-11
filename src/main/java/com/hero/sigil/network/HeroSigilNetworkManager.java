package com.hero.sigil.network;

import com.hero.sigil.HeroSigil;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Network manager for Hero Sigil mod.
 */
@EventBusSubscriber(modid = HeroSigil.MODID, bus = EventBusSubscriber.Bus.GAME)
public class HeroSigilNetworkManager {
    
    /**
     * Register all network handlers.
     */
    @SubscribeEvent
    public static void registerHandlers(RegisterPayloadHandlersEvent pEvent) {
        PayloadRegistrar registrar = pEvent.registrar(HeroSigil.MODID)
            .versionedOnly()
            .optional();
            
        // Register buff slot sync packet (bidirectional: server <-> client)
        registrar.playBidirectional(
            com.hero.sigil.network.BuffSlotSyncPacket.TYPE_ID,
            com.hero.sigil.network.BuffSlotSyncPacket::fromBytes,
            com.hero.sigil.network.BuffSlotSyncPacket::handle
        );
        
        // Register open GUI packet (server -> client)
        registrar.playToClient(
            "open_herosigil_gui",
            com.hero.sigil.network.CPacketOpenHeroSigilGUI::fromBytes,
            com.hero.sigil.network.CPacketOpenHeroSigilGUI::handle
        );
        
        // Register toggle buff slot packet (client -> server)
        registrar.playToServer(
            "toggle_buff_slot",
            com.hero.sigil.network.CPacketToggleBuffSlot::fromBytes,
            com.hero.sigil.network.CPacketToggleBuffSlot::handle
        );
    }

    /**
     * Send a packet to a specific player.
     */
    public static <T> void sendToPlayer(T packet, net.minecraft.server.level.ServerPlayer player) {
        net.neoforged.neoforge.network.NetworkHooks.sendTo(packet, player.connection.connection, player.getDirection());
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
