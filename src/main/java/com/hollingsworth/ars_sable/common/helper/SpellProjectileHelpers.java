package com.hollingsworth.ars_sable.common.helper;

import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SpellProjectileHelpers {

    public static void launchFromSublevel(EntityProjectileSpell spell, SubLevel subLevel, Vec3 localVelocity) {
        Pose3dc pose = subLevel.logicalPose();
        Vec3 anchor = new Vec3(0, spell.getBbHeight() / 2, 0);
        Vec3 localCenter = spell.position().add(anchor);
        Vec3 velocity = pose.transformNormal(localVelocity).add(Sable.HELPER.getVelocity(spell.level(), subLevel, localCenter).scale(1 / 20.0));
        spell.moveTo(pose.transformPosition(localCenter).subtract(anchor));
        spell.shoot(velocity.x, velocity.y, velocity.z, (float) velocity.length(), 0);
    }

    public static void redirectOnSublevel(Level level, BlockPos prismPos, EntityProjectileSpell spell, Runnable redirect) {
        SubLevel subLevel = Sable.HELPER.getContaining(level, prismPos);
        if (subLevel == null) {
            redirect.run();
            return;
        }
        Vec3 shipVelocity = Sable.HELPER.getVelocity(level, subLevel, prismPos.getCenter()).scale(1 / 20.0);
        spell.setDeltaMovement(spell.getDeltaMovement().subtract(shipVelocity));
        redirect.run();
        if (spell.isRemoved()) {
            return;
        }
        if (Sable.HELPER.getContaining(spell) == subLevel) {
            launchFromSublevel(spell, subLevel, spell.getDeltaMovement());
            spell.setPos(spell.position().add(shipVelocity));
        } else {
            spell.setDeltaMovement(spell.getDeltaMovement().add(shipVelocity));
        }
    }
}
