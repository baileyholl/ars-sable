package com.hollingsworth.ars_sable.common.sable;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;

public interface SublevelPhysicsRitual {
    void ars_sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep);
}
