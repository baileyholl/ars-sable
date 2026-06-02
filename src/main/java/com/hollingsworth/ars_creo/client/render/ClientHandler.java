package com.hollingsworth.ars_creo.client.render;

import com.hollingsworth.ars_creo.ArsCreo;
import com.hollingsworth.ars_creo.common.registry.ModBlockRegistry;
import com.hollingsworth.arsnouveau.common.items.AnimBlockItem;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

@Mod(value = ArsCreo.MODID, dist = Dist.CLIENT)
public class ClientHandler {
    public ClientHandler(IEventBus bus) {
        bus.addListener(ClientHandler::registerRenderers);
        bus.addListener(ClientHandler::registerClientExtensions);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockRegistry.STARBY_TILE.get(), StarbuncleWheelRenderer::new);
    }

    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return new GeoItemRenderer<AnimBlockItem>(new GeoModel<>() {
                    @Override
                    public void setCustomAnimations(AnimBlockItem item, long uniqueID, AnimationState<AnimBlockItem> customPredicate) {
                        super.setCustomAnimations(item, uniqueID, customPredicate);
                        getAnimationProcessor().getBone("wheel").setRotY(0);
                    }

                    @Override
                    public ResourceLocation getModelResource(AnimBlockItem animatable) {
                        return StarbuncleWheelModel.model;
                    }

                    @Override
                    public ResourceLocation getTextureResource(AnimBlockItem animatable) {
                        return StarbuncleWheelModel.texture;
                    }

                    @Override
                    public ResourceLocation getAnimationResource(AnimBlockItem animatable) {
                        return StarbuncleWheelModel.animations;
                    }
                });
            }
        }, ModBlockRegistry.STARBY_WHEEL_ITEM);
    }
}