package com.hollingsworth.ars_sable.common.spell;

import com.hollingsworth.ars_sable.common.sable.SublevelSpellEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class PullSublevelEffect extends SublevelSpellEffect {
    @Override
    public void onResolveSublevel(RigidBodyHandle handle, ServerSubLevel subLevel, BlockHitResult rayTraceResult, LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        Vec3 toShooter = shooter.position().subtract(subLevel.logicalPose().transformPosition(rayTraceResult.getLocation())).normalize();
        Vec3 impulse = subLevel.logicalPose().transformNormalInverse(toShooter.scale(getImpulse(spellStats.getAmpMultiplier())));
        applyImpulseToBlocks(handle, subLevel, rayTraceResult, shooter, spellStats, impulse);
    }
}
