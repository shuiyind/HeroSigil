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

    public HeroSigilItem() {
        super(new Properties()
            .stacksPerStack(1)  // Only one can be equipped at a time
            .durability(0)      // No durability by default (can be added later)
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, Player player, InteractionHand usedHand) {
        if (!level.isClientSide) {
            // Server-side: Open GUI to manage buff slots
            // TODO: Implement GUI opening logic
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
        // TODO: Apply base passives when equipped (e.g., slight XP boost)
    }

    /**
     * Called when the item is unequipped from a Curios slot.
     */
    @Override
    public void onRemove(ItemStack stack, LivingEntity entity) {
        super.onRemove(stack, entity);
        // TODO: Remove passives when unequipped
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
        // TODO: Implement right-click behavior (e.g., open GUI, activate ability)
        return super.useOn(context);
    }

    /**
     * Get the display name for tooltips.
     */
    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.core.Holder<Lnet.minecraft.world.item.Item>; net.minecraft.network.chat.Component.TooltipFlag flag) {
        // TODO: Add tooltip text showing unlocked buffs
        super.appendHoverText(stack, p_41429_, p_41430_, p_41431_);
    }

    /**
     * Check if the item can be repaired with specific materials.
     */
    @Override
    public boolean isRepairable(ItemStack stack) {
        return true;  // Can be repaired (if durability is added later)
    }
}
