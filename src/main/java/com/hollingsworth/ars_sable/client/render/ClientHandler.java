package com.hollingsworth.ars_sable.client.render;

import com.hollingsworth.ars_sable.ArsSable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@Mod(value = ArsSable.MODID, dist = Dist.CLIENT)
public class ClientHandler {
    public ClientHandler(IEventBus bus) {
        bus.addListener(ClientHandler::registerRenderers);
        bus.addListener(ClientHandler::registerClientExtensions);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {

    }

    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {

    }
}