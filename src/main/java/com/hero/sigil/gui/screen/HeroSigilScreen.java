package com.hero.sigil.gui.screen;

import com.hero.sigil.HeroSigil;
import com.hero.sigil.buffs.BuffEffect;
import com.hero.sigil.gui.menu.HeroSigilMenu;
import com.hero.sigil.gui.widget.BuffSlotWidget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Main GUI screen for managing Hero Sigil buff slots.
 */
public class HeroSigilScreen extends AbstractContainerScreen<HeroSigilMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        HeroSigil.MODID, "textures/gui/herosigil_gui.png"
    );

    // UI components
    public BuffSlotWidget buffSlot1;
    public BuffSlotWidget buffSlot2;
    public BuffSlotWidget buffSlot3;

    private Button closeButton;
    private Component titleText = Component.translatable("container.herosigil.title");

    public HeroSigilScreen(HeroSigilMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.imageWidth = 256;
        this.imageHeight = 180;
    }

    @Override
    protected void init() {
        super.init();

        // Center the screen
        this.inventoryLabelY = this.imageHeight - 96 + 4;
        this.titleLabelX = (this.width - this.font.width(titleText)) / 2;

        // Create buff slot widgets (will be positioned in resize)
        createBuffSlotWidgets();

        // Add close button
        closeButton = Button.builder(
            Component.translatable("gui.herosigil.close"),
            button -> Minecraft.getInstance().setScreen(null)
        )
        .bounds(this.width / 2 - 50, this.height - 30, 100, 20)
        .build();

        this.addRenderableWidget(closeButton);
    }

    private void createBuffSlotWidgets() {
        // Create buff slot widgets for display
        int startX = this.width / 2 - 67;
        int startY = 50;
        int spacingX = 45;

        buffSlot1 = new BuffSlotWidget(0, startX, startY, 32, 32);
        this.addRenderableWidget(buffSlot1);

        buffSlot2 = new BuffSlotWidget(1, startX + spacingX, startY, 32, 32);
        this.addRenderableWidget(buffSlot2);

        buffSlot3 = new BuffSlotWidget(2, startX + spacingX * 2, startY, 32, 32);
        this.addRenderableWidget(buffSlot3);
    }

    @Override
    public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
        super.resize(pMinecraft, pWidth, pHeight);

        // Reposition widgets based on new screen size
        int startX = this.width / 2 - 67;
        int startY = 50;
        int spacingX = 45;

        if (buffSlot1 != null) {
            buffSlot1.setPosition(startX, startY);
        }
        if (buffSlot2 != null) {
            buffSlot2.setPosition(startX + spacingX, startY);
        }
        if (buffSlot3 != null) {
            buffSlot3.setPosition(startX + spacingX * 2, startY);
        }

        // Reposition close button
        if (closeButton != null) {
            closeButton.setPosition(this.width / 2 - 50, this.height - 30);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTickTime, int pMouseX, int pMouseY) {
        // Draw background texture
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTickTime) {
        super.render(guiGraphics, pMouseX, pMouseY, pPartialTickTime);

        // Draw title at top center
        int x = (this.width - this.font.width(titleText)) / 2;
        guiGraphics.drawString(this.font, titleText, x, 10, 0xFFFFFF, false);
    }

    @Override
    public void containerTick() {
        super.containerTick();

        // Update buff slots state each tick to reflect server changes
        updateBuffSlotsState();
    }

    private void updateBuffSlotsState() {
        // Get the equipped Hero Sigil stack from player inventory
        ItemStack sigilStack = getMenu().getSigilStack();

        if (!sigilStack.isEmpty()) {
            // Update buff slots based on unlocked achievements and active states
            int[] buffs = com.hero.sigil.buffs.HeroSigilData.getBuffStatesArray(
                Minecraft.getInstance().player);

            // Get the list of available buffs from HeroSigilData
            java.util.List<BuffEffect> allBuffs = com.hero.sigil.buffs.HeroSigilData.getAllBuffs();

            if (buffSlot1 != null && allBuffs.size() > 0) {
                BuffEffect buff1 = allBuffs.get(0);
                buffSlot1.setUnlocked(buff1.isUnlocked());
                // Check bit 1 for active state: 0x2 = active, 0x1 = unlocked only
                boolean isActive = (buffs.length > 0) && ((buffs[0] & 0x2) != 0);
                buffSlot1.setActive(isActive);
            }

            if (buffSlot2 != null && allBuffs.size() > 1) {
                BuffEffect buff2 = allBuffs.get(1);
                buffSlot2.setUnlocked(buff2.isUnlocked());
                boolean isActive = (buffs.length > 1) && ((buffs[1] & 0x2) != 0);
                buffSlot2.setActive(isActive);
            }

            if (buffSlot3 != null && allBuffs.size() > 2) {
                BuffEffect buff3 = allBuffs.get(2);
                buffSlot3.setUnlocked(buff3.isUnlocked());
                boolean isActive = (buffs.length > 2) && ((buffs[2] & 0x2) != 0);
                buffSlot3.setActive(isActive);
            }
        } else {
            // No Hero Sigil equipped - all slots locked
            if (buffSlot1 != null) buffSlot1.setUnlocked(false);
            if (buffSlot2 != null) buffSlot2.setUnlocked(false);
            if (buffSlot3 != null) buffSlot3.setUnlocked(false);
        }
    }

    /**
     * Get the Hero Sigil menu for this screen.
     */
    @Override
    public HeroSigilMenu getMenu() {
        return this.menu;
    }
}
