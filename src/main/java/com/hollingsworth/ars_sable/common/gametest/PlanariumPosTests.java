package com.hollingsworth.ars_sable.common.gametest;

import com.hollingsworth.ars_sable.ArsSable;
import com.hollingsworth.ars_sable.common.SublevelPosData;
import com.hollingsworth.ars_sable.common.helper.SableProjectionHelper;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

@GameTestHolder(ArsSable.MODID)
@PrefixGameTestTemplate(false)
public class PlanariumPosTests {

    @GameTest(template = StorageLecternTests.TEMPLATE_EMPTY)
    public static void restorePosFollowsRepeatedSublevelMoves(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UUID playerId = UUID.randomUUID();
        BlockPos enteredFromWorld = helper.absolutePos(new BlockPos(4, 1, 2));
        BlockPos firstMoveWorld = helper.absolutePos(new BlockPos(5, 1, 2));
        BlockPos secondMoveWorld = helper.absolutePos(new BlockPos(6, 1, 2));
        BlockPos fallbackWorld = helper.absolutePos(new BlockPos(2, 1, 2));

        SublevelPosData data = SublevelPosData.from(level);
        data.put(level, playerId,GlobalPos.of(level.dimension(), enteredFromWorld), Vec2.ZERO, GlobalPos.of(level.dimension(), fallbackWorld));

        SubLevelAssemblyHelper.moveBlocks(level, new SubLevelAssemblyHelper.AssemblyTransform(enteredFromWorld, firstMoveWorld, 0, Rotation.NONE, level), List.of(enteredFromWorld));
        SubLevelAssemblyHelper.moveBlocks(level, new SubLevelAssemblyHelper.AssemblyTransform(firstMoveWorld, secondMoveWorld, 0, Rotation.NONE, level), List.of(firstMoveWorld));

        GlobalPos resolved = data.getTransformedPos(level, playerId);
        BlockPos expectedFeet = secondMoveWorld.above();
        helper.assertTrue(resolved != null && resolved.pos().equals(expectedFeet), "Expected restore pos to follow moves to " + expectedFeet + ", actual=" + (resolved == null ? null : resolved.pos()));
        data.removePlayer(playerId);
        helper.succeed();
    }

    @GameTest(template = StorageLecternTests.TEMPLATE_EMPTY)
    public static void restorePosFallsBackWhenSublevelGone(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UUID playerId = UUID.randomUUID();
        BlockPos enteredFromWorld = helper.absolutePos(new BlockPos(4, 1, 4));
        BlockPos fallbackWorld = helper.absolutePos(new BlockPos(2, 1, 4));

        SublevelPosData data = SublevelPosData.from(level);
        data.put(level, playerId,GlobalPos.of(level.dimension(), enteredFromWorld), Vec2.ZERO, GlobalPos.of(level.dimension(), fallbackWorld));

        GlobalPos resolved = data.getTransformedPos(level, playerId);
        helper.assertTrue(resolved != null && resolved.pos().equals(fallbackWorld), "Expected unmoved restore pos to fall back to " + fallbackWorld + ", actual=" + (resolved == null ? null : resolved.pos()));
        data.removePlayer(playerId);
        helper.succeed();
    }

