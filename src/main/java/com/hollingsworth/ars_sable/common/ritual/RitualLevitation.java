package com.hollingsworth.ars_sable.common.ritual;

import com.hollingsworth.ars_sable.ArsSable;
import com.hollingsworth.ars_sable.common.sable.SublevelPhysicsRitual;
import com.hollingsworth.arsnouveau.api.ritual.AbstractRitual;
import com.hollingsworth.arsnouveau.client.particle.ParticleColor;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public class RitualLevitation extends AbstractRitual implements SublevelPhysicsRitual {

    @Override
    protected void tick() {
        if (!getWorld().isClientSide && getWorld().getGameTime() % 100 == 0 && Sable.HELPER.getContaining(getWorld(), getPos()) != null) {
            takeSourceNow();
        }
    }

    @Override
    public void ars_sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        if (Sable.HELPER.getContaining(subLevel.getLevel(), getPos()) != subLevel) {
            return;
        }
        double liftCapacity = 32.0;
        Vector3d point = JOMLConversion.atCenterOf(getPos());
        double riseSpeed = Sable.HELPER.getVelocity(subLevel.getLevel(), subLevel, point, new Vector3d()).y;
        Vector3d impulse = DimensionPhysicsData.getGravity(subLevel.getLevel())
                .mul(-liftCapacity * Math.clamp(1 - riseSpeed, 0, 1) * timeStep);
        subLevel.getOrCreateQueuedForceGroup(ForceGroups.LEVITATION.get())
                .applyAndRecordPointForce(point, subLevel.logicalPose().orientation().transformInverse(impulse));
    }

    @Override
    public boolean canStart(@Nullable Player player) {
        return Sable.HELPER.getContaining(getWorld(), getPos()) != null;
    }

    @Override
    public int getSourceCost() {
        return 100;
    }

    @Override
    public ParticleColor getCenterColor() {
        return new ParticleColor(200, 255, 255);
    }

    @Override
    public ParticleColor getOuterColor() {
        return new ParticleColor(150, 90, 255);
    }

    @Override
    public ResourceLocation getRegistryName() {
        return ArsSable.prefix("ritual_levitation");
    }

    @Override
    public String getLangName() {
        return "Levitation";
    }

    @Override
    public String getLangDescription() {
        return "";
    }
}
