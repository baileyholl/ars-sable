package com.hollingsworth.ars_sable.common.sable;

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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

public abstract class SublevelSpellEffect {
    public @Nullable ModConfigSpec CONFIG;
    public @Nullable ModConfigSpec.DoubleValue BASE;
    public @Nullable ModConfigSpec.DoubleValue AMP;

    public abstract void onResolveSublevel(RigidBodyHandle handle, ServerSubLevel subLevel, BlockHitResult rayTraceResult, LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver);

    public void buildConfig(ModConfigSpec.Builder builder) {
        BASE = builder.comment("Flat level of force to apply").defineInRange("base", 8.0, 0, Double.MAX_VALUE);
        AMP = builder.comment("Additional force to apply per amplification level").defineInRange("amp", 4.0, 0, Double.MAX_VALUE);
    }

    public double getImpulse(double ampMultiplier) {
        return BASE.get() + AMP.get() * ampMultiplier;
    }

    public void applyImpulseToBlocks(RigidBodyHandle handle, ServerSubLevel subLevel, BlockHitResult rayTraceResult, LivingEntity shooter, SpellStats spellStats, Vec3 impulse) {
        for (BlockPos hitPos : SpellUtil.calcAOEBlocks(shooter, rayTraceResult.getBlockPos(), rayTraceResult, spellStats)) {
            if (Sable.HELPER.getContaining(subLevel.getLevel(), hitPos) != subLevel) {
                continue;
            }
            if (subLevel.getLevel().getBlockState(hitPos).isAir()) {
                continue;
            }
            handle.applyImpulseAtPoint(Vec3.atCenterOf(hitPos), impulse);
        }
    }
}
