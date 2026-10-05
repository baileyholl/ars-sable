package com.hollingsworth.ars_sable.mixin.ritual;

import com.hollingsworth.ars_sable.common.sable.SublevelPhysicsRitual;
import com.hollingsworth.arsnouveau.api.ritual.AbstractRitual;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractRitual.class)
public abstract class AbstractRitualMixin implements SublevelPhysicsRitual {

    @Override
    public void ars_sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
    }
}
