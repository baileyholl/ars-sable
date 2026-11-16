package com.hollingsworth.ars_sable.common.registry;

import com.hollingsworth.ars_sable.ArsSable;
import com.hollingsworth.ars_sable.common.helper.SublevelSpellHelper;
import com.hollingsworth.ars_sable.common.sable.SublevelSpellEffect;
import com.hollingsworth.ars_sable.common.spell.AugmentSublevel;
import com.hollingsworth.arsnouveau.api.event.EffectResolveEvent;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SublevelEffectRegistry {
    private static final Map<AbstractEffect, SublevelSpellEffect> EFFECTS = new ConcurrentHashMap<>();

    public static void register(AbstractEffect effect, SublevelSpellEffect sublevelEffect) {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        sublevelEffect.buildConfig(builder);
        sublevelEffect.CONFIG = builder.build();
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.SERVER, sublevelEffect.CONFIG, ArsSable.MODID + "/sublevel_" + effect.getRegistryName().getPath() + ".toml");
        EFFECTS.put(effect, sublevelEffect);
        effect.compatibleAugments.add(AugmentSublevel.INSTANCE);
    }

    public static void onEffectResolve(EffectResolveEvent.Pre event) {
        SublevelSpellEffect sublevelEffect = EFFECTS.get(event.resolveEffect);
        if (sublevelEffect == null || !(event.rayTraceResult instanceof BlockHitResult blockHitResult) || !event.spellStats.hasBuff(AugmentSublevel.INSTANCE)) {
            return;
        }
        SublevelSpellHelper.SublevelHit hit = SublevelSpellHelper.getSublevelHit(event.world, blockHitResult, event.shooter, event.context);
        if (hit != null) {
            ServerSubLevel subLevel = hit.subLevel();
            RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
            if (handle == null || subLevel.getMassTracker().isInvalid()) {
                return;
            }
            sublevelEffect.onResolveSublevel(handle, subLevel, hit.hitResult(), event.shooter, event.spellStats, event.context, event.resolver);
            event.setCanceled(true);
        }
    }
}
