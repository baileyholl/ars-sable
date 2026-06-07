package com.hollingsworth.ars_sable.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3dc;
import org.joml.Vector3f;

public class SublevelItemRenderHelper {
    public static boolean isInSublevel(Level level, Vec3 localPos) {
        return Sable.HELPER.getContaining(level, localPos) instanceof ClientSubLevel;
    }

    public static boolean applyIfNeeded(Level level, Vec3 localOrigin, PoseStack poseStack) {
        if (!(Sable.HELPER.getContaining(level, localOrigin) instanceof ClientSubLevel subLevel)) {
            return false;
        }

        Pose3dc renderPose = subLevel.renderPose();
        Vec3 projectedOrigin = renderPose.transformPosition(localOrigin);
        if (projectedOrigin.distanceToSqr(localOrigin) < 1.0E-6) {
            return false;
        }

        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vector3f current = poseStack.last().pose().transformPosition(0.0F, 0.0F, 0.0F, new Vector3f());
        Vec3 currentWorld = new Vec3(current.x() + camera.x, current.y() + camera.y, current.z() + camera.z);
        if (currentWorld.distanceToSqr(projectedOrigin) <= currentWorld.distanceToSqr(localOrigin)) {
            return false;
        }

        Vector3dc position = renderPose.position();
        Vector3dc rotationPoint = renderPose.rotationPoint();
        Vector3dc scale = renderPose.scale();

        poseStack.translate(-localOrigin.x, -localOrigin.y, -localOrigin.z);
        poseStack.translate(position.x(), position.y(), position.z());
        poseStack.mulPose(new Quaternionf(renderPose.orientation()));
        poseStack.scale((float) scale.x(), (float) scale.y(), (float) scale.z());
        poseStack.translate(localOrigin.x - rotationPoint.x(), localOrigin.y - rotationPoint.y(), localOrigin.z - rotationPoint.z());
        return true;
    }
}
