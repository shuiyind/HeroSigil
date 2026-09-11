package com.hero.sigil.gui.menu;

import com.hero.sigil.HeroSigil;
import com.hero.sigil.registry.ModRegistries;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/**
 * Container menu for Hero Sigil GUI.
 * 
 * Manages the item slots and data synchronization between client and server.
 */
public class HeroSigilMenu extends AbstractContainerMenu {
    
    private final ItemStack sigilStack;
    private final Player player;
    
    // Buff slot indices (will be expanded later)
    public static final int BUFF_SLOT_1 = 0;
    public static final int BUFF_SLOT_2 = 1;
    public static final int BUFF_SLOT_3 = 2;

    public HeroSigilMenu(int pContainerId, Inventory pPlayerInventory) {
        this(pContainerId, pPlayerInventory, ItemStack.EMPTY);
    }

    public HeroSigilMenu(int pContainerId, Inventory pPlayerInventory, FriendlyByteBuf data) {
        this(pContainerId, pPlayerInventory, data.readItem());
    }

    public HeroSigilMenu(int pContainerId, Inventory pPlayerInventory, ItemStack pSigilStack) {
        super(ModRegistries.HERO_SIGIL_MENU.get(), pContainerId);
        
        this.sigilStack = pSigilStack;
        this.player = pPlayerInventory.player;
        
        // Setup player inventory slots (standard Minecraft layout)
        int playerInvStartX = 8;
        int playerInvStartY = this.imageHeight - 96 + 4;
        
        // Main inventory rows
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new net.minecraft.world.inventory.Slot(pPlayerInventory, j + i * 9 + 9, 
                    playerInvStartX + j * 18, playerInvStartY + i * 18));
            }
        }
        
        // Hotbar row
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new net.minecraft.world.inventory.Slot(pPlayerInventory, i, 
                playerInvStartX + i * 18, playerInvStartY + 58));
        }
    }

    /**
     * Check if the player has the Hero Sigil equipped.
     */
    public boolean hasSigilEquipped() {
        return !this.sigilStack.isEmpty() && 
               this.sigilStack.getItem() == ModRegistries.HERO_SIGIL.get();
    }

    /**
     * Get the equipped Hero Sigil stack.
     */
    public ItemStack getSigilStack() {
        return this.sigilStack;
    }

    /**
     * Check if the player can interact with this menu.
     */
    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player pPlayer) {
        return stillValid(this, pPlayer, ModRegistries.HERO_SIGIL_MENU.get());
    }

    /**
     * Called when an item is clicked in the inventory.
     */
    @Override
    public ItemStack quickCraft(int pSlotId, int pMouseButton, int pClickTypeCount, net.minecraft.world.entity.player.Player pPlayer) {
        return super.quickCraft(pSlotId, pMouseButton, pClickTypeCount, pPlayer);
    }

    /**
     * Called when an item is dropped from the inventory.
     */
    @Override
    public ItemStack quickMoveScale(net.minecraft.world.entity.player.Player pPlayer, int pIndex) {
        return super.quickMove(pPlayer, pIndex);
    }

    /**
     * Get buff slot 1 state (unlocked/active).
     */
    public boolean isBuffSlotUnlocked(int slotId) {
        // TODO: Read from NBT data or achievement tracker
        switch (slotId) {
            case BUFF_SLOT_1: return this.sigilStack.getOrCreateTag().getInt("buff_slot_1_unlocked") == 1;
            case BUFF_SLOT_2: return this.sigilStack.getOrCreateTag().getInt("buff_slot_2_unlocked") == 1;
            case BUFF_SLOT_3: return this.sigilStack.getOrCreateTag().getInt("buff_slot_3_unlocked") == 1;
            default: return false;
        }
    }

    /**
     * Get the current active buff in a slot.
     */
    public int getActiveBuff(int slotId) {
        // TODO: Read from NBT data
        switch (slotId) {
            case BUFF_SLOT_1: return this.sigilStack.getOrCreateTag().getInt("buff_slot_1_active");
            case BUFF_SLOT_2: return this.sigilStack.getOrCreateTag().getInt("buff_slot_2_active");
            case BUFF_SLOT_3: return this.sigilStack.getOrCreateTag().getInt("buff_slot_3_active");
            default: return 0;
        }
    }

    /**
     * Factory method to create menu type.
     */
    public static class Factory implements net.minecraft.world.inventory.MenuType.FriendlyByteBufFactory<HeroSigilMenu> {
        @Override
        public HeroSigilMenu create(int pContainerId, Inventory pInventory, FriendlyByteBuf pBuffer) {
            return new HeroSigilMenu(pContainerId, pInventory, pBuffer);
        }
    }
}
