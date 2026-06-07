package com.hollingsworth.ars_sable.common.helper;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public class FlyingMobHelpers {

    public static void trackAnchorSublevel(Mob mob, BlockPos anchorPos) {
        if (mob.level().isClientSide || anchorPos == null || !(mob instanceof EntityMovementExtension movementExtension)) {
            return;
        }
        SubLevel subLevel = Sable.HELPER.getContaining(mob.level(), anchorPos.getCenter());
        if (subLevel == null) {
            return;
        }
        Vec3 localPos = localMobPos(mob, movementExtension, subLevel);
        movementExtension.sable$setTrackingSubLevel(subLevel);
        mob.setPos(subLevel.logicalPose().transformPosition(localPos));
    }

    private static Vec3 localMobPos(Mob mob, EntityMovementExtension movementExtension, SubLevel subLevel) {
        if (movementExtension.sable$getTrackingSubLevel() == subLevel) {
            return subLevel.lastPose().transformPositionInverse(mob.position());
        }
        if (Sable.HELPER.getContaining(mob.level(), mob.position()) == subLevel) {
            return mob.position();
        }
        return subLevel.logicalPose().transformPositionInverse(mob.position());
    }
}
