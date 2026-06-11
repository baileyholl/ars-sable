package com.hollingsworth.ars_sable.common.gametest;

import com.hollingsworth.ars_sable.ArsSable;
import com.hollingsworth.ars_sable.common.WarpSublevelTargetData;
import com.hollingsworth.ars_sable.common.helper.SableProjectionHelper;
import com.hollingsworth.ars_sable.common.helper.WarpSableHelper;
import com.hollingsworth.arsnouveau.common.block.tile.PortalTile;
import com.hollingsworth.arsnouveau.common.items.data.WarpScrollData;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

@GameTestHolder(ArsSable.MODID)
@PrefixGameTestTemplate(false)
public class WarpPortalTests {

    @GameTest(template = StorageLecternTests.TEMPLATE_EMPTY)
    public static void portalWithoutTrackedDataUsesOriginalScrollTarget(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos portalPos = new BlockPos(2, 1, 2);
        BlockPos target = new BlockPos(4, 1, 2);
        BlockPos targetWorld = helper.absolutePos(target);

        helper.setBlock(portalPos, BlockRegistry.PORTAL_BLOCK.get().defaultBlockState());
        helper.setBlock(target, Blocks.CHEST);

        PortalTile portal = helper.getBlockEntity(portalPos);
        portal.warpPos = targetWorld;
        portal.dimID = level.dimension().location().toString();
        portal.rotationVec = Vec2.ZERO;

        helper.assertTrue(WarpSublevelTargetData.from(level).get(GlobalPos.of(level.dimension(), targetWorld)) == null, "Expected no tracked warp target");

        Entity entity = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 1, 2));
        PortalTile.teleportEntityTo(entity, level, portal.warpPos, portal.rotationVec);

        helper.assertTrue(entity.blockPosition().equals(targetWorld), "Expected untracked portal target " + targetWorld + ", actual=" + entity.blockPosition());
        helper.succeed();
    }

    @GameTest(template = StorageLecternTests.TEMPLATE_EMPTY)
    public static void portalTargetTracksRepeatedSublevelMoves(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos portalPos = new BlockPos(2, 1, 2);
        BlockPos originalTarget = new BlockPos(4, 1, 2);
        BlockPos sublevelTarget = new BlockPos(5, 1, 2);
        BlockPos returnedTarget = new BlockPos(6, 1, 2);
        BlockPos movedSublevelTarget = new BlockPos(7, 1, 2);
        BlockPos originalTargetWorld = helper.absolutePos(originalTarget);
        BlockPos sublevelTargetWorld = helper.absolutePos(sublevelTarget);
        BlockPos returnedTargetWorld = helper.absolutePos(returnedTarget);
        BlockPos movedSublevelTargetWorld = helper.absolutePos(movedSublevelTarget);

        helper.setBlock(portalPos, BlockRegistry.PORTAL_BLOCK.get().defaultBlockState());
        helper.setBlock(originalTarget, Blocks.CHEST);

        PortalTile portal = helper.getBlockEntity(portalPos);
        portal.setFromScroll(new WarpScrollData(Optional.of(originalTargetWorld), level.dimension().location().toString(), Vec2.ZERO, false));

        SubLevelAssemblyHelper.moveBlocks(level, new SubLevelAssemblyHelper.AssemblyTransform(originalTargetWorld, sublevelTargetWorld, 0, Rotation.NONE, level), List.of(originalTargetWorld));
        SubLevelAssemblyHelper.moveBlocks(level, new SubLevelAssemblyHelper.AssemblyTransform(sublevelTargetWorld, returnedTargetWorld, 0, Rotation.NONE, level), List.of(sublevelTargetWorld));
        SubLevelAssemblyHelper.moveBlocks(level, new SubLevelAssemblyHelper.AssemblyTransform(returnedTargetWorld, movedSublevelTargetWorld, 0, Rotation.NONE, level), List.of(returnedTargetWorld));

        Entity entity = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 1, 2));
        PortalTile.teleportEntityTo(entity, level, portal.warpPos, portal.rotationVec);

        helper.assertTrue(entity.blockPosition().equals(movedSublevelTargetWorld), "Expected portal to resolve moved target " + movedSublevelTargetWorld + ", actual=" + entity.blockPosition());
        helper.succeed();
    }

    @GameTest(template = StorageLecternTests.TEMPLATE_EMPTY)
    public static void portalTeleportsToReboundTargetAfterRepeatedSublevelMoves(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos portalPos = new BlockPos(2, 1, 2);
        BlockPos originalTarget = new BlockPos(4, 1, 2);
        BlockPos sublevelTarget = new BlockPos(5, 1, 2);
        BlockPos returnedTarget = new BlockPos(6, 1, 2);
        BlockPos movedSublevelTarget = new BlockPos(7, 1, 2);
        BlockPos originalTargetWorld = helper.absolutePos(originalTarget);
        BlockPos sublevelTargetWorld = helper.absolutePos(sublevelTarget);
        BlockPos returnedTargetWorld = helper.absolutePos(returnedTarget);
        BlockPos movedSublevelTargetWorld = helper.absolutePos(movedSublevelTarget);

        helper.setBlock(portalPos, BlockRegistry.PORTAL_BLOCK.get().defaultBlockState());
        helper.setBlock(originalTarget, Blocks.CHEST);

        PortalTile portal = helper.getBlockEntity(portalPos);
        portal.setFromScroll(new WarpScrollData(Optional.of(originalTargetWorld), level.dimension().location().toString(), Vec2.ZERO, false));

        SubLevelAssemblyHelper.moveBlocks(level, new SubLevelAssemblyHelper.AssemblyTransform(originalTargetWorld, sublevelTargetWorld, 0, Rotation.NONE, level), List.of(originalTargetWorld));
        SubLevelAssemblyHelper.moveBlocks(level, new SubLevelAssemblyHelper.AssemblyTransform(sublevelTargetWorld, returnedTargetWorld, 0, Rotation.NONE, level), List.of(sublevelTargetWorld));
        SubLevelAssemblyHelper.moveBlocks(level, new SubLevelAssemblyHelper.AssemblyTransform(returnedTargetWorld, movedSublevelTargetWorld, 0, Rotation.NONE, level), List.of(returnedTargetWorld));

        portal.setFromScroll(new WarpScrollData(Optional.of(movedSublevelTargetWorld), level.dimension().location().toString(), Vec2.ZERO, false));
        Entity entity = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 1, 2));
        PortalTile.teleportEntityTo(entity, level, portal.warpPos, portal.rotationVec);

        helper.assertTrue(entity.blockPosition().equals(movedSublevelTargetWorld), "Expected rebound portal target " + movedSublevelTargetWorld + ", actual=" + entity.blockPosition());
        helper.succeed();
    }

    @GameTest(template = StorageLecternTests.TEMPLATE_EMPTY)
    public static void portalCreatedAfterSublevelTargetReturnsToWorldUsesMovedTarget(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos portalPos = new BlockPos(2, 1, 4);
        BlockPos fallbackTarget = new BlockPos(4, 1, 4);
        BlockPos sublevelTarget = new BlockPos(5, 1, 4);
        BlockPos returnedTarget = new BlockPos(6, 1, 4);
        BlockPos fallbackTargetWorld = helper.absolutePos(fallbackTarget);
        BlockPos sublevelTargetWorld = helper.absolutePos(sublevelTarget);
        BlockPos returnedTargetWorld = helper.absolutePos(returnedTarget);

        helper.setBlock(sublevelTarget, Blocks.CHEST);
        WarpSublevelTargetData.from(level).put(
                level,
                GlobalPos.of(level.dimension(), sublevelTargetWorld),
                GlobalPos.of(level.dimension(), sublevelTargetWorld),
                GlobalPos.of(level.dimension(), fallbackTargetWorld)
        );
        SubLevelAssemblyHelper.moveBlocks(level, new SubLevelAssemblyHelper.AssemblyTransform(sublevelTargetWorld, returnedTargetWorld, 0, Rotation.NONE, level), List.of(sublevelTargetWorld));

        helper.setBlock(portalPos, BlockRegistry.PORTAL_BLOCK.get().defaultBlockState());
        PortalTile portal = helper.getBlockEntity(portalPos);
        portal.setFromScroll(new WarpScrollData(Optional.of(sublevelTargetWorld), level.dimension().location().toString(), Vec2.ZERO, false));

        Entity entity = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 1, 4));
        PortalTile.teleportEntityTo(entity, level, portal.warpPos, portal.rotationVec);

        helper.assertTrue(entity.blockPosition().equals(returnedTargetWorld), "Expected portal to use returned target " + returnedTargetWorld + ", actual=" + entity.blockPosition());
        helper.succeed();
    }

    @GameTest(template = StorageLecternTests.TEMPLATE_EMPTY)
    public static void portalActivatedBeforeSublevelTargetReturnsToWorldUsesMovedTarget(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos portalPos = new BlockPos(2, 1, 6);
        BlockPos fallbackTarget = new BlockPos(4, 1, 6);
        BlockPos sublevelTarget = new BlockPos(5, 1, 6);
        BlockPos returnedTarget = new BlockPos(6, 1, 6);
        BlockPos fallbackTargetWorld = helper.absolutePos(fallbackTarget);
        BlockPos sublevelTargetWorld = helper.absolutePos(sublevelTarget);
        BlockPos returnedTargetWorld = helper.absolutePos(returnedTarget);

        helper.setBlock(sublevelTarget, Blocks.CHEST);
        WarpSublevelTargetData.from(level).put(
                level,
                GlobalPos.of(level.dimension(), sublevelTargetWorld),
                GlobalPos.of(level.dimension(), sublevelTargetWorld),
                GlobalPos.of(level.dimension(), fallbackTargetWorld)
        );

        helper.setBlock(portalPos, BlockRegistry.PORTAL_BLOCK.get().defaultBlockState());
        PortalTile portal = helper.getBlockEntity(portalPos);
        portal.setFromScroll(new WarpScrollData(Optional.of(sublevelTargetWorld), level.dimension().location().toString(), Vec2.ZERO, false));

        SubLevelAssemblyHelper.moveBlocks(level, new SubLevelAssemblyHelper.AssemblyTransform(sublevelTargetWorld, returnedTargetWorld, 0, Rotation.NONE, level), List.of(sublevelTargetWorld));

        Entity entity = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 1, 6));
        PortalTile.teleportEntityTo(entity, level, portal.warpPos, portal.rotationVec);

        helper.assertTrue(entity.blockPosition().equals(returnedTargetWorld), "Expected activated portal to use returned target " + returnedTargetWorld + ", actual=" + entity.blockPosition());
        helper.succeed();
    }

    @GameTest(template = StorageLecternTests.TEMPLATE_EMPTY)
    public static void portalBoundToSublevelTargetSurvivesWorldAndSublevelReassembly(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos portalPos = new BlockPos(2, 1, 8);
        BlockPos originalTarget = helper.absolutePos(new BlockPos(4, 1, 8));
        BlockPos returnedTarget = helper.absolutePos(new BlockPos(6, 1, 8));

        helper.setBlock(portalPos, BlockRegistry.PORTAL_BLOCK.get().defaultBlockState());
        level.setBlock(originalTarget, Blocks.CHEST.defaultBlockState(), 3);

        ServerSubLevel firstSubLevel = SubLevelAssemblyHelper.assembleBlocks(level, originalTarget, List.of(originalTarget), bounds(originalTarget));
        BlockPos firstSublevelTarget = firstSubLevel.getPlot().getCenterBlock();
        Vec3 firstProjectedTarget = SableCompanion.INSTANCE.projectOutOfSubLevel(level, firstSublevelTarget.getCenter());
        WarpSublevelTargetData.from(level).put(
                level,
                GlobalPos.of(level.dimension(), firstSublevelTarget),
                GlobalPos.of(level.dimension(), firstSublevelTarget),
                GlobalPos.of(level.dimension(), BlockPos.containing(firstProjectedTarget)),
                true
        );

        PortalTile portal = helper.getBlockEntity(portalPos);
        portal.setFromScroll(new WarpScrollData(Optional.of(firstSublevelTarget), level.dimension().location().toString(), Vec2.ZERO, false));
        SubLevelAssemblyHelper.moveBlocks(level, new SubLevelAssemblyHelper.AssemblyTransform(firstSublevelTarget, returnedTarget, 0, Rotation.NONE, level), List.of(firstSublevelTarget));

        Entity afterReturn = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 1, 8));
        PortalTile.teleportEntityTo(afterReturn, level, portal.warpPos, portal.rotationVec);
        helper.assertTrue(afterReturn.blockPosition().equals(returnedTarget.above()), "Expected returned world target " + returnedTarget.above() + ", actual=" + afterReturn.blockPosition());

        ServerSubLevel secondSubLevel = SubLevelAssemblyHelper.assembleBlocks(level, returnedTarget, List.of(returnedTarget), bounds(returnedTarget));
        BlockPos secondSublevelTarget = secondSubLevel.getPlot().getCenterBlock();
        BlockPos expectedSecondTarget = BlockPos.containing(SableCompanion.INSTANCE.projectOutOfSubLevel(level, secondSublevelTarget.getCenter())).above();

        Entity afterReassembly = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 1, 8));
        PortalTile.teleportEntityTo(afterReassembly, level, portal.warpPos, portal.rotationVec);
        helper.assertTrue(afterReassembly.blockPosition().equals(expectedSecondTarget), "Expected reassembled sublevel target " + expectedSecondTarget + ", actual=" + afterReassembly.blockPosition());
        helper.succeed();
    }

    @GameTest(template = StorageLecternTests.TEMPLATE_EMPTY)
    public static void portalTargetUsesSnapshotWhileSublevelUnloaded(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos portalPos = new BlockPos(2, 1, 4);
        BlockPos originalTarget = helper.absolutePos(new BlockPos(4, 1, 4));

        helper.setBlock(portalPos, BlockRegistry.PORTAL_BLOCK.get().defaultBlockState());
        level.setBlock(originalTarget, Blocks.CHEST.defaultBlockState(), 3);

        ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, originalTarget, List.of(originalTarget), bounds(originalTarget));
        BlockPos sublevelTarget = subLevel.getPlot().getCenterBlock();
        WarpSublevelTargetData data = WarpSublevelTargetData.from(level);
        data.put(
                level,
                GlobalPos.of(level.dimension(), sublevelTarget),
                GlobalPos.of(level.dimension(), sublevelTarget),
                GlobalPos.of(level.dimension(), originalTarget),
                true
        );

        PortalTile portal = helper.getBlockEntity(portalPos);
        portal.setFromScroll(new WarpScrollData(Optional.of(sublevelTarget), level.dimension().location().toString(), Vec2.ZERO, false));

        BlockPos expectedSnapshot = SableProjectionHelper.projectStandingPos(level, sublevelTarget);
        data.setSublevelLoaded(level, subLevel.getUniqueId(), false);

        Entity whileUnloaded = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 1, 4));
        PortalTile.teleportEntityTo(whileUnloaded, level, portal.warpPos, portal.rotationVec);
        helper.assertTrue(whileUnloaded.blockPosition().equals(expectedSnapshot), "Expected unloaded snapshot target " + expectedSnapshot + ", actual=" + whileUnloaded.blockPosition());

        data.setSublevelLoaded(level, subLevel.getUniqueId(), true);
        Entity afterReload = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 1, 4));
        PortalTile.teleportEntityTo(afterReload, level, portal.warpPos, portal.rotationVec);
        helper.assertTrue(afterReload.blockPosition().equals(expectedSnapshot), "Expected reloaded projected target " + expectedSnapshot + ", actual=" + afterReload.blockPosition());
        helper.succeed();
    }

    @GameTest(template = StorageLecternTests.TEMPLATE_EMPTY)
    public static void portalTargetRestoredWhenSublevelRemoved(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos portalPos = new BlockPos(2, 1, 6);
        BlockPos originalTarget = helper.absolutePos(new BlockPos(4, 1, 6));

        helper.setBlock(portalPos, BlockRegistry.PORTAL_BLOCK.get().defaultBlockState());
        level.setBlock(originalTarget, Blocks.CHEST.defaultBlockState(), 3);

        ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, originalTarget, List.of(originalTarget), bounds(originalTarget));
        BlockPos sublevelTarget = subLevel.getPlot().getCenterBlock();
        WarpSublevelTargetData data = WarpSublevelTargetData.from(level);
        data.put(
                level,
                GlobalPos.of(level.dimension(), sublevelTarget),
                GlobalPos.of(level.dimension(), sublevelTarget),
                GlobalPos.of(level.dimension(), originalTarget),
                true
        );

        PortalTile portal = helper.getBlockEntity(portalPos);
        portal.setFromScroll(new WarpScrollData(Optional.of(sublevelTarget), level.dimension().location().toString(), Vec2.ZERO, false));

        BlockPos expectedRestore = SableProjectionHelper.projectStandingPos(level, sublevelTarget);
        data.removeSublevel(level, subLevel.getUniqueId());

        Entity afterRemoval = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 1, 6));
        PortalTile.teleportEntityTo(afterRemoval, level, portal.warpPos, portal.rotationVec);
        helper.assertTrue(afterRemoval.blockPosition().equals(expectedRestore), "Expected removal restore target " + expectedRestore + ", actual=" + afterRemoval.blockPosition());
        helper.succeed();
    }

    @GameTest(template = StorageLecternTests.TEMPLATE_EMPTY)
    public static void portalBoundToWorldTargetTracksIntoSublevel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos portalPos = new BlockPos(2, 1, 2);
        BlockPos originalTarget = helper.absolutePos(new BlockPos(4, 1, 2));

        helper.setBlock(portalPos, BlockRegistry.PORTAL_BLOCK.get().defaultBlockState());
        level.setBlock(originalTarget, Blocks.CHEST.defaultBlockState(), 3);

        WarpSublevelTargetData data = WarpSublevelTargetData.from(level);
        data.put(
                level,
                GlobalPos.of(level.dimension(), originalTarget),
                GlobalPos.of(level.dimension(), originalTarget),
                GlobalPos.of(level.dimension(), originalTarget),
                true
        );

        PortalTile portal = helper.getBlockEntity(portalPos);
        portal.setFromScroll(new WarpScrollData(Optional.of(originalTarget), level.dimension().location().toString(), Vec2.ZERO, false));

        ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, originalTarget, List.of(originalTarget), bounds(originalTarget));
        BlockPos sublevelTarget = subLevel.getPlot().getCenterBlock();

        WarpSublevelTargetData.Target tracked = data.get(GlobalPos.of(level.dimension(), originalTarget));
        helper.assertTrue(tracked != null && tracked.pos().pos().equals(sublevelTarget), "Expected tracked target to follow assembly to " + sublevelTarget + ", actual=" + (tracked == null ? null : tracked.pos().pos()));
        helper.assertTrue(tracked.sublevelId().isPresent() && tracked.sublevelId().get().equals(subLevel.getUniqueId()), "Expected tracked target to record containing sublevel id");

        BlockPos expected = SableProjectionHelper.projectTargetPos(level, sublevelTarget, true);
        Entity entity = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 1, 2));
        PortalTile.teleportEntityTo(entity, level, portal.warpPos, portal.rotationVec);
        helper.assertTrue(entity.blockPosition().equals(expected), "Expected assembled sublevel target " + expected + ", actual=" + entity.blockPosition());
        helper.succeed();
    }

    @GameTest(template = StorageLecternTests.TEMPLATE_EMPTY)
    public static void scrollBoundInWorldTracksTargetIntoSublevel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos standPos = helper.absolutePos(new BlockPos(4, 2, 4));
        BlockPos floorPos = standPos.below();
        level.setBlock(floorPos, Blocks.CHEST.defaultBlockState(), 3);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos bound = WarpSableHelper.bindPosition(player, standPos);
        helper.assertTrue(bound.equals(floorPos), "Expected world bind to track floor block " + floorPos + ", actual=" + bound);
        helper.assertTrue(WarpSublevelTargetData.from(level).get(GlobalPos.of(level.dimension(), bound)) != null, "Expected world bind to create a tracked warp target");

        // Binding in the real world without any sublevel involvement still resolves to the stand position.
        Vec3 beforeAssembly = WarpSableHelper.project(level, bound);
        helper.assertTrue(BlockPos.containing(beforeAssembly).equals(standPos), "Expected unmoved bind to resolve to " + standPos + ", actual=" + BlockPos.containing(beforeAssembly));

        ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, floorPos, List.of(floorPos), bounds(floorPos));
        BlockPos sublevelTarget = subLevel.getPlot().getCenterBlock();

        BlockPos expected = SableProjectionHelper.projectTargetPos(level, sublevelTarget, true);
        Vec3 afterAssembly = WarpSableHelper.project(level, bound);
        helper.assertTrue(BlockPos.containing(afterAssembly).equals(expected), "Expected assembled bind to resolve to " + expected + ", actual=" + BlockPos.containing(afterAssembly));
        helper.succeed();
    }

    private static BoundingBox3i bounds(BlockPos pos) {
        return new BoundingBox3i(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }
}
