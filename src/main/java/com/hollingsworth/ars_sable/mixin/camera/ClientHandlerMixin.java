package com.hollingsworth.ars_sable.mixin.camera;

import com.hollingsworth.ars_sable.common.helper.ScryerHelpers;
import com.hollingsworth.ars_sable.common.sable.SublevelCamera;
import com.hollingsworth.arsnouveau.client.registry.ClientHandler;
import com.hollingsworth.arsnouveau.common.entity.ScryerCamera;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientHandler.class)
public class ClientHandlerMixin {

    @WrapOperation(method = "lambda$static$35", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private static BlockEntity ars_sable$cameraOverlayMountedCrystal(Level level, BlockPos pos, Operation<BlockEntity> original) {
        Entity cameraEntity = Minecraft.getInstance().cameraEntity;
        if (cameraEntity instanceof ScryerCamera camera
                && cameraEntity instanceof SublevelCamera sublevelCamera
                && sublevelCamera.ars_sable$isSublevelCamera()) {
            ScryerHelpers.MountedCrystal crystal = ScryerHelpers.findMountedCrystal(camera);
            if (crystal != null && crystal.inSublevel()) {
                return original.call(level, crystal.pos());
            }
        }
        return original.call(level, pos);
    }
}
