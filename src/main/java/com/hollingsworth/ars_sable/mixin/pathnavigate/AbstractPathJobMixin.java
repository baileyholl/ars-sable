package com.hollingsworth.ars_sable.mixin.pathnavigate;

import com.hollingsworth.ars_sable.common.helper.PathNavigateHelpers;
import com.hollingsworth.arsnouveau.common.entity.pathfinding.pathjobs.AbstractPathJob;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractPathJob.class)
public class AbstractPathJobMixin {

    @Inject(method = "prepareStart", at = @At("RETURN"), cancellable = true)
    private static void ars_sable$returnLocalStart(LivingEntity entity, CallbackInfoReturnable<BlockPos> cir) {
        cir.setReturnValue(PathNavigateHelpers.localPreparedStart(entity, cir.getReturnValue()));
    }
}
