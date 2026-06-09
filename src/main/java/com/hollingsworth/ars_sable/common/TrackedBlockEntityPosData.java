package com.hollingsworth.ars_sable.common;

import com.hollingsworth.ars_sable.common.sable.TrackedWorldPositionBlockEntity;
import com.hollingsworth.arsnouveau.common.util.ANCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
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
    private final Map<UUID, Entry> entries = new HashMap<>();
    private final Map<UUID, HashSet<BlockPos>> trackedPositions = new HashMap<>();
    private final Map<BlockPos, UUID> blockEntityPositions = new HashMap<>();
    private final Map<BlockPos, HashSet<UUID>> trackedPositionToEntries = new HashMap<>();

    private static final Codec<Map<UUID, Entry>> ENTRIES_CODEC = EntryPair.CODEC.listOf()
            .xmap(
                    list -> list.stream().collect(Collectors.toMap(EntryPair::key, EntryPair::value)),
                    map -> map.entrySet().stream().map(entry -> new EntryPair(entry.getKey(), entry.getValue())).toList()
            );

    private static final Codec<Map<UUID, HashSet<BlockPos>>> TRACKED_POSITIONS_CODEC = TrackedPositionEntry.CODEC.listOf()
            .xmap(
                    list -> list.stream().collect(Collectors.toMap(TrackedPositionEntry::key, TrackedPositionEntry::positions)),
                    map -> map.entrySet().stream().map(entry -> new TrackedPositionEntry(entry.getKey(), entry.getValue())).toList()
            );

    public static final Codec<TrackedBlockEntityPosData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ENTRIES_CODEC.optionalFieldOf("entries", Map.of()).forGetter(data -> data.entries),
            TRACKED_POSITIONS_CODEC.optionalFieldOf("tracked_positions", Map.of()).forGetter(data -> data.trackedPositions)
    ).apply(instance, (entries, trackedPositions) -> {
        TrackedBlockEntityPosData data = new TrackedBlockEntityPosData();
        data.entries.putAll(entries);
        data.trackedPositions.putAll(trackedPositions);
        data.rebuildIndexes();
        return data;
    }));

    private void rebuildIndexes() {
        blockEntityPositions.clear();
        trackedPositionToEntries.clear();

        for (Entry entry : entries.values()) {
            blockEntityPositions.put(entry.blockEntityPos(), entry.id());
        }

        for (Map.Entry<UUID, HashSet<BlockPos>> entry : trackedPositions.entrySet()) {
            UUID id = entry.getKey();
            for (BlockPos pos : entry.getValue()) {
                trackedPositionToEntries.computeIfAbsent(pos, key -> new HashSet<>()).add(id);
            }
        }
    }

    public void sync(UUID id, BlockPos blockEntityPos, Collection<BlockPos> trackedPositions) {
        Entry oldEntry = entries.put(id, new Entry(id, blockEntityPos.immutable()));
        if (oldEntry != null && !oldEntry.blockEntityPos().equals(blockEntityPos)) {
            blockEntityPositions.remove(oldEntry.blockEntityPos());
        }
        blockEntityPositions.put(blockEntityPos.immutable(), id);
        setTrackedPositions(id, trackedPositions);
    }

    public void setTrackedPositions(UUID id, Collection<BlockPos> trackedPositions) {
        HashSet<BlockPos> oldPositions = this.trackedPositions.getOrDefault(id, new HashSet<>());
        HashSet<BlockPos> newPositions = trackedPositions.stream()
                .filter(Objects::nonNull)
                .map(BlockPos::immutable)
                .collect(Collectors.toCollection(HashSet::new));

        for (BlockPos oldPos : oldPositions) {
            if (newPositions.contains(oldPos)) {
                continue;
            }
            HashSet<UUID> ids = trackedPositionToEntries.get(oldPos);
            if (ids != null) {
                ids.remove(id);
                if (ids.isEmpty()) {
                    trackedPositionToEntries.remove(oldPos);
                }
            }
        }

        for (BlockPos newPos : newPositions) {
            trackedPositionToEntries.computeIfAbsent(newPos, key -> new HashSet<>()).add(id);
        }

        this.trackedPositions.put(id, newPositions);
    }

    public @Nullable Entry getEntry(UUID id) {
        return entries.get(id);
    }

    public Set<BlockPos> getTrackedPositions(UUID id) {
        HashSet<BlockPos> positions = trackedPositions.get(id);
        if (positions == null || positions.isEmpty()) {
            return Set.of();
        }
        return positions.stream()
                .map(BlockPos::immutable)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public void removeIfAtPosition(UUID id, BlockPos blockEntityPos) {
        Entry entry = entries.get(id);
        if (entry == null || !entry.blockEntityPos().equals(blockEntityPos)) {
            return;
        }
        remove(id);
    }

    public void remove(UUID id) {
        Entry removedEntry = entries.remove(id);
        if (removedEntry != null) {
            blockEntityPositions.remove(removedEntry.blockEntityPos());
        }

        HashSet<BlockPos> removedPositions = trackedPositions.remove(id);
        if (removedPositions == null) {
            return;
        }

        for (BlockPos trackedPos : removedPositions) {
            HashSet<UUID> ids = trackedPositionToEntries.get(trackedPos);
            if (ids != null) {
                ids.remove(id);
                if (ids.isEmpty()) {
                    trackedPositionToEntries.remove(trackedPos);
                }
            }
        }
    }

    public void handleBlockMoved(ServerLevel level, BlockPos oldPos, BlockPos newPos) {
        UUID blockEntityId = blockEntityPositions.remove(oldPos);
        if (blockEntityId != null) {
            Entry entry = entries.get(blockEntityId);
            if (entry != null) {
                Entry movedEntry = new Entry(blockEntityId, newPos.immutable());
                entries.put(blockEntityId, movedEntry);
                blockEntityPositions.put(newPos.immutable(), blockEntityId);
            }
        }

        HashSet<UUID> affectedIds = trackedPositionToEntries.remove(oldPos);
        if (affectedIds == null || affectedIds.isEmpty()) {
            return;
        }

        for (UUID affectedId : List.copyOf(affectedIds)) {
            HashSet<BlockPos> positions = trackedPositions.get(affectedId);
            if (positions == null) {
                continue;
            }
            positions.remove(oldPos);
            positions.add(newPos.immutable());
            trackedPositionToEntries.computeIfAbsent(newPos.immutable(), key -> new HashSet<>()).add(affectedId);

            Entry entry = entries.get(affectedId);
            if (entry == null) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(entry.blockEntityPos());
            if (blockEntity instanceof TrackedWorldPositionBlockEntity trackedBlockEntity) {
                trackedBlockEntity.ars_sable$replaceTrackedPosition(oldPos, newPos);
            }
        }
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
        return level.getServer().overworld().getDataStorage().computeIfAbsent(factory(), "fs_tracked_block_entity_pos");
    }

    private record EntryPair(UUID key, Entry value) {
        static final Codec<EntryPair> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("key").forGetter(EntryPair::key),
                Entry.CODEC.codec().fieldOf("value").forGetter(EntryPair::value)
        ).apply(instance, EntryPair::new));
    }

    private record TrackedPositionEntry(UUID key, HashSet<BlockPos> positions) {
        static final Codec<TrackedPositionEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("key").forGetter(TrackedPositionEntry::key),
                BlockPos.CODEC.listOf()
                        .xmap(HashSet::new, List::copyOf)
                        .fieldOf("positions").forGetter(TrackedPositionEntry::positions)
        ).apply(instance, TrackedPositionEntry::new));
    }

    public record Entry(UUID id, BlockPos blockEntityPos) {
        public static final MapCodec<Entry> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Entry::id),
                BlockPos.CODEC.fieldOf("block_entity_pos").forGetter(Entry::blockEntityPos)
        ).apply(instance, Entry::new));
    }
}



