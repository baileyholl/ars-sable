package com.hollingsworth.ars_sable.client.render;

import com.hollingsworth.ars_sable.ArsSable;
import com.hollingsworth.ars_sable.common.registry.ModBlockRegistry;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.jetbrains.annotations.NotNull;

@Mod(value = ArsSable.MODID, dist = Dist.CLIENT)
public class ClientHandler {
    public ClientHandler(IEventBus bus) {
        bus.addListener(ClientHandler::registerRenderers);
        bus.addListener(ClientHandler::registerClientExtensions);
        NeoForge.EVENT_BUS.addListener(ClientHandler::onLevelUnload);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {

    }

    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new MiniatureSublevelItemRenderer();
                }
                return renderer;
            }
        }, ModBlockRegistry.MINIATURE_SUBLEVEL.get());
    }

    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            MiniatureSublevelItemRenderer.clear();
        }
    }
}
