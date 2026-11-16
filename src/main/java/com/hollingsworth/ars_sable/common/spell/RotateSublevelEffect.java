package com.hollingsworth.ars_sable.common.spell;

import com.hollingsworth.ars_sable.common.sable.SublevelSpellEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.joml.Vector3d;

public class RotateSublevelEffect extends SublevelSpellEffect {
    @Override
    public void buildConfig(ModConfigSpec.Builder builder) {
        BASE = builder.comment("Flat angular velocity to apply in radians per second").defineInRange("base", 1.0, 0, Double.MAX_VALUE);
        AMP = builder.comment("Additional angular velocity to apply per amplification level").defineInRange("amp", 0.5, 0, Double.MAX_VALUE);
    }

    @Override
    public void onResolveSublevel(RigidBodyHandle handle, ServerSubLevel subLevel, BlockHitResult rayTraceResult, LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        double amp = spellStats.getAmpMultiplier();
        Vector3d axis = spellStats.isSensitive() ? new Vector3d(rayTraceResult.getDirection().step()) : subLevel.logicalPose().orientation().transformInverse(new Vector3d(0, 1, 0));
        handle.applyAngularImpulse(subLevel.getMassTracker().getInertiaTensor().transform(axis.mul((amp < 0 ? 1 : -1) * getImpulse(Math.abs(amp)))));
    }
}