    @GameTest(template = StorageLecternTests.TEMPLATE_EMPTY)
    public static void restorePosSurvivesWorldAndSublevelReassembly(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UUID playerId = UUID.randomUUID();
        BlockPos originalWorld = helper.absolutePos(new BlockPos(4, 1, 6));
        BlockPos returnedWorld = helper.absolutePos(new BlockPos(6, 1, 6));

        level.setBlock(originalWorld, Blocks.CHEST.defaultBlockState(), 3);
        ServerSubLevel firstSubLevel = SubLevelAssemblyHelper.assembleBlocks(level, originalWorld, List.of(originalWorld), bounds(originalWorld));
        BlockPos firstSublevelPos = firstSubLevel.getPlot().getCenterBlock();

        SublevelPosData data = SublevelPosData.from(level);
        data.put(level, playerId,GlobalPos.of(level.dimension(), firstSublevelPos), Vec2.ZERO,
                GlobalPos.of(level.dimension(), SableProjectionHelper.projectStandingPos(level, firstSublevelPos)));

        GlobalPos onSublevel = data.getTransformedPos(level, playerId);
        BlockPos expectedProjected = SableProjectionHelper.projectStandingPos(level, firstSublevelPos);
        helper.assertTrue(onSublevel != null && onSublevel.pos().equals(expectedProjected), "Expected projected restore pos " + expectedProjected + ", actual=" + (onSublevel == null ? null : onSublevel.pos()));

        SubLevelAssemblyHelper.moveBlocks(level, new SubLevelAssemblyHelper.AssemblyTransform(firstSublevelPos, returnedWorld, 0, Rotation.NONE, level), List.of(firstSublevelPos));

        GlobalPos afterReturn = data.getTransformedPos(level, playerId);
        BlockPos expectedReturnedFeet = returnedWorld.above();
        helper.assertTrue(afterReturn != null && afterReturn.pos().equals(expectedReturnedFeet), "Expected returned world restore pos " + expectedReturnedFeet + ", actual=" + (afterReturn == null ? null : afterReturn.pos()));

        ServerSubLevel secondSubLevel = SubLevelAssemblyHelper.assembleBlocks(level, returnedWorld, List.of(returnedWorld), bounds(returnedWorld));
        BlockPos secondSublevelPos = secondSubLevel.getPlot().getCenterBlock();
        BlockPos expectedReassembled = SableProjectionHelper.projectStandingPos(level, secondSublevelPos);

        GlobalPos afterReassembly = data.getTransformedPos(level, playerId);
        helper.assertTrue(afterReassembly != null && afterReassembly.pos().equals(expectedReassembled), "Expected reassembled restore pos " + expectedReassembled + ", actual=" + (afterReassembly == null ? null : afterReassembly.pos()));
        data.removePlayer(playerId);
        helper.succeed();
    }

    @GameTest(template = StorageLecternTests.TEMPLATE_EMPTY)
    public static void restorePosUsesSnapshotWhileSublevelUnloaded(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UUID playerId = UUID.randomUUID();
        BlockPos originalWorld = helper.absolutePos(new BlockPos(4, 1, 8));

        level.setBlock(originalWorld, Blocks.CHEST.defaultBlockState(), 3);
        ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, originalWorld, List.of(originalWorld), bounds(originalWorld));
        BlockPos sublevelPos = subLevel.getPlot().getCenterBlock();

        SublevelPosData data = SublevelPosData.from(level);
        data.put(level, playerId,GlobalPos.of(level.dimension(), sublevelPos), Vec2.ZERO,
                GlobalPos.of(level.dimension(), SableProjectionHelper.projectStandingPos(level, sublevelPos)));

        BlockPos expectedSnapshot = SableProjectionHelper.projectStandingPos(level, sublevelPos);
        data.setSublevelLoaded(level, subLevel.getUniqueId(), false);

        GlobalPos whileUnloaded = data.getTransformedPos(level, playerId);
        helper.assertTrue(whileUnloaded != null && whileUnloaded.pos().equals(expectedSnapshot), "Expected unloaded snapshot " + expectedSnapshot + ", actual=" + (whileUnloaded == null ? null : whileUnloaded.pos()));

