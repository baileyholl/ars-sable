package com.hollingsworth.ars_sable.mixin.storage;

import com.hollingsworth.ars_sable.common.TrackedBlockEntityPosData;
import com.hollingsworth.ars_sable.common.sable.TrackedWorldPositionBlockEntity;
import com.hollingsworth.arsnouveau.common.block.CraftingLecternBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingLecternBlock.class)
public class CraftingLecternBlockMixin {
    @Inject(method = "onRemove", at = @At("HEAD"))
    private void ars_sable$cleanupTrackedPositions(BlockState state, Level world, BlockPos pos, BlockState newState, boolean flag, CallbackInfo ci) {
        if (state.is(newState.getBlock()) || !(world instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof TrackedWorldPositionBlockEntity trackedBlockEntity) {
            TrackedBlockEntityPosData.from(serverLevel).removeIfAtPosition(trackedBlockEntity.ars_sable$getTrackingId(), pos);
        }
    }
}

