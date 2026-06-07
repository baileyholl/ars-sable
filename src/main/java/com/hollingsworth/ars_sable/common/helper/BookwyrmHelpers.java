package com.hollingsworth.ars_sable.common.helper;

import com.hollingsworth.arsnouveau.common.entity.EntityBookwyrm;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class BookwyrmHelpers {

    public static void trackLecternSublevel(EntityBookwyrm bookwyrm, BlockPos lecternPos) {
        if (bookwyrm.level().isClientSide || lecternPos == null || !(bookwyrm instanceof EntityMovementExtension movementExtension)) {
            return;
        }
        SubLevel subLevel = Sable.HELPER.getContaining(bookwyrm.level(), lecternPos.getCenter());
        if (subLevel == null) {
            return;
        }
        Vec3 localPos = localBookwyrmPos(bookwyrm, movementExtension, subLevel);
        movementExtension.sable$setTrackingSubLevel(subLevel);
        bookwyrm.setPos(subLevel.logicalPose().transformPosition(localPos));
    }

    private static Vec3 localBookwyrmPos(EntityBookwyrm bookwyrm, EntityMovementExtension movementExtension, SubLevel subLevel) {
        if (movementExtension.sable$getTrackingSubLevel() == subLevel) {
            return subLevel.lastPose().transformPositionInverse(bookwyrm.position());
        }
        if (Sable.HELPER.getContaining(bookwyrm.level(), bookwyrm.position()) == subLevel) {
            return bookwyrm.position();
        }
        return subLevel.logicalPose().transformPositionInverse(bookwyrm.position());
    }
}
