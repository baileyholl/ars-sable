package com.hollingsworth.ars_sable.mixin.ritual;

import com.hollingsworth.ars_sable.common.sable.SublevelPhysicsRitual;
import com.hollingsworth.arsnouveau.common.block.tile.RitualBrazierTile;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(RitualBrazierTile.class)
public abstract class RitualBrazierTileMixin implements BlockEntitySubLevelActor {

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        RitualBrazierTile tile = (RitualBrazierTile) (Object) this;
        if (tile.ritual != null && !tile.isOff && tile.ritual.isRunning() && !tile.ritual.needsSourceNow()) {
            ((SublevelPhysicsRitual) tile.ritual).ars_sable$physicsTick(subLevel, handle, timeStep);
        }
    }
}
