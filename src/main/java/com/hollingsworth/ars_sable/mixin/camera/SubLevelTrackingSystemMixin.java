package com.hollingsworth.ars_sable.mixin.camera;

import com.hollingsworth.ars_sable.common.sable.SublevelCamera;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.sublevel.system.SubLevelTrackingSystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SubLevelTrackingSystem.class, remap = false)
public class SubLevelTrackingSystemMixin {

    // When a player is viewing through a sublevel scryer camera, track Sable movement from the camera's position
    @WrapOperation(method = "shouldLoad", at = @At(value = "INVOKE", target = "Lorg/joml/Vector3dc;distanceSquared(DDD)D"), remap = false)
    private double ars_sable$trackFromScryerCamera(Vector3dc sublevelPosition, double x, double y, double z, Operation<Double> original, @Local(argsOnly = true) Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            Entity camera = serverPlayer.getCamera();
            if (camera instanceof SublevelCamera sublevelCamera && sublevelCamera.ars_sable$isSublevelCamera()) {
                return original.call(sublevelPosition, camera.getX(), camera.getY(), camera.getZ());
            }
        }
        return original.call(sublevelPosition, x, y, z);
    }
}
