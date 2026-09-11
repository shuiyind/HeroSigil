package com.hero.sigil.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;

/**
 * Widget for displaying a buff slot in the Hero Sigil GUI.
 */
public class BuffSlotWidget extends AbstractButton {
    
    private final int slotId;
    private boolean unlocked = false;
    private boolean active = false;
    private net.minecraft.client.gui.components.Tooltip tooltip;

    public BuffSlotWidget(int pSlotId, int pX, int pY, int pWidth, int pHeight) {
        super(pX, pY, pWidth, pHeight, Component.empty());
        this.slotId = pSlotId;
    }

    @Override
    public void onClick(double pMouseX, double pMouseY) {
        if (this.unlocked && !this.active) {
            // Slot is unlocked but not active - activate it
            
            Minecraft.getInstance().getSoundSource().play(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK_PRESS, 
                net.minecraft.client.resources.sounds.SimpleSoundInstance.defaultVolume()
            );
            
            this.setActive(true);
            
            // Send packet to server to toggle buff slot
            if (Minecraft.getInstance().player != null) {
                com.hero.sigil.network.CPacketToggleBuffSlot packet = 
                    new com.hero.sigil.network.CPacketToggleBuffSlot(
                        Minecraft.getInstance().player.getId(),
                        this.slotId
                    );
                net.neoforged.neoforge.network.NetworkHooks.sendToServer(packet);
            }
        } else if (this.unlocked && this.active) {
            // Slot is active - deactivate it
            
            Minecraft.getInstance().getSoundSource().play(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK_RELEASE, 
                net.minecraft.client.resources.sounds.SimpleSoundInstance.defaultVolume()
            );
            
            this.setActive(false);
            
            // Send packet to server to toggle buff slot
            if (Minecraft.getInstance().player != null) {
                com.hero.sigil.network.CPacketToggleBuffSlot packet = 
                    new com.hero.sigil.network.CPacketToggleBuffSlot(
                        Minecraft.getInstance().player.getId(),
                        this.slotId
                    );
                net.neoforged.neoforge.network.NetworkHooks.sendToServer(packet);
            }
        }
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        
        // Draw background based on state
        if (this.unlocked) {
            if (this.active) {
                // Active state - highlight with gold color
                guiGraphics.fill(this.getX(), this.getY(), 
                    this.getX() + this.getWidth(), this.getY() + this.getHeight(), 
                    0x80FFD700); // Gold highlight for active slots
                
                // Draw inner glow effect
                guiGraphics.fill(this.getX() + 2, this.getY() + 2, 
                    this.getX() + this.getWidth() - 2, this.getY() + this.getHeight() - 2, 
                    0x40FFD700); // Lighter gold inner glow
            } else {
                // Unlocked but inactive - neutral state with subtle highlight
                guiGraphics.fill(this.getX(), this.getY(), 
                    this.getX() + this.getWidth(), this.getY() + this.getHeight(), 
                    0xFF606060); // Gray for unlocked but not active
                
                // Draw subtle border to indicate clickable
                guiGraphics.strokeLine(this.getX(), this.getY(), 
                    this.getX() + this.getWidth(), this.getY(), 
                    0x80AAAAAA);
            }
        } else {
            // Locked slot - darker appearance with X mark
            guiGraphics.fill(this.getX(), this.getY(), 
                this.getX() + this.getWidth(), this.getY() + this.getHeight(), 
                0xFF2A2A2A); // Dark gray for locked slots
            
            // Draw "X" to indicate locked
            int centerX = this.getX() + this.getWidth() / 2;
            int centerY = this.getY() + this.getHeight() / 2;
            int size = Math.min(this.getWidth(), this.getHeight()) / 4;
            
            guiGraphics.strokeLine(centerX - size, centerY - size, 
                centerX + size, centerY + size, 
                0xFF666666);
            guiGraphics.strokeLine(centerX + size, centerY - size, 
                centerX - size, centerY + size, 
                0xFF666666);
        }

        // Draw border around the slot
        int borderColor = this.unlocked ? 0xFFFFFFFF : 0xFF444444;
        guiGraphics.strokeLine(this.getX(), this.getY(), 
            this.getX() + this.getWidth(), this.getY(), 
            borderColor);
        guiGraphics.strokeLine(this.getX(), this.getY() + this.getHeight(), 
            this.getX() + this.getWidth(), this.getY() + this.getHeight(), 
            borderColor);
        guiGraphics.strokeLine(this.getX(), this.getY(), 
            this.getX(), this.getY() + this.getHeight(), 
            borderColor);
        guiGraphics.strokeLine(this.getX() + this.getWidth(), this.getY(), 
            this.getX() + this.getWidth(), this.getY() + this.getHeight(), 
            borderColor);

        // Draw buff icon if active and unlocked
        if (this.active && this.unlocked) {
            drawBuffIcon(guiGraphics, minecraft);
        } else if (!this.unlocked) {
            // Show locked indicator
            guiGraphics.drawString(minecraft.font, "🔒", 
                this.getX() + this.getWidth() / 2 - 4, 
                this.getY() + this.getHeight() / 2 - 6, 
                0xFF888888, false);
        }

        // Draw tooltip on hover
        if (this.isHovered()) {
            net.minecraft.client.gui.components.Tooltip tooltip = getTooltip();
            if (tooltip != null) {
                guiGraphics.renderTooltip(
                    minecraft.font, 
                    this.getTooltip().getVisualOrder(), 
                    pMouseX - this.getX(), 
                    pMouseY - this.getY()
                );
            }
        }
    }

