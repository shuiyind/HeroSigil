package com.hero.sigil.registry;

import com.hero.sigil.HeroSigil;
import com.hero.sigil.gui.menu.HeroSigilMenu;
import com.hero.sigil.item.HeroSigilItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItems;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Central registry for all Hero Sigil mod objects.
 */
public class ModRegistries {

    /**
     * Deferred Register for Sound Events.
     */
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = 
        DeferredRegister.create(NeoForgeRegistries.Keys.SOUND_EVENTS, HeroSigil.MODID);

    /**
     * Buff 音效注册
     */
    public static final DeferredHolder<SoundEvent, SoundEvent> BUFF_UNLOCK = SOUND_EVENTS.register(
        "buff_unlock",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HeroSigil.MODID, "buff_unlock"))
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> BUFF_ACTIVATE = SOUND_EVENTS.register(
        "buff_activate",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HeroSigil.MODID, "buff_activate"))
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> BUFF_DEACTIVATE = SOUND_EVENTS.register(
        "buff_deactivate",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HeroSigil.MODID, "buff_deactivate"))
    );

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
