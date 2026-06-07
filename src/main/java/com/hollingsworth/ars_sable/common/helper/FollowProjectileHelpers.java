package com.hollingsworth.ars_sable.common.helper;

import com.hollingsworth.arsnouveau.common.entity.EntityFollowProjectile;
import com.hollingsworth.arsnouveau.common.entity.EntityFlyingItem;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FollowProjectileHelpers {

    public static void projectSublevelTarget(EntityFollowProjectile projectile, Level level, Vec3 from, Vec3 to) {
        Vec3 projectedFrom = Sable.HELPER.projectOutOfSubLevel(level, from.add(0.5, 0.5, 0.5));
        Vec3 projectedTo = Sable.HELPER.projectOutOfSubLevel(level, to.add(0.5, 0.5, 0.5));
        projectile.setPos(projectedFrom);
        projectile.getEntityData().set(EntityFollowProjectile.from, BlockPos.containing(projectedFrom));
        projectile.getEntityData().set(EntityFollowProjectile.to, BlockPos.containing(projectedTo));
        projectile.setDespawnDistance((int) (projectedFrom.distanceTo(projectedTo) + 10));
    }

    public static void projectSublevelTarget(EntityFlyingItem item, Level level, Vec3 from, Vec3 to) {
        Vec3 projectedFrom = Sable.HELPER.projectOutOfSubLevel(level, from);
        Vec3 projectedTo = Sable.HELPER.projectOutOfSubLevel(level, to);
        item.setPos(projectedFrom);
        item.getEntityData().set(EntityFlyingItem.from, projectedFrom);
        item.getEntityData().set(EntityFlyingItem.to, projectedTo);
    }
}
