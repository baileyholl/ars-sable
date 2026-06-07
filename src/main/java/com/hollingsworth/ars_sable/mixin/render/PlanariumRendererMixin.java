package com.hollingsworth.ars_sable.mixin.render;

import com.hollingsworth.arsnouveau.client.renderer.tile.PlanariumRenderer;
import com.hollingsworth.arsnouveau.common.block.tile.PlanariumTile;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlanariumRenderer.class)
public class PlanariumRendererMixin {
    @WrapOperation(method = "drawRender", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V"))
    private static void ars_sable$drawInSublevel(PoseStack poseStack, double x, double y, double z, Operation<Void> original, @Local(argsOnly = true) PlanariumTile tile) {
        Vec3 renderOrigin = Vec3.atLowerCornerOf(tile.getBlockPos());
        if (!(Sable.HELPER.getContaining(tile.getLevel(), renderOrigin) instanceof ClientSubLevel subLevel)) {
            original.call(poseStack, x, y, z);
            return;
        }

        Pose3dc renderPose = subLevel.renderPose();
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vector3dc position = renderPose.position();
        Vector3dc rotationPoint = renderPose.rotationPoint();
        Vector3dc scale = renderPose.scale();

        poseStack.translate(position.x() - camera.x, position.y() - camera.y, position.z() - camera.z);
        poseStack.mulPose(new Quaternionf(renderPose.orientation()));
        poseStack.scale((float) scale.x(), (float) scale.y(), (float) scale.z());
        poseStack.translate(renderOrigin.x - rotationPoint.x(), renderOrigin.y - rotationPoint.y(), renderOrigin.z - rotationPoint.z());
    }
}
