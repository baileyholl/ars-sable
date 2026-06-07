package com.hollingsworth.ars_sable.common.helper;

import com.hollingsworth.arsnouveau.ArsNouveau;
import com.hollingsworth.arsnouveau.common.block.ScryerCrystal;
import com.hollingsworth.arsnouveau.common.entity.ScryerCamera;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class ScryerHelpers {

    public static boolean attachCameraToSublevel(ScryerCamera camera, Level level, BlockPos pos) {
        SubLevel subLevel = Sable.HELPER.getContaining(level, pos.getCenter());
        if (subLevel == null) {
            return false;
        }
        // Project the entity's actual position so the crystal's vertical (down) offset is preserved.
        camera.setPos(subLevel.logicalPose().transformPosition(camera.position()));
        ((EntityMovementExtension) camera).sable$setTrackingSubLevel(subLevel);
        return true;
    }

    public static boolean isInSublevel(Level level, BlockPos pos) {
        return Sable.HELPER.getContaining(level, pos.getCenter()) != null;
    }

    public static BlockPos projectChunkCenter(Level level, BlockPos pos) {
        SubLevel subLevel = Sable.HELPER.getContaining(level, pos.getCenter());
        if (subLevel == null) {
            return pos;
        }
        return BlockPos.containing(subLevel.logicalPose().transformPosition(pos.getCenter()));
    }

    public static void serverTickFollow(ScryerCamera camera, Vec3 localPos) {
        if (!(camera.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        SubLevel subLevel = Sable.HELPER.getContaining(serverLevel, localPos);
        if (subLevel == null) {
            ((EntityMovementExtension) camera).sable$setTrackingSubLevel(null);
            return;
        }
        ((EntityMovementExtension) camera).sable$setTrackingSubLevel(subLevel);
        ChunkPos oldChunk = camera.chunkPosition();
        ChunkTrackingView oldView = camera.getCameraChunks();
        camera.setPos(subLevel.logicalPose().transformPosition(localPos));
        if (camera.chunkPosition().equals(oldChunk) || !(oldView instanceof ChunkTrackingView.Positioned positioned)) {
            return;
        }
        camera.setChunkLoadingDistance(positioned.viewDistance());
        ChunkTrackingView newView = camera.getCameraChunks();
        ChunkTrackingView.difference(oldView, newView,
                entering -> ArsNouveau.ticketController.forceChunk(serverLevel, camera, entering.x, entering.z, true, false),
                leaving -> ArsNouveau.ticketController.forceChunk(serverLevel, camera, leaving.x, leaving.z, false, false));
        camera.setHasSentChunks(false);
    }

    public static void clientTickFollow(ScryerCamera camera, Vec3 localPos) {
        SubLevel subLevel = Sable.HELPER.getContaining(camera.level(), localPos);
        if (subLevel == null) {
            ((EntityMovementExtension) camera).sable$setTrackingSubLevel(null);
            return;
        }
        ((EntityMovementExtension) camera).sable$setTrackingSubLevel(subLevel);
        camera.xOld = camera.xo = camera.getX();
        camera.yOld = camera.yo = camera.getY();
        camera.zOld = camera.zo = camera.getZ();
        camera.setPos(subLevel.logicalPose().transformPosition(localPos));
    }

    @Nullable
    public static MountedCrystal findMountedCrystal(ScryerCamera camera) {
        SubLevelAccess trackingSublevel = SableCompanion.INSTANCE.getTrackingSubLevel(camera);
        if (trackingSublevel != null) {
            Vec3 localPos = trackingSublevel.logicalPose().transformPositionInverse(camera.position());
            MountedCrystal crystal = findMountedCrystal(camera.level(), localPos, trackingSublevel);
            if (crystal != null) {
                return crystal;
            }

            Vec3 lastLocalPos = trackingSublevel.lastPose().transformPositionInverse(camera.position());
            return findMountedCrystal(camera.level(), lastLocalPos, trackingSublevel);
        }
        return findMountedCrystal(camera.level(), camera.position(), null);
    }

    @Nullable
    public static MountedCrystal findMountedCrystal(Level level, Vec3 localPos) {
        SubLevel subLevel = Sable.HELPER.getContaining(level, localPos);
        if (subLevel == null) {
            return null;
        }
        return findMountedCrystal(level, localPos, subLevel);
    }

    @Nullable
    private static MountedCrystal findMountedCrystal(Level level, Vec3 origin, @Nullable SubLevelAccess subLevel) {
        return SableCompanion.INSTANCE.runIncludingSubLevels(level, (Position) origin, true, subLevel, (candidateSublevel, pos) -> {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() != BlockRegistry.SCRYERS_CRYSTAL.get() || !state.hasProperty(ScryerCrystal.FACING)) {
                return null;
            }
            Direction facing = state.getValue(ScryerCrystal.FACING);
            Vec3 localPos = pos.getCenter();
            if (facing == Direction.DOWN) {
                localPos = localPos.add(0.0, 0.25, 0.0);
            }
            return new MountedCrystal(pos.immutable(), facing, localPos, candidateSublevel != null);
        });
    }

    public record MountedCrystal(BlockPos pos, Direction facing, Vec3 localPos, boolean inSublevel) {
    }
}
