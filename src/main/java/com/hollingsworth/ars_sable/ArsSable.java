package com.hollingsworth.ars_sable;

import com.hollingsworth.ars_sable.common.SableSublevelObserver;
import com.hollingsworth.ars_sable.common.command.MiniatureSublevelCommand;
import com.hollingsworth.ars_sable.common.datagen.LangDatagen;
import com.hollingsworth.ars_sable.common.registry.CreativeTabRegistry;
import com.hollingsworth.ars_sable.common.registry.DataComponentRegistry;
import com.hollingsworth.ars_sable.common.registry.ModBlockRegistry;
import com.hollingsworth.ars_sable.common.spell.EffectMiniaturize;
import com.hollingsworth.ars_sable.network.ACNetworking;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import dev.ryanhcode.sable.neoforge.event.ForgeSableSubLevelContainerReadyEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.RegisterEvent;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ArsSable.MODID)
public class ArsSable {
    public static final String MODID = "ars_sable";

    public ArsSable(IEventBus modBus, ModContainer modContainer) {
        GlyphRegistry.registerSpell(EffectMiniaturize.INSTANCE);
        modContainer.registerConfig(ModConfig.Type.COMMON, ArsSableConfig.SERVER_CONFIG);
        modBus.addListener(ACNetworking::register);
        modBus.addListener(ArsSable::registerEvents);
        modBus.addListener(ArsSable::registerCapability);
        modBus.addListener(ArsSable::commonSetup);
        modBus.addListener(LangDatagen::gather);
        NeoForge.EVENT_BUS.addListener(ArsSable::onSublevelReady);
        NeoForge.EVENT_BUS.addListener(MiniatureSublevelCommand::register);
        registers(modBus);
    }

    public static void registers(IEventBus event) {
        ModBlockRegistry.ITEMS.register(event);
        ModBlockRegistry.BLOCK_REG.register(event);
        ModBlockRegistry.BLOCK_ENTITY_REG.register(event);
        CreativeTabRegistry.TABS.register(event);
        DataComponentRegistry.DATA.register(event);
    }

    public static void onSublevelReady(ForgeSableSubLevelContainerReadyEvent ready){
        ready.getContainer().addObserver(new SableSublevelObserver());
    }

    public static void registerEvents(RegisterEvent event) {
        event.register(Registries.ITEM, helper -> ModBlockRegistry.onBlockItemsRegistry());
    }

    public static void commonSetup(FMLCommonSetupEvent event) {
    }

    public static void registerCapability(RegisterCapabilitiesEvent event) {
    }

    public static ResourceLocation prefix(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}