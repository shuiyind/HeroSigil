package com.hero.sigil.client;

import com.hero.sigil.gui.menu.HeroSigilMenu;
import com.hero.sigil.gui.screen.HeroSigilScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Helper class for opening the Hero Sigil GUI.
 */
public class GuiOpenHelper {

    /**
     * Open the Hero Sigil GUI on the client side.
     * 
     * @param player The player who owns the GUI
     */
    public static void openHeroSigilGui(Player pPlayer) {
        Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        
        if (minecraft != null && minecraft.screen == null) {
            // Create and open the screen
            com.hero.sigil.gui.menu.HeroSigilMenu menu = new com.hero.sigil.gui.menu.HeroSigilMenu(
                0, 
                pPlayer.getInventory(), 
                net.minecraft.world.item.ItemStack.EMPTY
            );
            
            minecraft.setScreen(new com.hero.sigil.gui.screen.HeroSigilScreen(
                menu, 
                pPlayer.getInventory(), 
                Component.translatable("container.herosigil.title")
            ));
        }
    }

    /**
     * Send a GUI open request to the server.
     */
    public static void sendGuiOpenRequest(net.minecraft.server.level.ServerPlayer pServerPlayer) {
        // TODO: Implement packet-based GUI opening if needed
    }
}
