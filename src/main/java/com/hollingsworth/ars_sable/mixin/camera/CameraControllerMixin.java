package com.hollingsworth.ars_sable.mixin.camera;

import com.hollingsworth.ars_sable.common.sable.SublevelCamera;
import com.hollingsworth.arsnouveau.common.block.ScryerCrystal;
import com.hollingsworth.arsnouveau.common.camera.CameraController;
import com.hollingsworth.arsnouveau.common.entity.ScryerCamera;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CameraController.class)
public class CameraControllerMixin {
    
    @Inject(method = "onClientTick(Lnet/neoforged/neoforge/client/event/ClientTickEvent$Post;)V", at = @At("TAIL"))
    private static void ars_sable$followRenderPosition(ClientTickEvent.Post event, CallbackInfo ci) {
        Entity cameraEntity = Minecraft.getInstance().cameraEntity;
        if (cameraEntity instanceof SublevelCamera sublevelCamera && sublevelCamera.ars_sable$isSublevelCamera()) {
            CameraController.setRenderPosition(cameraEntity);
        }
    }

    @WrapOperation(method = "moveViewHorizontally", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockState ars_sable$facingFromSync(Level level, BlockPos pos, Operation<BlockState> original, @Local(argsOnly = true) ScryerCamera cam) {
        BlockState state = original.call(level, pos);
        if (!state.hasProperty(ScryerCrystal.FACING) && cam instanceof SublevelCamera sublevelCamera) {
            if (!sublevelCamera.ars_sable$isSublevelCamera()) {
                return state;
            }
            Direction facing = sublevelCamera.ars_sable$getFacing();
            if (facing == null) {
                return state;
            }
            return BlockRegistry.SCRYERS_CRYSTAL.get().defaultBlockState()
                    .setValue(ScryerCrystal.FACING, facing);
        }
        return state;
    }
}
