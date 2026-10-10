package com.hollingsworth.ars_sable.common.spell;

import com.hollingsworth.ars_sable.common.sable.SublevelSpellEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.util.SpellUtil;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Vector3d;

public class RotateSublevelEffect extends SublevelSpellEffect {
    @Override
    public void onResolveSublevel(RigidBodyHandle handle, ServerSubLevel subLevel, BlockHitResult rayTraceResult, LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        double amp = spellStats.getAmpMultiplier();
        Vector3d axis = spellStats.isSensitive() ? new Vector3d(rayTraceResult.getDirection().step()) : subLevel.logicalPose().orientation().transformInverse(new Vector3d(0, 1, 0));
        Vector3d impulse = axis.mul((amp < 0 ? 1 : -1) * getImpulse(Math.abs(amp)));
        for (BlockPos hitPos : SpellUtil.calcAOEBlocks(shooter, rayTraceResult.getBlockPos(), rayTraceResult, spellStats)) {
            if (Sable.HELPER.getContaining(subLevel.getLevel(), hitPos) != subLevel) {
                continue;
            }
            if (subLevel.getLevel().getBlockState(hitPos).isAir()) {
                continue;
            }
            handle.applyAngularImpulse(impulse);
        }
    }
}
