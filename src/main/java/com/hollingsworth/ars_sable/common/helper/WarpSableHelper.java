package com.hollingsworth.ars_sable.common.helper;

import com.hollingsworth.ars_sable.common.WarpSublevelTargetData;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class WarpSableHelper {
    public static Vec3 project(Level level, BlockPos pos) {
        return project(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
    }

    public static Vec3 project(Level level, double x, double y, double z) {
        if (level == null) {
            return new Vec3(x, y, z);
        }
        BlockPos targetPos = BlockPos.containing(x, y, z);
        if (SableCompanion.INSTANCE.getContaining(level, targetPos) == null) {
            if (level instanceof ServerLevel serverLevel) {
                GlobalPos fallback = WarpSublevelTargetData.from(serverLevel).get(level.dimension().location().toString(), targetPos);
                if (fallback != null && fallback.dimension().equals(level.dimension())) {
                    return new Vec3(fallback.pos().getX() + 0.5D, fallback.pos().getY(), fallback.pos().getZ() + 0.5D);
                }
            }
            return new Vec3(x, y, z);
        }
        Vector3d projected = SableCompanion.INSTANCE.projectOutOfSubLevel(level, new Vector3d(x, y, z), new Vector3d());
        return new Vec3(projected.x(), adjustedY(level, targetPos), projected.z());
    }

    public static BlockPos bindPosition(Player player, BlockPos original) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return original;
        }

        SubLevelAccess subLevel = SableCompanion.INSTANCE.getTrackingOrVehicleSubLevel(player);
        if (subLevel == null) {
            subLevel = SableCompanion.INSTANCE.getLastTrackingSubLevel(player);
        }
        if (subLevel == null) {
            return original;
        }

        BlockPos localPos = BlockPos.containing(subLevel.logicalPose().transformPositionInverse(player.position()));
        Vec3 fallbackPos = project(serverLevel, localPos);
        WarpSublevelTargetData.from(serverLevel).put(
                serverLevel.dimension().location().toString(),
                localPos,
                GlobalPos.of(serverLevel.dimension(), BlockPos.containing(fallbackPos))
        );
        return localPos;
    }

    private static double adjustedY(Level level, BlockPos localSpawnPos) {
        double x = localSpawnPos.getX();
        double y = localSpawnPos.getY();
        double z = localSpawnPos.getZ();
        return Math.ceil(Math.max(
                Math.max(projectedY(level, x, y, z), projectedY(level, x + 1.0D, y, z)),
                Math.max(projectedY(level, x, y, z + 1.0D), projectedY(level, x + 1.0D, y, z + 1.0D))
        ) - 1.0E-6D);
    }

    private static double projectedY(Level level, double x, double y, double z) {
        return SableCompanion.INSTANCE.projectOutOfSubLevel(level, new Vector3d(x, y, z), new Vector3d()).y();
    }
}
