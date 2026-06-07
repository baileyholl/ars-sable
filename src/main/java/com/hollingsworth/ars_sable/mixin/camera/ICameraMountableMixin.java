package com.hollingsworth.ars_sable.mixin.camera;

import com.hollingsworth.ars_sable.common.helper.ScryerHelpers;
import com.hollingsworth.arsnouveau.api.camera.ICameraMountable;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ICameraMountable.class)
public interface ICameraMountableMixin {

    // Force-load the projected real-world chunks the camera will render, not the extreme sublevel ones.
    @WrapOperation(method = "mountCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/SectionPos;of(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/SectionPos;"))
    private SectionPos ars_sable$projectChunkCenter(BlockPos pos, Operation<SectionPos> original, @Local(argsOnly = true) Level level) {
        if (!ScryerHelpers.isInSublevel(level, pos)) {
            return original.call(pos);
        }
        return original.call(ScryerHelpers.projectChunkCenter(level, pos));
    }
}
