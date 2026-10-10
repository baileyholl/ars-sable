package com.hollingsworth.ars_sable.common.spell;

import com.hollingsworth.ars_sable.common.block.ImpulsePrismTile;
import com.hollingsworth.ars_sable.common.sable.SublevelSpellEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.api.util.SpellUtil;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class KnockbackSublevelEffect extends SublevelSpellEffect {

    @Override
    public void onResolveSublevel(RigidBodyHandle handle, ServerSubLevel subLevel, BlockHitResult rayTraceResult, LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        double impulse = getImpulse(spellStats.getAmpMultiplier());
        for(BlockPos hitPos : SpellUtil.calcAOEBlocks(shooter, rayTraceResult.getBlockPos(), rayTraceResult, spellStats)) {
            if (Sable.HELPER.getContaining(subLevel.getLevel(), hitPos) != subLevel) {
                continue;
            }
            if (subLevel.getLevel().getBlockEntity(hitPos) instanceof ImpulsePrismTile prism) {
                prism.receiveImpulse(impulse);
                continue;
            }
            if(subLevel.getLevel().getBlockState(hitPos).isAir()) {
                continue;
            }
            Vec3 face = Vec3.atLowerCornerOf(rayTraceResult.getDirection().getNormal());
            handle.applyImpulseAtPoint(Vec3.atCenterOf(hitPos).add(face.scale(0.5)), face.scale(-impulse));
        }
    }
}
