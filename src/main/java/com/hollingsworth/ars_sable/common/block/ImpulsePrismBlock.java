package com.hollingsworth.ars_sable.common.block;

import com.hollingsworth.arsnouveau.api.event.EventQueue;
import com.hollingsworth.arsnouveau.api.event.ITimedEvent;
import com.hollingsworth.arsnouveau.common.block.ModBlock;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class ImpulsePrismBlock extends ModBlock {
    public static final DirectionProperty FACING = DirectionalBlock.FACING;

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }

    public void redirectImpulse(Level level, BlockPos pos, BlockState state, Vector3dc impulse) {
        Vec3i facing = state.getValue(ImpulsePrismBlock.FACING).getNormal();
        RepeatImpulseEvent event = new RepeatImpulseEvent(level, pos, new Vector3d(facing.getX(), facing.getY(), facing.getZ()).mul(-impulse.length()), state);
        event.applyImpulse();
        EventQueue.getServerInstance().addEvent(event);
    }

    public static class RepeatImpulseEvent implements ITimedEvent {
        public Level level;
        public BlockPos pos;
        public Vector3dc impulse;
        public BlockState state;
        public long startTime;
        public int ticks;
        public int maxTicks;

        public RepeatImpulseEvent(Level level, BlockPos pos, Vector3dc impulse, BlockState state) {
            this.level = level;
            this.pos = pos;
            this.impulse = impulse;
            this.state = state;
            this.startTime = level.getGameTime();
            maxTicks = 20;
        }

        public void applyImpulse() {
            if (Sable.HELPER.getContaining(level, pos) instanceof ServerSubLevel subLevel && !subLevel.isRemoved() && !subLevel.getMassTracker().isInvalid()) {
                Vector3d torque = JOMLConversion.atCenterOf(pos).sub(subLevel.getMassTracker().getCenterOfMass()).cross(impulse);
                RigidBodyHandle.of(subLevel).applyLinearAndAngularImpulse(impulse, torque);
            }
        }

        @Override
        public void tick(boolean serverSide) {
            if (level.getGameTime() == startTime) {
                return;
            }
            ticks++;
            applyImpulse();
        }

        @Override
        public boolean isExpired() {
            return this.ticks >= maxTicks || !level.getBlockState(pos).equals(state);
        }
    }
}
