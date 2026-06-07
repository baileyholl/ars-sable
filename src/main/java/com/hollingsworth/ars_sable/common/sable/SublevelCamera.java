package com.hollingsworth.ars_sable.common.sable;

import net.minecraft.core.Direction;

import javax.annotation.Nullable;

public interface SublevelCamera {
    @Nullable
    Direction ars_sable$getFacing();

    boolean ars_sable$isSublevelCamera();
}
