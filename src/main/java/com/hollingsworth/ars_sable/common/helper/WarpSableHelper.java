package com.hollingsworth.ars_sable.common.helper;

import com.hollingsworth.ars_sable.common.WarpSublevelTargetData;
import com.hollingsworth.ars_sable.common.WarpSublevelTargetData.Target;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class WarpSableHelper {
    public static Vec3 project(Level level, BlockPos pos) {
        return project(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
    }

    public static Vec3 project(Level level, double x, double y, double z) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return new Vec3(x, y, z);
        }
        BlockPos targetPos = BlockPos.containing(x, y, z);
        SubLevelAccess targetSubLevel = SableCompanion.INSTANCE.getContaining(level, targetPos);

        GlobalPos key = GlobalPos.of(level.dimension(), targetPos);
        WarpSublevelTargetData targetData = WarpSublevelTargetData.from(serverLevel);
        Target target = targetData.get(key);
        GlobalPos trackedTarget = target == null ? null : target.pos();
        boolean placeAbove = target != null && target.placeAbove();

        if (trackedTarget != null && trackedTarget.dimension().equals(level.dimension()) && targetSubLevel == null && trackedTarget.pos().equals(targetPos)) {
            GlobalPos fallbackTarget = target.fallback();
            if (fallbackTarget != null) {
                trackedTarget = fallbackTarget;
            }
        }
        if (trackedTarget != null && trackedTarget.dimension().equals(level.dimension()) && (targetSubLevel == null || !trackedTarget.pos().equals(targetPos))) {
            targetPos = trackedTarget.pos();
            x = targetPos.getX() + 0.5D;
            y = targetPos.getY();
            z = targetPos.getZ() + 0.5D;
            targetSubLevel = SableCompanion.INSTANCE.getContaining(level, targetPos);
        }

        if (targetSubLevel == null) {
            return new Vec3(x, placeAbove ? y + 1.0D : y, z);
        }
        Vector3d projected = SableCompanion.INSTANCE.projectOutOfSubLevel(level, new Vector3d(x, y, z), new Vector3d());
        return new Vec3(projected.x(), adjustedY(level, placeAbove ? targetPos.above() : targetPos), projected.z());
    }

    public static void trackWarpTarget(ServerLevel serverLevel, GlobalPos target) {
        WarpSublevelTargetData targetData = WarpSublevelTargetData.from(serverLevel);
        if (targetData.get(target) != null) {
            return;
        }
        targetData.put(target, target);
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

        BlockPos localPos = BlockPos.containing(subLevel.logicalPose().transformPositionInverse(player.position())).below();
        Vec3 fallbackPos = SableCompanion.INSTANCE.projectOutOfSubLevel(serverLevel, localPos.getCenter());
        WarpSublevelTargetData.from(serverLevel).put(
                GlobalPos.of(serverLevel.dimension(), localPos.immutable()),
                GlobalPos.of(serverLevel.dimension(), localPos.immutable()),
                GlobalPos.of(serverLevel.dimension(), BlockPos.containing(fallbackPos)),
                true
        );
        return localPos;
    }

    public static BlockPos storageBlockPosition(Entity entity, BlockPos original) {
        if (entity instanceof EntityMovementExtension movementExtension) {
            entity.getInBlockState();
            BlockPos pos = movementExtension.sable$getInBlockStatePos();
            if (SableCompanion.INSTANCE.getContaining(entity.level(), pos) != null) {
                return pos;
            }
        }
        return original;
    }

    private static double adjustedY(Level level, BlockPos localSpawnPos) {
        double x = localSpawnPos.getX();
        double y = localSpawnPos.getY();
        double z = localSpawnPos.getZ();
        return Math.ceil(Math.max(
                Math.max(projectedY(level, x, y, z), projectedY(level, x + 1.0D, y, z)),
                Math.max(projectedY(level, x, y, z + 1.0D), projectedY(level, x + 1.0D, y, z + 1.0D))
        ));
    }

    private static double projectedY(Level level, double x, double y, double z) {
        return SableCompanion.INSTANCE.projectOutOfSubLevel(level, new Vector3d(x, y, z), new Vector3d()).y();
    }
}