    private void drawBuffIcon(GuiGraphics guiGraphics, Minecraft minecraft) {
        // Get the buff effect for this slot from HeroSigilData
        java.util.List<com.hero.sigil.buffs.BuffEffect> buffs = 
            com.hero.sigil.buffs.HeroSigilData.getAllBuffs();
        
        if (this.slotId >= 0 && this.slotId < Math.min(buffs.size(), 3)) {
            com.hero.sigil.buffs.BuffEffect buff = buffs.get(this.slotId);
            
            // Draw buff icon as a colored circle with the effect symbol
            int centerX = this.getX() + this.getWidth() / 2;
            int centerY = this.getY() + this.getHeight() / 2;
            int radius = Math.min(this.getWidth(), this.getHeight()) / 3 - 2;
            
            // Draw outer ring
            guiGraphics.fillCircle(centerX, centerY, radius + 1, 
                0xFF4682B4); // Steel blue for active buff
            
            // Draw inner circle with effect color based on slot type
            int fillColor = getBuffColor(this.slotId);
            guiGraphics.fillCircle(centerX, centerY, radius - 1, 
                fillColor);
            
            // Draw buff symbol (first letter of the effect name)
            String firstLetter = "";
            if (!minecraft.level().isClientSide()) {
                MobEffect effect = buff.getMobEffect();
                if (effect != null && effect.getDescriptionId() != null && effect.getDescriptionId().length() > 4) {
                    firstLetter = effect.getDescriptionId().substring(0, 1).toUpperCase(); // Get first letter of effect name
                }
            }
            
            guiGraphics.drawString(minecraft.font, 
                firstLetter.isEmpty() ? "?" : firstLetter, 
                centerX - minecraft.font.width(firstLetter) / 2, 
                centerY - minecraft.font.height("") / 2 + 1, 
                0xFFFFFFFF, false);
        }
    }

    private int getBuffColor(int slotId) {
        // Different colors for different buff types
        switch (slotId) {
            case 0: return 0xFF3CB371; // Green - Saturation/Health Boost
            case 1: return 0xFF4682B4; // Blue - Speed
            case 2: return 0xFFFF6347; // Red - Strength
            default: return 0xFFFFFFFF;
        }
    }

    @Override
    public net.minecraft.client.gui.components.Tooltip getTooltip() {
        if (this.tooltip == null) {
            Component baseComponent;
            
            if (!this.unlocked) {
                baseComponent = Component.translatable("tooltip.herosigil.slot.locked", this.slotId + 1);
            } else if (!this.active) {
                baseComponent = Component.translatable("tooltip.herosigil.slot.unlocked", this.slotId + 1);
            } else {
                // Get active buff name from HeroSigilData
                java.util.List<com.hero.sigil.buffs.BuffEffect> buffs = 
                    com.hero.sigil.buffs.HeroSigilData.getAllBuffs();
                
                String buffName = "";
                if (this.slotId >= 0 && this.slotId < Math.min(buffs.size(), 3)) {
                    com.hero.sigil.buffs.BuffEffect buff = buffs.get(this.slotId);
                    // Get the localized name from MobEffect's translation key
                    net.minecraft.world.effect.MobEffect effect = buff.getMobEffect();
                    if (effect != null) {
                        baseComponent = Component.translatable(effect.getDescriptionId());
                    } else {
                        baseComponent = Component.translatable("tooltip.herosigil.slot.active", 
                            this.slotId + 1, "Unknown Buff");
                    }
                } else {
                    baseComponent = Component.translatable("tooltip.herosigil.slot.active", 
                        this.slotId + 1, "Unknown Buff");
                }
            }
            
            this.tooltip = net.minecraft.client.gui.components.Tooltip.create(
                baseComponent.getVisualOrder(), 
                getTooltipLines()
            );
        }
        
        return this.tooltip;
    }

