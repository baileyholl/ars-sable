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

    // Projects a floor block out of its sublevel and returns the feet-level position standing on top of it.
    public static BlockPos projectStandingPos(Level level, BlockPos floorPos) {
        Vector3d realPos = projectOut(level, floorPos.getX() + 0.5D, floorPos.getY(), floorPos.getZ() + 0.5D);
        return new BlockPos(Mth.floor(realPos.x()), projectedTopY(level, floorPos.above()), Mth.floor(realPos.z()));
    }
}
