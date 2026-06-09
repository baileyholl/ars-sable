package com.hollingsworth.ars_sable.mixin.sable;

import com.hollingsworth.ars_sable.common.TrackedBlockEntityPosData;
import com.hollingsworth.ars_sable.common.WarpSublevelTargetData;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SubLevelAssemblyHelper.class)
public class SubLevelAssemblyHelperMixin {
    @Inject(method = "moveBlocks", at = @At("TAIL"))
    private static void ars_sable$trackMovedBlockPositions(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform, Iterable<BlockPos> blocks, CallbackInfo ci) {
        TrackedBlockEntityPosData data = TrackedBlockEntityPosData.from(level);
        WarpSublevelTargetData warpData = WarpSublevelTargetData.from(level);
        for (BlockPos oldPos : blocks) {
            BlockPos newPos = transform.apply(oldPos);
            if (!newPos.equals(oldPos)) {
                data.handleBlockMoved(level, oldPos, newPos);
                warpData.handleBlockMoved(level, oldPos, newPos);
            }
        }
    }
}


