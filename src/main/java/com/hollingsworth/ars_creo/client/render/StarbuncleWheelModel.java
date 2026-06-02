package com.hollingsworth.ars_creo.client.render;

import com.hollingsworth.ars_creo.ArsCreo;
import com.hollingsworth.ars_creo.common.block.StarbuncleWheelBlock;
import com.hollingsworth.ars_creo.common.block.StarbuncleWheelTile;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

@OnlyIn(Dist.CLIENT)
public class StarbuncleWheelModel extends GeoModel<StarbuncleWheelTile> {
    @Override
    public void setCustomAnimations(StarbuncleWheelTile entity, long uniqueID, AnimationState<StarbuncleWheelTile> customPredicate) {
        super.setCustomAnimations(entity, uniqueID, customPredicate);
        GeoBone wheel = this.getAnimationProcessor().getBone("wheel");
        Direction facing = entity.getBlockState().getValue(StarbuncleWheelBlock.FACING);
        float angle = ((AnimationTickHolder.getRenderTime(entity.getLevel()) * entity.getSpeed() * .3f) % 360) / 180 * (float) Math.PI;
        if (facing == Direction.SOUTH || facing == Direction.EAST)
            angle = -angle;
        wheel.setRotY(angle);
    }

    static final ResourceLocation model = ResourceLocation.fromNamespaceAndPath(ArsCreo.MODID, "geo/starbuncle_wheel.geo.json");
    static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(ArsCreo.MODID, "textures/block/starbuncle_wheel.png");
    static final ResourceLocation animations = ResourceLocation.fromNamespaceAndPath(ArsCreo.MODID, "animations/starbuncle_wheel_animation.json");

    @Override
    public ResourceLocation getModelResource(StarbuncleWheelTile object) {
        return model;
    }

    @Override
    public ResourceLocation getTextureResource(StarbuncleWheelTile object) {
        return texture;
    }

    @Override
    public ResourceLocation getAnimationResource(StarbuncleWheelTile animatable) {
        return animations;
    }

}