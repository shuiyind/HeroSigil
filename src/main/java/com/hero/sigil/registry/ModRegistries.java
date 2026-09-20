package com.hero.sigil.registry;

import com.hero.sigil.HeroSigil;
import com.hero.sigil.gui.menu.HeroSigilMenu;
import com.hero.sigil.item.HeroSigilItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;


/**
 * Central registry for all Hero Sigil mod objects.
 */
public class ModRegistries {

    /**
     * Deferred Register for Sound Events.
     */
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(Registries.SOUND_EVENT, HeroSigil.MODID);

    /**
     * Buff 音效注册
     */
    public static final DeferredHolder<SoundEvent, SoundEvent> BUFF_UNLOCK = SOUND_EVENTS.register(
        "buff_unlock",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HeroSigil.MODID, "buff_unlock"))
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> BUFF_ACTIVATE = SOUND_EVENTS.register(
        "buff_activate",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HeroSigil.MODID, "buff_activate"))
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> BUFF_DEACTIVATE = SOUND_EVENTS.register(
        "buff_deactivate",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HeroSigil.MODID, "buff_deactivate"))
    );

    /**
     * Deferred Register for GUI Menus.
     */
    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(Registries.MENU, HeroSigil.MODID);

    /**
     * The main Hero Sigil accessory item.
     * This is the core item that players will equip to unlock buff slots.
     */
    public static final DeferredHolder<Item, HeroSigilItem> HERO_SIGIL =
        HeroSigil.ITEMS.register("hero_sigil", HeroSigilItem::new);

    /**
     * GUI Menu type for the Hero Sigil inventory screen.
     */
    public static final DeferredHolder<MenuType<?>, MenuType<HeroSigilMenu>> HERO_SIGIL_MENU =
        MENUS.register("herosigil_menu", () -> new MenuType<HeroSigilMenu>(new HeroSigilMenu.Factory(), FeatureFlags.VANILLA_SET));

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
