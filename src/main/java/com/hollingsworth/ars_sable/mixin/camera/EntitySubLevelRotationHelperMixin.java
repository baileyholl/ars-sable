package com.hollingsworth.ars_sable.mixin.camera;

import com.hollingsworth.ars_sable.common.sable.SublevelCamera;
import com.hollingsworth.arsnouveau.common.entity.ScryerCamera;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinhelpers.camera.camera_rotation.EntitySubLevelRotationHelper;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.Entity;
import org.joml.Quaterniond;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Mixin(value = EntitySubLevelRotationHelper.class, remap = false)
public class EntitySubLevelRotationHelperMixin {

    @Inject(method = "getSubLevelInheritedOrientation", at = @At("HEAD"), cancellable = true, remap = false)
    private static void ars_sable$scryerCameraOrientation(Entity cameraEntity, Function<SubLevel, Pose3dc> poseProvider, EntitySubLevelRotationHelper.Type type, CallbackInfoReturnable<Quaterniond> cir) {
        if (!(cameraEntity instanceof ScryerCamera)) {
            return;
        }
        SubLevel subLevel = Sable.HELPER.getTrackingSubLevel(cameraEntity);
        if (subLevel != null && ((SublevelCamera) cameraEntity).ars_sable$isSublevelCamera()) {
            cir.setReturnValue(new Quaterniond(poseProvider.apply(subLevel).orientation()));
        }
    }
}
