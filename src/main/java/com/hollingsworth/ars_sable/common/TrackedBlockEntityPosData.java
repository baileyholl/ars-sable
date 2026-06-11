package com.hollingsworth.ars_sable.common;

import com.hollingsworth.ars_sable.common.sable.TrackedWorldPositionBlockEntity;
import com.hollingsworth.arsnouveau.common.util.ANCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class TrackedBlockEntityPosData extends SavedData {
    // Tracking ID from TrackedWorldPositionBlockEntity -> the block entity's position
    private final Map<UUID, BlockPos> blockEntityPosById = new HashMap<>();
    private final Map<UUID, HashSet<BlockPos>> trackedPositions = new HashMap<>();
    // Blockpos back to the TrackedWorldPositionBlockEntity id
    private final TrackedPosIndex<BlockPos, UUID> blockEntityPosIndex = new TrackedPosIndex<>();
    private final TrackedPosIndex<BlockPos, UUID> trackedPositionToEntries = new TrackedPosIndex<>();

    private static final Codec<Map<UUID, BlockPos>> ENTRIES_CODEC =
            uuidKeyedMapCodec("value", BlockPos.CODEC.fieldOf("block_entity_pos").codec());

    private static final Codec<Map<UUID, HashSet<BlockPos>>> TRACKED_POSITIONS_CODEC =
            uuidKeyedMapCodec("positions", BlockPos.CODEC.listOf().xmap(HashSet::new, List::copyOf));

    public static final Codec<TrackedBlockEntityPosData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ENTRIES_CODEC.optionalFieldOf("entries", Map.of()).forGetter(data -> data.blockEntityPosById),
            TRACKED_POSITIONS_CODEC.optionalFieldOf("tracked_positions", Map.of()).forGetter(data -> data.trackedPositions)
    ).apply(instance, (entries, trackedPositions) -> {
        TrackedBlockEntityPosData data = new TrackedBlockEntityPosData();
        data.blockEntityPosById.putAll(entries);
        data.trackedPositions.putAll(trackedPositions);
        data.rebuildIndexes();
        return data;
    }));

    private static <V> Codec<Map<UUID, V>> uuidKeyedMapCodec(String valueFieldName, Codec<V> valueCodec) {
        Codec<Map.Entry<UUID, V>> pairCodec = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("key").forGetter(Map.Entry::getKey),
                valueCodec.fieldOf(valueFieldName).forGetter(Map.Entry::getValue)
        ).apply(instance, Map::entry));
        return pairCodec.listOf().xmap(
                list -> list.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                map -> List.copyOf(map.entrySet())
        );
    }

    private void rebuildIndexes() {
        blockEntityPosIndex.clear();
        trackedPositionToEntries.clear();

        blockEntityPosById.forEach((id, pos) -> blockEntityPosIndex.add(pos, id));

        for (Map.Entry<UUID, HashSet<BlockPos>> entry : trackedPositions.entrySet()) {
            UUID id = entry.getKey();
            for (BlockPos pos : entry.getValue()) {
                trackedPositionToEntries.add(pos, id);
            }
        }
    }

    public void sync(UUID id, BlockPos blockEntityPos, Collection<BlockPos> trackedPositions) {
        BlockPos oldPos = blockEntityPosById.put(id, blockEntityPos);
        if (oldPos != null) {
            blockEntityPosIndex.remove(oldPos, id);
        }
        blockEntityPosIndex.add(blockEntityPos, id);
        setTrackedPositions(id, trackedPositions);
    }

    public void setTrackedPositions(UUID id, Collection<BlockPos> trackedPositions) {
        HashSet<BlockPos> oldPositions = this.trackedPositions.getOrDefault(id, new HashSet<>());
        HashSet<BlockPos> newPositions = new HashSet<>(trackedPositions);

        for (BlockPos oldPos : oldPositions) {
            if (newPositions.contains(oldPos)) {
                continue;
            }
            trackedPositionToEntries.remove(oldPos, id);
        }

        for (BlockPos newPos : newPositions) {
            trackedPositionToEntries.add(newPos, id);
        }

        if (newPositions.isEmpty()) {
            this.trackedPositions.remove(id);
        } else {
            this.trackedPositions.put(id, newPositions);
        }
    }

    public @Nullable BlockPos getBlockEntityPos(UUID id) {
        return blockEntityPosById.get(id);
    }

    public Set<BlockPos> getTrackedPositions(UUID id) {
        HashSet<BlockPos> positions = trackedPositions.get(id);
        return positions == null ? Set.of() : positions;
    }

    public void removeIfAtPosition(UUID id, BlockPos blockEntityPos) {
        if (!blockEntityPos.equals(blockEntityPosById.get(id))) {
            return;
        }
        remove(id);
    }

    public void remove(UUID id) {
        BlockPos removedPos = blockEntityPosById.remove(id);
        if (removedPos != null) {
            blockEntityPosIndex.remove(removedPos, id);
        }

        HashSet<BlockPos> removedPositions = trackedPositions.remove(id);
        if (removedPositions == null) {
            return;
        }

        for (BlockPos trackedPos : removedPositions) {
            trackedPositionToEntries.remove(trackedPos, id);
        }
    }

    public void handleBlockMoved(ServerLevel level, BlockPos oldPos, BlockPos newPos) {
        blockEntityPosIndex.moveAll(oldPos, newPos, id -> {
            if (!oldPos.equals(blockEntityPosById.get(id))) {
                return false;
            }
            blockEntityPosById.put(id, newPos);
            return true;
        });

        trackedPositionToEntries.moveAll(oldPos, newPos, affectedId -> {
            HashSet<BlockPos> positions = trackedPositions.get(affectedId);
            if (positions == null || !positions.remove(oldPos)) {
                return false;
            }
            positions.add(newPos);

            BlockPos blockEntityPos = blockEntityPosById.get(affectedId);
            if (blockEntityPos != null) {
                BlockEntity blockEntity = level.getBlockEntity(blockEntityPos);
                if (blockEntity instanceof TrackedWorldPositionBlockEntity trackedBlockEntity) {
                    trackedBlockEntity.ars_sable$replaceTrackedPosition(oldPos, newPos);
                }
            }
            return true;
        });
    }


    public static SavedData.Factory<TrackedBlockEntityPosData> factory() {
        return new SavedData.Factory<>(TrackedBlockEntityPosData::new, TrackedBlockEntityPosData::load, null);
    }

    public static TrackedBlockEntityPosData load(CompoundTag tag, HolderLookup.Provider provider) {
        return ANCodecs.decode(CODEC, tag);
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.merge((CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow());
        return tag;
    }

    public static TrackedBlockEntityPosData from(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(), "fs_tracked_block_entity_pos");
    }
}
