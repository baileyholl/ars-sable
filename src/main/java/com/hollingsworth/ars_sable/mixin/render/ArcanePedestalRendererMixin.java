package com.hollingsworth.ars_sable.mixin.render;

import com.hollingsworth.ars_sable.client.render.SublevelItemRenderHelper;
import com.hollingsworth.arsnouveau.client.renderer.tile.ArcanePedestalRenderer;
import com.hollingsworth.arsnouveau.common.block.tile.ArcanePedestalTile;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ArcanePedestalRenderer.class)
public class ArcanePedestalRendererMixin {
    @WrapMethod(method = "render")
    private void ars_sable$renderSublevelItem(ArcanePedestalTile tile, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, Operation<Void> original) {
        boolean inSublevel = SublevelItemRenderHelper.isInSublevel(tile.getLevel(), Vec3.atCenterOf(tile.getBlockPos()));
        poseStack.pushPose();
        if (!SublevelItemRenderHelper.applyIfNeeded(tile.getLevel(), Vec3.atLowerCornerOf(tile.getBlockPos()), poseStack)) {
            if (inSublevel) {
                poseStack.translate(0.5D, 0.0D, 0.5D);
                original.call(tile, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
                poseStack.popPose();
                return;
            }
            poseStack.popPose();
            original.call(tile, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        poseStack.translate(0.5D, 0.0D, 0.5D);
        original.call(tile, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
