package com.hollingsworth.ars_sable.common.helper;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.common.spell.method.MethodUnderfoot;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public class SublevelSpellHelper {

    // transforms hit results to the local space of the sublevel if one was hit
    public static @Nullable SublevelHit getSublevelHit(Level level, BlockHitResult rayTraceResult, LivingEntity shooter, SpellContext spellContext) {
        BlockPos hitPos = rayTraceResult.getBlockPos();
        if (spellContext.getSpell().getCastMethod() instanceof MethodUnderfoot) {
            BlockHitResult below = level.clip(new ClipContext(shooter.position(), shooter.position().subtract(0, 1, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
            hitPos = below.getType() == HitResult.Type.BLOCK ? below.getBlockPos() : shooter.getOnPos();
        }
        SubLevel containing = Sable.HELPER.getContaining(level, hitPos);
        if (containing != null) {
            return containing instanceof ServerSubLevel subLevel ? localizeHit(level, subLevel, rayTraceResult, hitPos) : null;
        }
        return Sable.HELPER.runIncludingSubLevels(level, hitPos.getCenter(), false, null,
                (SubLevel candidate, BlockPos pos) -> candidate instanceof ServerSubLevel subLevel && !level.getBlockState(pos).isEmpty() ? localizeHit(level, subLevel, rayTraceResult, pos) : null);
    }

    private static SublevelHit localizeHit(Level level, ServerSubLevel subLevel, BlockHitResult rayTraceResult, BlockPos localPos) {
        if (Sable.HELPER.getContaining(level, rayTraceResult.getBlockPos()) == subLevel) {
            return new SublevelHit(subLevel, rayTraceResult);
        }
        Pose3dc pose = subLevel.logicalPose();
        Vec3i normal = rayTraceResult.getDirection().getNormal();
        Vector3d localNormal = pose.orientation().transformInverse(new Vector3d(normal.getX(), normal.getY(), normal.getZ()));
        Direction direction = Direction.getNearest(localNormal.x, localNormal.y, localNormal.z);
        return new SublevelHit(subLevel, new BlockHitResult(pose.transformPositionInverse(rayTraceResult.getLocation()), direction, localPos, rayTraceResult.isInside()));
    }

    public record SublevelHit(ServerSubLevel subLevel, BlockHitResult hitResult) {
    }
}
