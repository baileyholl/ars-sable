package com.hollingsworth.ars_sable.common.gametest;

import com.hollingsworth.ars_sable.ArsSable;
import com.hollingsworth.ars_sable.common.TrackedBlockEntityPosData;
import com.hollingsworth.ars_sable.common.sable.TrackedWorldPositionBlockEntity;
import com.hollingsworth.arsnouveau.common.block.tile.StorageLecternTile;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@GameTestHolder(ArsSable.MODID)
@PrefixGameTestTemplate(false)
public class TrackedBlockEntityPosDataTests {

    @GameTest(template = StorageLecternTests.TEMPLATE_EMPTY)
    public static void syncStoresEntryAndTrackedPositions(GameTestHelper helper) {
        TrackedBlockEntityPosData data = new TrackedBlockEntityPosData();
        UUID id = UUID.randomUUID();
        BlockPos ownerPos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos targetA = helper.absolutePos(new BlockPos(2, 1, 1));
        BlockPos targetB = helper.absolutePos(new BlockPos(3, 1, 1));

        data.sync(id, ownerPos, List.of(targetA, targetB, targetA));

        helper.assertTrue(data.getEntry(id).blockEntityPos().equals(ownerPos), "Expected entry position " + ownerPos);
        helper.assertTrue(data.getTrackedPositions(id).equals(Set.of(targetA, targetB)), "Expected unique tracked positions");
        helper.succeed();
    }

    @GameTest(template = StorageLecternTests.TEMPLATE_EMPTY)
    public static void setTrackedPositionsReplacesIndexes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TrackedBlockEntityPosData data = new TrackedBlockEntityPosData();
        UUID id = UUID.randomUUID();
        BlockPos ownerPos = helper.absolutePos(new BlockPos(1, 1, 2));
        BlockPos oldTarget = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos keptTarget = helper.absolutePos(new BlockPos(3, 1, 2));
        BlockPos newTarget = helper.absolutePos(new BlockPos(4, 1, 2));
        BlockPos movedOldTarget = helper.absolutePos(new BlockPos(5, 1, 2));
        BlockPos movedKeptTarget = helper.absolutePos(new BlockPos(6, 1, 2));

        data.sync(id, ownerPos, List.of(oldTarget, keptTarget));
        data.setTrackedPositions(id, List.of(keptTarget, newTarget));
        data.handleBlockMoved(level, oldTarget, movedOldTarget);
        data.handleBlockMoved(level, keptTarget, movedKeptTarget);

        Set<BlockPos> positions = data.getTrackedPositions(id);
        helper.assertTrue(!positions.contains(oldTarget), "Old target remained tracked");
        helper.assertTrue(!positions.contains(movedOldTarget), "Removed target index was still active");
        helper.assertTrue(positions.contains(newTarget), "New target was not tracked");
        helper.assertTrue(positions.contains(movedKeptTarget), "Kept target did not move");
        helper.succeed();
    }

    @GameTest(template = StorageLecternTests.TEMPLATE_EMPTY)
    public static void removeIfAtPositionRequiresMatchingOwnerPosition(GameTestHelper helper) {
        TrackedBlockEntityPosData data = new TrackedBlockEntityPosData();
        UUID id = UUID.randomUUID();
        BlockPos ownerPos = helper.absolutePos(new BlockPos(1, 1, 3));
        BlockPos wrongOwnerPos = helper.absolutePos(new BlockPos(2, 1, 3));
        BlockPos target = helper.absolutePos(new BlockPos(3, 1, 3));

        data.sync(id, ownerPos, List.of(target));
        data.removeIfAtPosition(id, wrongOwnerPos);

        helper.assertTrue(data.getEntry(id) != null, "Entry was removed from the wrong owner position");

        data.removeIfAtPosition(id, ownerPos);

        helper.assertTrue(data.getEntry(id) == null, "Entry was not removed from matching owner position");
        helper.assertTrue(data.getTrackedPositions(id).isEmpty(), "Tracked positions were not removed");
        helper.succeed();
    }

    @GameTest(template = StorageLecternTests.TEMPLATE_EMPTY)
    public static void handleBlockMovedUpdatesEntryAndTrackedPositions(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TrackedBlockEntityPosData data = new TrackedBlockEntityPosData();
        UUID ownerId = UUID.randomUUID();
        UUID trackerId = UUID.randomUUID();
        BlockPos oldOwnerPos = helper.absolutePos(new BlockPos(1, 1, 4));
        BlockPos newOwnerPos = helper.absolutePos(new BlockPos(2, 1, 4));
        BlockPos trackerPos = helper.absolutePos(new BlockPos(3, 1, 4));
        BlockPos oldTarget = helper.absolutePos(new BlockPos(4, 1, 4));
        BlockPos newTarget = helper.absolutePos(new BlockPos(5, 1, 4));

        data.sync(ownerId, oldOwnerPos, List.of());
        data.sync(trackerId, trackerPos, List.of(oldTarget));
        data.handleBlockMoved(level, oldOwnerPos, newOwnerPos);
        data.handleBlockMoved(level, oldTarget, newTarget);

        helper.assertTrue(data.getEntry(ownerId).blockEntityPos().equals(newOwnerPos), "Moved owner entry was not updated");
        helper.assertTrue(data.getTrackedPositions(trackerId).equals(Set.of(newTarget)), "Moved tracked position was not updated");
        helper.succeed();
    }

    @GameTest(template = StorageLecternTests.TEMPLATE_EMPTY)
    public static void handleBlockMovedUpdatesLoadedTrackedBlockEntity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TrackedBlockEntityPosData data = new TrackedBlockEntityPosData();
        BlockPos ownerPos = new BlockPos(1, 1, 5);
        BlockPos oldTarget = helper.absolutePos(new BlockPos(2, 1, 5));
        BlockPos newTarget = helper.absolutePos(new BlockPos(3, 1, 5));

        helper.setBlock(ownerPos, BlockRegistry.CRAFTING_LECTERN.get().defaultBlockState());
        StorageLecternTile owner = helper.getBlockEntity(ownerPos);
        owner.handlerPosList.add(new StorageLecternTile.HandlerPos(oldTarget, null));

        UUID id = ((TrackedWorldPositionBlockEntity) owner).ars_sable$getTrackingId();
        data.sync(id, owner.getBlockPos(), List.of(oldTarget));
        data.handleBlockMoved(level, oldTarget, newTarget);

        helper.assertTrue(owner.handlerPosList.stream().anyMatch(handlerPos -> handlerPos.pos().equals(newTarget)), "Loaded block entity was not updated");
        helper.assertTrue(owner.handlerPosList.stream().noneMatch(handlerPos -> handlerPos.pos().equals(oldTarget)), "Loaded block entity kept stale target");
        helper.succeed();
    }
}
