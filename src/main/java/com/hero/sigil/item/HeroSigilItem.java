package com.hero.sigil.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnBlockContext;

/**
 * Hero Sigil - The main accessory item that provides buff slots.
 *
 * This item will be equipped via Curios API in a custom slot.
 * Each unlocked buff slot grants a specific positive effect to the player.
 */
public class HeroSigilItem extends Item {

    // Tick counter for periodic buff refresh (every 5 seconds = 100 ticks)
    private static final int BUFF_REFRESH_INTERVAL = 100;

    public HeroSigilItem() {
        super(new Properties()
            .stacksPerStack(1)  // Only one can be equipped at a time
            .durability(0)      // No durability by default (can be added later)
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, Player player, InteractionHand usedHand) {
        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            // Server-side: Open GUI to manage buff slots

            // Sync current buff states with achievements
            com.hero.sigil.buffs.HeroSigilData.syncFromAchievements(player);

            // Send updated buff states to client and open GUI
            com.hero.sigil.network.BuffSlotSyncPacket syncPacket = new com.hero.sigil.network.BuffSlotSyncPacket(
                player.getId(),
                com.hero.sigil.buffs.HeroSigilData.getBuffStatesArray(player)
            );

            // Send buff states first, then open GUI
            com.hero.sigil.network.HeroSigilNetworkManager.sendToPlayer(syncPacket, serverPlayer);

            // Open the GUI on client side
            com.hero.sigil.network.CPacketOpenHeroSigilGUI guiPacket =
                new com.hero.sigil.network.CPacketOpenHeroSigilGUI(player.getId());
            com.hero.sigil.network.HeroSigilNetworkManager.sendToPlayer(guiPacket, serverPlayer);
        }
        return super.use(level, player, usedHand);
    }

    /**
     * Called when the item is equipped in a Curios slot.
     * This method can be overridden to apply passive effects.
     */
    @Override
    public void onEquip(ItemStack stack, LivingEntity entity) {
        super.onEquip(stack, entity);

        // Apply all active buffs if this is a player
        if (entity instanceof Player player && !player.level().isClientSide()) {
            com.hero.sigil.buffs.HeroSigilData.applyAllBuffs(player);

            // Sync buff states to client
            com.hero.sigil.network.BuffSlotSyncPacket packet = new com.hero.sigil.network.BuffSlotSyncPacket(
                player.getId(),
                com.hero.sigil.buffs.HeroSigilData.getBuffStatesArray(player)
            );
            com.hero.sigil.network.HeroSigilNetworkManager.sendToPlayer(packet, (net.minecraft.server.level.ServerPlayer) player);
        }
    }

    /**
     * Called when the item is unequipped from a Curios slot.
     */
    @Override
    public void onRemove(ItemStack stack, LivingEntity entity) {
        super.onRemove(stack, entity);

        // Remove all active buffs if this is a player
        if (entity instanceof Player player && !player.level().isClientSide()) {
            com.hero.sigil.buffs.HeroSigilData.reset(player);
        }
    }

    /**
     * 对每个装备的玩家进行 tick 更新，管理 buff 刷新
     */
    public static void onPlayerTick(Player player) {
        if (player.level().isClientSide() || !(player instanceof net.minecraft.server.level.ServerPlayer)) {
            return;
        }

        // Check if player has a Hero Sigil equipped
        ItemStack sigilStack = getEquippedSigil(player);
        if (sigilStack.isEmpty()) {
            return;
        }

        // 对每个激活的 buff 独立刷新
        java.util.List<com.hero.sigil.buffs.BuffEffect> buffs = com.hero.sigil.buffs.HeroSigilData.getAllBuffs();
        boolean needsSync = false;

        for (com.hero.sigil.buffs.BuffEffect buff : buffs) {
            if (buff.isActive() && buff.isUnlocked()) {
                buff.applyBuff(player);
                if (buff.getDurationSeconds() > 0) { // 非永久 buff
                    needsSync = true;
                }
            }
        }

        // 仅在必要时同步状态
        if (needsSync) {
            com.hero.sigil.network.BuffSlotSyncPacket packet = new com.hero.sigil.network.BuffSlotSyncPacket(
                player.getId(),
                com.hero.sigil.buffs.HeroSigilData.getBuffStatesArray(player)
            );
            com.hero.sigil.network.HeroSigilNetworkManager.sendToPlayer(packet, (net.minecraft.server.level.ServerPlayer) player);
        }
    }

