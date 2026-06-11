package com.hollingsworth.ars_sable.common.gametest;

import com.hollingsworth.ars_sable.ArsSable;
import com.hollingsworth.ars_sable.common.TrackedBlockEntityPosData;
import com.hollingsworth.ars_sable.common.sable.TrackedWorldPositionBlockEntity;
import com.hollingsworth.arsnouveau.api.ANFakePlayer;
import com.hollingsworth.arsnouveau.common.block.tile.StorageLecternTile;
import com.hollingsworth.arsnouveau.common.entity.goal.bookwyrm.TransferTask;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(ArsSable.MODID)
@PrefixGameTestTemplate(false)
public class StorageLecternTests {

    public static final String TEMPLATE_EMPTY = "empty10";

    @GameTest(template = TEMPLATE_EMPTY)
    public static void movedHandlerPositionUpdatesLectern(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos ownerPos = new BlockPos(2, 1, 2);
        BlockPos oldTarget = new BlockPos(4, 1, 2);
        BlockPos newTarget = new BlockPos(5, 1, 2);
        BlockPos oldTargetWorld = helper.absolutePos(oldTarget);
        BlockPos newTargetWorld = helper.absolutePos(newTarget);

        StorageLecternTile owner = placeLectern(helper, ownerPos);
        helper.setBlock(oldTarget, Blocks.CHEST);
        owner.addHandlerPos(owner, oldTargetWorld);

        SubLevelAssemblyHelper.moveBlocks(level, new SubLevelAssemblyHelper.AssemblyTransform(oldTargetWorld, newTargetWorld, 0, Rotation.NONE, level), List.of(oldTargetWorld));

        helper.assertTrue(owner.handlerPosList.stream().anyMatch(handlerPos -> handlerPos.pos().equals(newTargetWorld)), "Expected handler to move to " + newTargetWorld);
        helper.assertTrue(owner.handlerPosList.stream().noneMatch(handlerPos -> handlerPos.pos().equals(oldTargetWorld)), "Stale handler target was kept at " + oldTargetWorld);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE_EMPTY)
    public static void movedHandlerPositionUpdatesMultipleLecterns(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos ownerAPos = new BlockPos(2, 1, 2);
        BlockPos ownerBPos = new BlockPos(2, 1, 4);
        BlockPos oldTarget = new BlockPos(4, 1, 3);
        BlockPos newTarget = new BlockPos(5, 1, 3);
        BlockPos oldTargetWorld = helper.absolutePos(oldTarget);
        BlockPos newTargetWorld = helper.absolutePos(newTarget);

        StorageLecternTile ownerA = placeLectern(helper, ownerAPos);
        StorageLecternTile ownerB = placeLectern(helper, ownerBPos);
        helper.setBlock(oldTarget, Blocks.CHEST);
        ownerA.addHandlerPos(ownerA, oldTargetWorld);
        ownerB.addHandlerPos(ownerB, oldTargetWorld);

        SubLevelAssemblyHelper.moveBlocks(level, new SubLevelAssemblyHelper.AssemblyTransform(oldTargetWorld, newTargetWorld, 0, Rotation.NONE, level), List.of(oldTargetWorld));

        helper.assertTrue(ownerA.handlerPosList.stream().anyMatch(handlerPos -> handlerPos.pos().equals(newTargetWorld)), "First lectern did not move shared target");
        helper.assertTrue(ownerB.handlerPosList.stream().anyMatch(handlerPos -> handlerPos.pos().equals(newTargetWorld)), "Second lectern did not move shared target");
        helper.assertTrue(ownerA.handlerPosList.stream().noneMatch(handlerPos -> handlerPos.pos().equals(oldTargetWorld)), "First lectern kept stale shared target");
        helper.assertTrue(ownerB.handlerPosList.stream().noneMatch(handlerPos -> handlerPos.pos().equals(oldTargetWorld)), "Second lectern kept stale shared target");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE_EMPTY)
    public static void wipedSharedConnectionKeepsOtherLecternTracking(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos ownerAPos = new BlockPos(2, 1, 2);
        BlockPos ownerBPos = new BlockPos(2, 1, 4);
        BlockPos oldTarget = new BlockPos(4, 1, 3);
        BlockPos newTarget = new BlockPos(5, 1, 3);
        BlockPos oldTargetWorld = helper.absolutePos(oldTarget);
        BlockPos newTargetWorld = helper.absolutePos(newTarget);

        StorageLecternTile ownerA = placeLectern(helper, ownerAPos);
        StorageLecternTile ownerB = placeLectern(helper, ownerBPos);
        helper.setBlock(oldTarget, Blocks.CHEST);
        ownerA.addHandlerPos(ownerA, oldTargetWorld);
        ownerB.addHandlerPos(ownerB, oldTargetWorld);

        TrackedBlockEntityPosData data = TrackedBlockEntityPosData.from(level);
        TrackedWorldPositionBlockEntity trackedA = (TrackedWorldPositionBlockEntity) ownerA;
        TrackedWorldPositionBlockEntity trackedB = (TrackedWorldPositionBlockEntity) ownerB;

        ownerA.onFinishedConnectionLast(oldTargetWorld, Direction.UP, null, ANFakePlayer.getPlayer(level));
        helper.assertTrue(data.getBlockEntityPos(trackedA.ars_sable$getTrackingId()) == null, "Wiped lectern kept tracked entry");
        helper.assertTrue(data.getTrackedPositions(trackedB.ars_sable$getTrackingId()).contains(oldTargetWorld), "Other lectern lost shared target tracking");

        SubLevelAssemblyHelper.moveBlocks(level, new SubLevelAssemblyHelper.AssemblyTransform(oldTargetWorld, newTargetWorld, 0, Rotation.NONE, level), List.of(oldTargetWorld));

        helper.assertTrue(ownerA.handlerPosList.stream().noneMatch(handlerPos -> handlerPos.pos().equals(newTargetWorld)), "Wiped lectern tracked moved shared target");
        helper.assertTrue(ownerB.handlerPosList.stream().anyMatch(handlerPos -> handlerPos.pos().equals(newTargetWorld)), "Other lectern did not move shared target");
        helper.assertTrue(ownerB.handlerPosList.stream().noneMatch(handlerPos -> handlerPos.pos().equals(oldTargetWorld)), "Other lectern kept stale shared target");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE_EMPTY)
    public static void brokenSharedLecternKeepsOtherLecternTracking(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos ownerAPos = new BlockPos(2, 1, 2);
        BlockPos ownerBPos = new BlockPos(2, 1, 4);
        BlockPos oldTarget = new BlockPos(4, 1, 3);
        BlockPos newTarget = new BlockPos(5, 1, 3);
        BlockPos oldTargetWorld = helper.absolutePos(oldTarget);
        BlockPos newTargetWorld = helper.absolutePos(newTarget);

        StorageLecternTile ownerA = placeLectern(helper, ownerAPos);
        StorageLecternTile ownerB = placeLectern(helper, ownerBPos);
        helper.setBlock(oldTarget, Blocks.CHEST);
        ownerA.addHandlerPos(ownerA, oldTargetWorld);
        ownerB.addHandlerPos(ownerB, oldTargetWorld);

        TrackedBlockEntityPosData data = TrackedBlockEntityPosData.from(level);
        TrackedWorldPositionBlockEntity trackedA = (TrackedWorldPositionBlockEntity) ownerA;
        TrackedWorldPositionBlockEntity trackedB = (TrackedWorldPositionBlockEntity) ownerB;

        helper.setBlock(ownerAPos, Blocks.AIR);
        helper.assertTrue(data.getBlockEntityPos(trackedA.ars_sable$getTrackingId()) == null, "Broken lectern kept tracked entry");
        helper.assertTrue(data.getTrackedPositions(trackedA.ars_sable$getTrackingId()).isEmpty(), "Broken lectern kept tracked positions");
        helper.assertTrue(data.getTrackedPositions(trackedB.ars_sable$getTrackingId()).contains(oldTargetWorld), "Other lectern lost shared target tracking");

        SubLevelAssemblyHelper.moveBlocks(level, new SubLevelAssemblyHelper.AssemblyTransform(oldTargetWorld, newTargetWorld, 0, Rotation.NONE, level), List.of(oldTargetWorld));

        helper.assertTrue(ownerB.handlerPosList.stream().anyMatch(handlerPos -> handlerPos.pos().equals(newTargetWorld)), "Other lectern did not move shared target");
        helper.assertTrue(ownerB.handlerPosList.stream().noneMatch(handlerPos -> handlerPos.pos().equals(oldTargetWorld)), "Other lectern kept stale shared target");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE_EMPTY)
    public static void brokenLecternRemovesTrackedPositions(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos ownerPos = new BlockPos(2, 1, 7);
        BlockPos target = new BlockPos(4, 1, 7);
        BlockPos targetWorld = helper.absolutePos(target);

        StorageLecternTile owner = placeLectern(helper, ownerPos);
        helper.setBlock(target, Blocks.CHEST);
        owner.addHandlerPos(owner, targetWorld);

        TrackedBlockEntityPosData data = TrackedBlockEntityPosData.from(level);
        TrackedWorldPositionBlockEntity trackedOwner = (TrackedWorldPositionBlockEntity) owner;
        helper.assertTrue(data.getBlockEntityPos(trackedOwner.ars_sable$getTrackingId()) != null, "Expected tracked lectern entry before break");
        helper.assertTrue(data.getTrackedPositions(trackedOwner.ars_sable$getTrackingId()).contains(targetWorld), "Expected tracked connection before break");

        helper.setBlock(ownerPos, Blocks.AIR);

        helper.assertTrue(data.getBlockEntityPos(trackedOwner.ars_sable$getTrackingId()) == null, "Broken lectern kept tracked entry");
        helper.assertTrue(data.getTrackedPositions(trackedOwner.ars_sable$getTrackingId()).isEmpty(), "Broken lectern kept tracked positions");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE_EMPTY)
    public static void wipedLecternConnectionDoesNotTrackMovedFormerTarget(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos ownerPos = new BlockPos(2, 1, 8);
        BlockPos oldTarget = new BlockPos(4, 1, 8);
        BlockPos newTarget = new BlockPos(5, 1, 8);
        BlockPos oldTargetWorld = helper.absolutePos(oldTarget);
        BlockPos newTargetWorld = helper.absolutePos(newTarget);

        StorageLecternTile owner = placeLectern(helper, ownerPos);
        helper.setBlock(oldTarget, Blocks.CHEST);
        owner.addHandlerPos(owner, oldTargetWorld);

        TrackedBlockEntityPosData data = TrackedBlockEntityPosData.from(level);
        TrackedWorldPositionBlockEntity trackedOwner = (TrackedWorldPositionBlockEntity) owner;
        helper.assertTrue(data.getTrackedPositions(trackedOwner.ars_sable$getTrackingId()).contains(oldTargetWorld), "Expected tracked connection before wipe");

        owner.onFinishedConnectionLast(oldTargetWorld, Direction.UP, null, ANFakePlayer.getPlayer(level));
        helper.assertTrue(owner.handlerPosList.stream().noneMatch(handlerPos -> handlerPos.pos().equals(oldTargetWorld)), "Dominion wand wipe kept handler target");
        helper.assertTrue(data.getBlockEntityPos(trackedOwner.ars_sable$getTrackingId()) == null, "Wiped lectern kept tracked entry");
        helper.assertTrue(data.getTrackedPositions(trackedOwner.ars_sable$getTrackingId()).isEmpty(), "Wiped lectern kept tracked positions");

        SubLevelAssemblyHelper.moveBlocks(level, new SubLevelAssemblyHelper.AssemblyTransform(oldTargetWorld, newTargetWorld, 0, Rotation.NONE, level), List.of(oldTargetWorld));

        helper.assertTrue(owner.handlerPosList.stream().noneMatch(handlerPos -> handlerPos.pos().equals(newTargetWorld)), "Former target was tracked after wipe");
        helper.assertTrue(data.getBlockEntityPos(trackedOwner.ars_sable$getTrackingId()) == null, "Former target move recreated tracked entry");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE_EMPTY)
    public static void movedHandlerPositionClearsQueuedTransferTasks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos ownerPos = new BlockPos(2, 1, 6);
        BlockPos oldTarget = new BlockPos(4, 1, 6);
        BlockPos newTarget = new BlockPos(5, 1, 6);
        BlockPos oldTargetWorld = helper.absolutePos(oldTarget);
        BlockPos newTargetWorld = helper.absolutePos(newTarget);

