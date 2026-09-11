package com.hero.sigil.registry;

import com.hero.sigil.HeroSigil;
import com.hero.sigil.item.HeroSigilItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Central registry for all Hero Sigil mod objects.
 */
public class ModRegistries {

    /**
     * The main Hero Sigil accessory item.
     * This is the core item that players will equip to unlock buff slots.
     */
    public static final DeferredItem<HeroSigilItem> HERO_SIGIL = 
        HeroSigil.ITEMS.register("hero_sigil", HeroSigilItem::new);

    /**
     * Initialize all registries.
     * Called during mod initialization.
     */
    public static void init() {
        // All registrations happen automatically via DeferredRegister
        HeroSigil.LOGGER.info("Hero Sigil registries initialized");
    }

    /**
     * Get the registered Hero Sigil item.
     */
    public static Item getHeroSigilItem() {
        return HERO_SIGIL.get();
    }
}
