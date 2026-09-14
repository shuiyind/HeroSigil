package com.hero.sigil;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(HeroSigil.MODID)
public class HeroSigil {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "herosigil";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // Deferred Register for Items (including the Hero Sigil accessory)
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    // Creative Mode Tab for the mod
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_MODE_TAB = CREATIVE_MODE_TABS.register(
        "herosigil_tab", 
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.herosigil"))
            .icon(() -> ModRegistries.HERO_SIGIL.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ModRegistries.HERO_SIGIL.get());
            })
            .build()
    );

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public HeroSigil(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        
        // Register sound events to the mod event bus
        ModRegistries.SOUND_EVENTS.register(modEventBus);

        // Initialize all registries
        ModRegistries.init();

        // Register ourselves for server and other game events we are interested in.
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // Initialization task is called when the mod is being initialized
        modEventBus.addListener(this::commonSetup);

        // Register network handlers
        modEventBus.addListener(com.hero.sigil.network.HeroSigilNetworkManager::registerHandlers);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HERO SIGIL MOD LOADED SUCCESSFULLY!");
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting - Hero Sigil");
    }
}
