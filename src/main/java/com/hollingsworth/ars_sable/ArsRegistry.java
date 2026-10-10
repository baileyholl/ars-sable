package com.hollingsworth.ars_sable;

import com.hollingsworth.ars_sable.common.registry.SublevelEffectRegistry;
import com.hollingsworth.ars_sable.common.ritual.RitualLevitation;
import com.hollingsworth.ars_sable.common.spell.*;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.registry.RitualRegistry;
import com.hollingsworth.arsnouveau.common.spell.effect.*;

public class ArsRegistry {

    public static void init() {
        GlyphRegistry.registerSpell(EffectMiniaturize.INSTANCE);
        GlyphRegistry.registerSpell(AugmentSublevel.INSTANCE);
        SublevelEffectRegistry.register(EffectKnockback.INSTANCE, new KnockbackSublevelEffect());
        SublevelEffectRegistry.register(EffectLaunch.INSTANCE, new LaunchSublevelEffect());
        SublevelEffectRegistry.register(EffectLeap.INSTANCE, new LeapSublevelEffect());
        SublevelEffectRegistry.register(EffectPull.INSTANCE, new PullSublevelEffect());
        SublevelEffectRegistry.register(EffectGravity.INSTANCE, new GravitySublevelEffect());
        SublevelEffectRegistry.register(EffectRotate.INSTANCE, new RotateSublevelEffect());
        RitualRegistry.registerRitual(new RitualLevitation());
    }
}
