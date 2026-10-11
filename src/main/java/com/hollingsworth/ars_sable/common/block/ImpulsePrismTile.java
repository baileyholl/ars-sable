package com.hollingsworth.ars_sable.common.block;

import com.hollingsworth.ars_sable.common.registry.ModBlockRegistry;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ImpulsePrismTile extends BlockEntity implements BlockEntitySubLevelActor {

    private double impulse;
    private int ticksLeft;

    public ImpulsePrismTile(BlockPos pos, BlockState state) {
        super(ModBlockRegistry.IMPULSE_PRISM_TILE.get(), pos, state);
    }

    public void receiveImpulse(double impulse) {
        this.impulse = impulse;
        this.ticksLeft = 20;
    }

    @Override
    public void sable$tick(ServerSubLevel subLevel) {
        if (ticksLeft <= 0) {
            return;
        }
        ticksLeft--;
        RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
        if (handle == null || subLevel.getMassTracker().isInvalid()) {
            return;
        }
        Vec3 facing = Vec3.atLowerCornerOf(getBlockState().getValue(ImpulsePrismBlock.FACING).getNormal());
        handle.applyImpulseAtPoint(Vec3.atCenterOf(getBlockPos()), facing.scale(impulse));
    }
}
