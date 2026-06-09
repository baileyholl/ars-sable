package com.hollingsworth.ars_sable.common.gametest;

import com.hollingsworth.ars_sable.ArsSable;
import com.hollingsworth.ars_sable.common.WarpSublevelTargetData;
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

    private static BoundingBox3i bounds(BlockPos pos) {
        return new BoundingBox3i(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }
}
