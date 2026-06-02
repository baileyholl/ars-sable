package com.hollingsworth.ars_creo.client.render;

import com.hollingsworth.ars_creo.common.block.StarbuncleWheelBlock;
import com.hollingsworth.ars_creo.common.block.StarbuncleWheelTile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

@OnlyIn(Dist.CLIENT)
public class StarbuncleWheelRenderer extends GeoBlockRenderer<StarbuncleWheelTile> {
    public static StarbuncleWheelModel wheelModel = new StarbuncleWheelModel();

    public StarbuncleWheelRenderer(BlockEntityRendererProvider.Context ignored) {
        super(wheelModel);
    }

    @Override
    public void actuallyRender(PoseStack stack, StarbuncleWheelTile animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        Direction facing = animatable.getBlockState().getValue(StarbuncleWheelBlock.FACING);
        stack.pushPose();
        if (facing != Direction.UP && facing != Direction.DOWN)
            stack.mulPose(Axis.YP.rotationDegrees(-90));
        super.actuallyRender(stack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        stack.popPose();
    }
}