    /**
     * Get the equipped Hero Sigil ItemStack for a player.
     */
    private static ItemStack getEquippedSigil(Player player) {
        // Check Curios API slots if available
        try {
            top.theillusivec4.curios.api.CuriosApi curiosApi = top.theillusivec4.curios.api.CuriosAPI.getCuriosInventory(player).orElse(null);
            if (curiosApi != null) {
                ItemStack sigil = curiosApi.getItemStacksHandler().getStackInSlot("hero_sigil", 0);
                if (!sigil.isEmpty() && sigil.getItem() instanceof HeroSigilItem) {
                    return sigil;
                }
            }
        } catch (Exception e) {
            // Curios API not available, continue with fallback check
        }

        // Fallback: Check main inventory for testing purposes
        return ItemStack.EMPTY;
    }

    /**
     * Check if this item can be equipped in a Curios slot.
     */
    @Override
    public boolean canEquipFromTrade(ItemStack stack, net.minecraft.world.entity.Entity target) {
        return true;  // Can be obtained from villager trades
    }

    /**
     * Define the equipment slot type for this item.
     * For Curios compatibility, this will be handled by the API.
     */
    @Override
    public EquipmentSlot getEquipmentType(ItemStack stack) {
        return EquipmentSlot.CHEST;  // Default fallback, Curios will override
    }

    /**
     * Check if the item can be equipped in the given slot type.
     */
    @Override
    public boolean canEquipFromItemStack(ItemStack stack, net.minecraft.world.entity.EquipmentSlot slot) {
        return super.canEquipFromItemStack(stack, slot);
    }

    /**
     * Check if this item is compatible with Curios API.
     * Returns true to allow Curios to manage equipping/unequipping.
     */
    @Override
    public boolean canEquip(ItemStack stack, net.minecraft.world.entity.EquipmentSlot slot,
                           LivingEntity entity) {
        return true;  // Allow Curios API integration
    }

    /**
     * Called when the item is used on a block.
     */
    @Override
    public InteractionResult useOn(UseOnBlockContext context) {
        Player player = context.getPlayer();
        if (player != null && !player.level().isClientSide()) {
            // Open GUI when right-clicking with the item
            com.hero.sigil.buffs.HeroSigilData.syncFromAchievements(player);

            // Send updated buff states to client and open GUI
            com.hero.sigil.network.BuffSlotSyncPacket syncPacket = new com.hero.sigil.network.BuffSlotSyncPacket(
                player.getId(),
                com.hero.sigil.buffs.HeroSigilData.getBuffStatesArray(player)
            );

            // Send buff states first, then open GUI
            com.hero.sigil.network.HeroSigilNetworkManager.sendToPlayer(syncPacket, (net.minecraft.server.level.ServerPlayer) player);

            // Open the GUI on client side
            com.hero.sigil.network.CPacketOpenHeroSigilGUI guiPacket =
                new com.hero.sigil.network.CPacketOpenHeroSigilGUI(player.getId());
            com.hero.sigil.network.HeroSigilNetworkManager.sendToPlayer(guiPacket, (net.minecraft.server.level.ServerPlayer) player);
        }
        return super.useOn(context);
    }

    /**
     * Get the display name for tooltips.
     */
    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.core.Holder<Item> holder,
                                net.minecraft.network.chat.Component.TooltipFlag flag,
                                java.util.List<net.minecraft.network.chat.Component> tooltip) {
        // Add tooltip text showing unlocked buffs

        // Show current buff status if player has data
        net.minecraft.world.entity.player.Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null && !player.level().isClientSide()) {
            int[] buffs = com.hero.sigil.buffs.HeroSigilData.getBuffStatesArray(player);
            java.util.List<com.hero.sigil.buffs.BuffEffect> allBuffs =
                com.hero.sigil.buffs.HeroSigilData.getAllBuffs();

            for (int i = 0; i < Math.min(buffs.length, allBuffs.size()); i++) {
                boolean unlocked = (buffs[i] & 0x1) != 0; // Bit 0: unlocked
                boolean active = (buffs[i] & 0x2) != 0;   // Bit 1: active

                String statusText = unlocked ?
                    (active ? "✓ Active" : "○ Unlocked") : "✗ Locked";

                tooltip.add(net.minecraft.network.chat.Component.literal(
                    "Slot " + (i + 1) + ": " + statusText));
            }
        }

        super.appendHoverText(stack, holder, flag, tooltip);
    }

    /**
     * Check if the item can be repaired with specific materials.
     */
    @Override
    public boolean isRepairable(ItemStack stack) {
        return true;  // Can be repaired (if durability is added later)
    }

}
