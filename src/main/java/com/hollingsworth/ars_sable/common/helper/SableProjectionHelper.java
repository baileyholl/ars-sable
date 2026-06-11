package com.hollingsworth.ars_sable.common.helper;

import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;

public class SableProjectionHelper {

    // Projects a sublevel-local position out to world space.
    public static Vector3d projectOut(Level level, double x, double y, double z) {
        return SableCompanion.INSTANCE.projectOutOfSubLevel(level, new Vector3d(x, y, z), new Vector3d());
    }

    // Highest projected world Y among the four bottom corners of the local block, rounded up so a
    // teleported entity never clips into a tilted sublevel.
    public static int projectedTopY(Level level, BlockPos localPos) {
        double x = localPos.getX();
        double y = localPos.getY();
        double z = localPos.getZ();
        double max = Math.max(
                Math.max(projectOut(level, x, y, z).y(), projectOut(level, x + 1.0D, y, z).y()),
                Math.max(projectOut(level, x, y, z + 1.0D).y(), projectOut(level, x + 1.0D, y, z + 1.0D).y())
        );
        return Mth.ceil(max);
    }

    public static BlockPos projectTargetPos(Level level, BlockPos targetPos, boolean placeAbove) {
        Vector3d realPos = projectOut(level, targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D);
        int y = projectedTopY(level, placeAbove ? targetPos.above() : targetPos);
        return new BlockPos(Mth.floor(realPos.x()), y, Mth.floor(realPos.z()));
    }

    // Projects a floor block out of its sublevel and returns the feet-level position standing on top of it.
    public static BlockPos projectStandingPos(Level level, BlockPos floorPos) {
        return projectTargetPos(level, floorPos, true);
    }
}
