package com.hollingsworth.ars_sable.common.helper;

import com.hollingsworth.arsnouveau.common.entity.pathfinding.MinecoloniesAdvancedPathNavigate;
import com.hollingsworth.arsnouveau.common.entity.pathfinding.PathResult;
import com.hollingsworth.arsnouveau.common.entity.pathfinding.pathjobs.AbstractPathJob;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PathNavigateHelpers {

    public static boolean tryMoveToBlockPos(MinecoloniesAdvancedPathNavigate self, BlockPos pos, double speedFactor, Operation<Boolean> original){
        if (pos == null) {
            return false;
        }
        BlockPos start = AbstractPathJob.prepareStart(self.getOurEntity());
        if (!isSublevelPath(self.getOurEntity(), start, pos)) {
            return original.call(pos, speedFactor);
        }
        BlockPos target = adjustPathTarget(self.getOurEntity(), pos, start);
        return self.moveToXYZ(target.getX(), target.getY(), target.getZ(), speedFactor) != null;
    }

    public static PathResult moveToXYZ(MinecoloniesAdvancedPathNavigate self, double x, double y, double z, double speedFactor, Operation<PathResult> original) {
        BlockPos start = AbstractPathJob.prepareStart(self.getOurEntity());
        BlockPos target = adjustPathTarget(self.getOurEntity(), BlockPos.containing(x, y, z), start);
        int range = (int) self.getOurEntity().getAttribute(Attributes.FOLLOW_RANGE).getValue();
        if (Math.abs(start.getX() - target.getX()) > 1024 + range || Math.abs(start.getZ() - target.getZ()) > 1024 + range) {
            return null;
        }
        return original.call((double) target.getX(), (double) target.getY(), (double) target.getZ(), speedFactor);
    }

    public static Vec3 projectTarget(Level level, double x, double y, double z) {
        Vec3 target = new Vec3(x, y, z);
        if (Sable.HELPER.getContaining(level, target) == null) {
            return target;
        }
        return Sable.HELPER.projectOutOfSubLevel(level, target);
    }

    public static Vec3 projectEntityPosition(Entity entity) {
        SubLevelAccess subLevel = SableCompanion.INSTANCE.getTrackingOrVehicleSubLevel(entity);
        if (subLevel != null) {
            return subLevel.logicalPose().transformPosition(entity.position());
        }
        return Sable.HELPER.projectOutOfSubLevel(entity.level(), entity.position());
    }

    public static BlockPos localPreparedStart(Entity entity, BlockPos preparedSublevelPos) {
        BlockPos sublevelStart = BlockPos.containing(entity.getX(), entity.getY(), entity.getZ());
        BlockPos localStart = entity.blockPosition();
        if (entity instanceof EntityMovementExtension movementExtension) {
            entity.getInBlockState();
            localStart = movementExtension.sable$getInBlockStatePos();
        }
        return localStart.offset(preparedSublevelPos.subtract(sublevelStart));
    }

    private static boolean isSublevelPath(Entity entity, BlockPos start, BlockPos target) {
        Level level = entity.level();
        return SableCompanion.INSTANCE.getTrackingOrVehicleSubLevel(entity) != null
                || SableCompanion.INSTANCE.getContaining(level, start) != null
                || SableCompanion.INSTANCE.getContaining(level, target) != null;
    }

    private static BlockPos adjustPathTarget(Entity entity, BlockPos pos, BlockPos start) {
        Level level = entity.level();
        SubLevelAccess startSublevel = SableCompanion.INSTANCE.getContaining(level, start);
        if (startSublevel != null) {
            if (SableCompanion.INSTANCE.getContaining(level, pos) == startSublevel) {
                return pos;
            }
            Vec3 localPos = startSublevel.logicalPose().transformPositionInverse(pos.getCenter());
            if (SableCompanion.INSTANCE.getContaining(level, localPos) == startSublevel) {
                return BlockPos.containing(localPos);
            }
            return pos;
        }
        SubLevelAccess trackingSublevel = SableCompanion.INSTANCE.getTrackingOrVehicleSubLevel(entity);
        if (trackingSublevel != null) {
            if (SableCompanion.INSTANCE.getContaining(level, pos) == trackingSublevel) {
                return pos;
            }
            Vec3 localPos = trackingSublevel.logicalPose().transformPositionInverse(pos.getCenter());
            if (SableCompanion.INSTANCE.getContaining(level, localPos) == trackingSublevel) {
                return BlockPos.containing(localPos);
            }
            return pos;
        }
        SubLevelAccess targetSublevel = SableCompanion.INSTANCE.getContaining(level, pos);
        if (targetSublevel == null) {
            return pos;
        }
        return BlockPos.containing(SableCompanion.INSTANCE.projectOutOfSubLevel(level, pos.getCenter()));
    }
}