        data.setSublevelLoaded(level, subLevel.getUniqueId(), true);
        GlobalPos afterReload = data.getTransformedPos(level, playerId);
        helper.assertTrue(afterReload != null && afterReload.pos().equals(expectedSnapshot), "Expected reloaded projected pos " + expectedSnapshot + ", actual=" + (afterReload == null ? null : afterReload.pos()));
        data.removePlayer(playerId);
        helper.succeed();
    }

    @GameTest(template = StorageLecternTests.TEMPLATE_EMPTY)
    public static void restorePosServedAfterSublevelRemovedWhileUnloaded(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UUID playerId = UUID.randomUUID();
        BlockPos originalWorld = helper.absolutePos(new BlockPos(6, 1, 8));

        level.setBlock(originalWorld, Blocks.CHEST.defaultBlockState(), 3);
        ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, originalWorld, List.of(originalWorld), bounds(originalWorld));
        BlockPos sublevelPos = subLevel.getPlot().getCenterBlock();

        SublevelPosData data = SublevelPosData.from(level);
        data.put(level, playerId, GlobalPos.of(level.dimension(), sublevelPos), Vec2.ZERO,
                GlobalPos.of(level.dimension(), SableProjectionHelper.projectStandingPos(level, sublevelPos)));

        BlockPos expectedSnapshot = SableProjectionHelper.projectStandingPos(level, sublevelPos);
        data.setSublevelLoaded(level, subLevel.getUniqueId(), false);
        data.removeSublevel(level, subLevel.getUniqueId());

        GlobalPos afterRemoval = data.getTransformedPos(level, playerId);
        helper.assertTrue(afterRemoval != null && afterRemoval.pos().equals(expectedSnapshot), "Expected removal restore pos " + expectedSnapshot + ", actual=" + (afterRemoval == null ? null : afterRemoval.pos()));
        data.removePlayer(playerId);
        helper.succeed();
    }

    @GameTest(template = StorageLecternTests.TEMPLATE_EMPTY)
    public static void restorePosTracksWorldPosAssembledIntoSublevel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UUID playerId = UUID.randomUUID();
        BlockPos enteredFromWorld = helper.absolutePos(new BlockPos(2, 1, 6));
        BlockPos returnedWorld = helper.absolutePos(new BlockPos(2, 1, 8));

        // Entering from a plain world position tracks the floor block stood on, with the feet
        // position as fallback, matching PlanariumHelpers#sendEntityTo.
        BlockPos feetPos = enteredFromWorld.above();
        SublevelPosData data = SublevelPosData.from(level);
        data.put(level, playerId,GlobalPos.of(level.dimension(), enteredFromWorld), Vec2.ZERO, GlobalPos.of(level.dimension(), feetPos));

        GlobalPos beforeAssembly = data.getTransformedPos(level, playerId);
        helper.assertTrue(beforeAssembly != null && beforeAssembly.pos().equals(feetPos), "Expected untouched world restore pos " + feetPos + ", actual=" + (beforeAssembly == null ? null : beforeAssembly.pos()));

        level.setBlock(enteredFromWorld, Blocks.CHEST.defaultBlockState(), 3);
        ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, enteredFromWorld, List.of(enteredFromWorld), bounds(enteredFromWorld));
        BlockPos sublevelPos = subLevel.getPlot().getCenterBlock();
        BlockPos expectedProjected = SableProjectionHelper.projectStandingPos(level, sublevelPos);

        GlobalPos onSublevel = data.getTransformedPos(level, playerId);
        helper.assertTrue(onSublevel != null && onSublevel.pos().equals(expectedProjected), "Expected world pos to be tracked into sublevel, projected " + expectedProjected + ", actual=" + (onSublevel == null ? null : onSublevel.pos()));

        SubLevelAssemblyHelper.moveBlocks(level, new SubLevelAssemblyHelper.AssemblyTransform(sublevelPos, returnedWorld, 0, Rotation.NONE, level), List.of(sublevelPos));

        GlobalPos afterReturn = data.getTransformedPos(level, playerId);
        BlockPos expectedReturnedFeet = returnedWorld.above();
        helper.assertTrue(afterReturn != null && afterReturn.pos().equals(expectedReturnedFeet), "Expected disassembled restore pos " + expectedReturnedFeet + ", actual=" + (afterReturn == null ? null : afterReturn.pos()));
        data.removePlayer(playerId);
        helper.succeed();
    }

    private static BoundingBox3i bounds(BlockPos pos) {
        return new BoundingBox3i(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }
}
