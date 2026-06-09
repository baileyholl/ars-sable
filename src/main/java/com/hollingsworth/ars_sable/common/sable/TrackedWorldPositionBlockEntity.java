package com.hollingsworth.ars_sable.common.sable;

import net.minecraft.core.BlockPos;

import java.util.Collection;
import java.util.UUID;

public interface TrackedWorldPositionBlockEntity {
    UUID ars_sable$getTrackingId();

    Collection<BlockPos> ars_sable$getTrackedPositions();

    void ars_sable$replaceTrackedPosition(BlockPos oldPos, BlockPos newPos);
}

