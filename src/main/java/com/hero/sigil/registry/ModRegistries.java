package com.hero.sigil.registry;

import com.hero.sigil.HeroSigil;
import com.hero.sigil.gui.menu.HeroSigilMenu;
import com.hero.sigil.item.HeroSigilItem;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItems;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central registry for all Hero Sigil mod objects.
 */
public class ModRegistries {

    /**
     * Deferred Register for GUI Menus.
     */
    public static final DeferredRegister.Menus MENUS = 
        DeferredRegister.createMenus(HeroSigil.MODID);

    /**
     * The main Hero Sigil accessory item.
     * This is the core item that players will equip to unlock buff slots.
     */
    public static final DeferredItems.DeferredItem<HeroSigilItem> HERO_SIGIL = 
        HeroSigil.ITEMS.register("hero_sigil", HeroSigilItem::new);

    /**
     * GUI Menu type for the Hero Sigil inventory screen.
     */
    public static final DeferredHolder<MenuType<?>, MenuType<HeroSigilMenu>> HERO_SIGIL_MENU = 
        MENUS.register("herosigil_menu", () -> new MenuType<>(HeroSigilMenu.Factory::new));

    /**
     * Initialize all registries.
     * Called during mod initialization.
     */
    public static void init() {
        // Register menus to the event bus
        MENUS.register(HeroSigil.MODID);
        
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