    private java.util.List<net.minecraft.network.chat.Component> getTooltipLines() {
        java.util.ArrayList<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
        
        if (!this.unlocked) {
            // Show achievement requirement for unlocked slots
            com.hero.sigil.achievement.AchievementTracker.AchievementType[] achievements = 
                com.hero.sigil.achievement.AchievementTracker.AchievementType.values();
            
            if (this.slotId < achievements.length) {
                com.hero.sigil.achievement.AchievementType achievement = achievements[this.slotId];
                lines.add(Component.translatable("tooltip.herosigil.requirement", 
                    Component.translatable(achievement.name().toLowerCase())));
            } else {
                lines.add(Component.translatable("tooltip.herosigil.click_to_select"));
            }
        } else if (!this.active) {
            lines.add(Component.translatable("tooltip.herosigil.click_to_activate"));
            
            // Show what buff will be activated
            java.util.List<com.hero.sigil.buffs.BuffEffect> buffs = 
                com.hero.sigil.buffs.HeroSigilData.getAllBuffs();
            
            if (this.slotId >= 0 && this.slotId < Math.min(buffs.size(), 3)) {
                com.hero.sigil.buffs.BuffEffect buff = buffs.get(this.slotId);
                net.minecraft.world.effect.MobEffect effect = buff.getMobEffect();
                if (effect != null) {
                    lines.add(Component.translatable(effect.getDescriptionId() + ".desc"));
                }
            }
        } else {
            // Show current active status and duration info
            java.util.List<com.hero.sigil.buffs.BuffEffect> buffs = 
                com.hero.sigil.buffs.HeroSigilData.getAllBuffs();
            
            if (this.slotId >= 0 && this.slotId < Math.min(buffs.size(), 3)) {
                com.hero.sigil.buffs.BuffEffect buff = buffs.get(this.slotId);
                
                lines.add(Component.translatable("tooltip.herosigil.active_status", 
                    Component.literal("✓")));
                
                if (!buff.isPermanent()) {
                    lines.add(Component.translatable("tooltip.herosigil.duration", buff.getDurationSeconds()));
                } else {
                    lines.add(Component.translatable("tooltip.herosigil.permanent"));
                }
            }
        }
        
        return lines;
    }

    @Override
    public net.minecraft.client.gui.components.Tooltip getNarration() {
        return super.getNarration();
    }

    /**
     * Set the unlocked state of this buff slot.
     */
    public void setUnlocked(boolean pUnlocked) {
        if (this.unlocked != pUnlocked) {
            this.unlocked = pUnlocked;
            // Reset tooltip cache when state changes
            this.tooltip = null;
        }
    }

    /**
     * Check if this buff slot is unlocked.
     */
    public boolean isUnlocked() {
        return this.unlocked;
    }

    /**
     * Set the active state of this buff slot.
     */
    public void setActive(boolean pActive) {
        if (this.active != pActive) {
            this.active = pActive;
            // Reset tooltip cache when state changes
            this.tooltip = null;
            
            // Play sound for visual feedback
            Minecraft minecraft = Minecraft.getInstance();
            if (pActive) {
                minecraft.getSoundSource().play(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK_PRESS, 
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.defaultVolume()
                );
            } else {
                minecraft.getSoundSource().play(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK_RELEASE, 
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.defaultVolume()
                );
            }
        }
    }

    /**
     * Check if this buff slot is currently active.
     */
    public boolean isActive() {
        return this.active;
    }

    /**
     * Update internal state (can be called to refresh UI).
     */
    public void updateState() {
        // Reset tooltip cache on state change
        if (this.tooltip != null) {
            this.tooltip = net.minecraft.client.gui.components.Tooltip.create(
                getTooltip().getVisualOrder(), 
                getTooltipLines()
            );
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
        // No special narration needed for this widget
    }
}