        StorageLecternTile owner = placeLectern(helper, ownerPos);
        helper.setBlock(oldTarget, Blocks.CHEST);
        owner.addHandlerPos(owner, oldTargetWorld);
        owner.transferTasks.add(new TransferTask(oldTargetWorld, owner.getBlockPos(), ItemStack.EMPTY, level.getGameTime()));
        owner.transferTasks.add(new TransferTask(owner.getBlockPos(), oldTargetWorld, ItemStack.EMPTY, level.getGameTime()));

        SubLevelAssemblyHelper.moveBlocks(level, new SubLevelAssemblyHelper.AssemblyTransform(oldTargetWorld, newTargetWorld, 0, Rotation.NONE, level), List.of(oldTargetWorld));

        helper.assertTrue(owner.transferTasks.isEmpty(), "Queued transfer tasks were not cleared");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE_EMPTY)
    public static void movedMainLecternPositionUpdatesLoadedLectern(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos ownerPos = new BlockPos(2, 1, 4);
        BlockPos oldMain = new BlockPos(4, 1, 4);
        BlockPos newMain = new BlockPos(5, 1, 4);
        BlockPos oldMainWorld = helper.absolutePos(oldMain);
        BlockPos newMainWorld = helper.absolutePos(newMain);

        StorageLecternTile owner = placeLectern(helper, ownerPos);
        placeLectern(helper, oldMain);
        owner.mainLecternPos = oldMainWorld;
        owner.onLoad();

        SubLevelAssemblyHelper.moveBlocks(level, new SubLevelAssemblyHelper.AssemblyTransform(oldMainWorld, newMainWorld, 0, Rotation.NONE, level), List.of(oldMainWorld));

        helper.assertTrue(newMainWorld.equals(owner.mainLecternPos), "Expected main lectern position to move to " + newMainWorld + ", actual=" + owner.mainLecternPos);
        helper.assertTrue(owner.handlerPosList.stream().noneMatch(handlerPos -> handlerPos.pos().equals(newMainWorld)), "Moved main lectern was added as a handler at " + newMainWorld);
        helper.assertTrue(owner.handlerPosList.stream().noneMatch(handlerPos -> handlerPos.pos().equals(oldMainWorld)), "Stale main lectern position was kept as a handler at " + oldMainWorld);
        helper.succeed();
    }

    private static StorageLecternTile placeLectern(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, BlockRegistry.CRAFTING_LECTERN.get().defaultBlockState());
        if (helper.getBlockEntity(pos) instanceof StorageLecternTile lectern) {
            return lectern;
        }
        helper.fail("Expected storage lectern at " + pos);
        throw new IllegalStateException();
    }

}
