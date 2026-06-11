package com.hollingsworth.ars_sable.common;

import com.hollingsworth.ars_sable.common.helper.SableProjectionHelper;
import com.hollingsworth.arsnouveau.common.world.saved_data.JarDimData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec2;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SublevelPosData extends SavedData {
    // Player -> the floor block they stood on when entering the planarium
    private final Map<UUID, Entry> entries = new HashMap<>();
    // Current tracked position -> players whose entries should update when that block moves
    private final TrackedPosIndex<GlobalPos, UUID> trackedPosToPlayers = new TrackedPosIndex<>();
    // Containing sublevel id -> players whose entries are currently on that sublevel
    private final TrackedPosIndex<UUID, UUID> sublevelToPlayers = new TrackedPosIndex<>();

    private static final Codec<Map.Entry<UUID, Entry>> ENTRY_MAPPING_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("key").forGetter(Map.Entry::getKey),
            Entry.CODEC.codec().fieldOf("data").forGetter(Map.Entry::getValue)
    ).apply(instance, Map::entry));

    public void put(ServerLevel level, UUID playerId, GlobalPos trackedFloorPos, Vec2 rot, GlobalPos fallbackPos) {
        removePlayer(playerId);
        Optional<UUID> sublevelId = containingSublevelId(level, trackedFloorPos.pos());
        entries.put(playerId, new Entry(new JarDimData.RotPos(trackedFloorPos, rot), trackedFloorPos, fallbackPos, sublevelId, Optional.empty()));
        trackedPosToPlayers.add(trackedFloorPos, playerId);
        sublevelId.ifPresent(id -> sublevelToPlayers.add(id, playerId));
    }

    public @Nullable Entry getForPlayer(UUID id) {
        return entries.get(id);
    }

    // Returns the pos transformed by any sublevels if applicable
    public @Nullable GlobalPos getTransformedPos(ServerLevel serverLevel, Player player) {
        return getTransformedPos(serverLevel, player.getUUID());
    }

    public @Nullable GlobalPos getTransformedPos(ServerLevel serverLevel, UUID playerId) {
        Entry entry = entries.get(playerId);
        if (entry == null) {
            return null;
        }
        if (entry.restorePos().isPresent()) {
            return entry.restorePos().get();
        }
        GlobalPos tracked = entry.trackedPos();
        ServerLevel trackedLevel = serverLevel.getServer().getLevel(tracked.dimension());
        if (trackedLevel == null) {
            return null;
        }
        if (SableCompanion.INSTANCE.getContaining(trackedLevel, tracked.pos()) == null) {
            // The tracked floor block is no longer on a sublevel
            return tracked.equals(entry.originalPos()) ? entry.fallbackPos() : GlobalPos.of(tracked.dimension(), tracked.pos().above());
        }
        return GlobalPos.of(tracked.dimension(), SableProjectionHelper.projectStandingPos(trackedLevel, tracked.pos()));
    }

    public void handleBlockMoved(ServerLevel level, BlockPos oldPos, BlockPos newPos) {
        GlobalPos oldTracked = GlobalPos.of(level.dimension(), oldPos);
        GlobalPos newTracked = GlobalPos.of(level.dimension(), newPos);
        trackedPosToPlayers.moveAll(oldTracked, newTracked, playerId -> {
            Entry entry = entries.get(playerId);
            // Keep the entry
            if (entry == null || entry.restorePos().isPresent() || !entry.trackedPos().equals(oldTracked)) {
                return false;
            }
            Optional<UUID> newSublevelId = containingSublevelId(level, newPos);
            if (!entry.sublevelId().equals(newSublevelId)) {
                entry.sublevelId().ifPresent(id -> sublevelToPlayers.remove(id, playerId));
                newSublevelId.ifPresent(id -> sublevelToPlayers.add(id, playerId));
            }
            entries.put(playerId, entry.withTrackedPos(newTracked).withSublevelId(newSublevelId));
            return true;
        });
    }

    public void setSublevelLoaded(ServerLevel serverLevel, UUID sublevelId, boolean isLoaded) {
        for (UUID playerId : sublevelToPlayers.get(sublevelId)) {
            Entry entry = entries.get(playerId);
            if (entry == null) {
                continue;
            }
            if (isLoaded) {
                entries.put(playerId, entry.withRestorePos(Optional.empty()));
            } else if (entry.restorePos().isEmpty()) {
                GlobalPos restorePos = GlobalPos.of(entry.trackedPos().dimension(), SableProjectionHelper.projectStandingPos(serverLevel, entry.trackedPos().pos()));
                entries.put(playerId, entry.withRestorePos(Optional.of(restorePos)));
            }
        }
    }

    public void removeSublevel(ServerLevel serverLevel, UUID sublevelId) {
        for (UUID playerId : sublevelToPlayers.removeAll(sublevelId)) {
            Entry entry = entries.get(playerId);
            if (entry == null) {
                continue;
            }
            Optional<GlobalPos> restorePos = entry.restorePos().or(() ->
                    Optional.of(GlobalPos.of(entry.trackedPos().dimension(), SableProjectionHelper.projectStandingPos(serverLevel, entry.trackedPos().pos()))));
            entries.put(playerId, entry.withSublevelId(Optional.empty()).withRestorePos(restorePos));
        }
    }

    public void removePlayer(UUID playerId) {
        Entry entry = entries.remove(playerId);
        if (entry == null) {
            return;
        }
        trackedPosToPlayers.remove(entry.trackedPos(), playerId);
        entry.sublevelId().ifPresent(id -> sublevelToPlayers.remove(id, playerId));
    }

    private static Optional<UUID> containingSublevelId(ServerLevel level, BlockPos pos) {
        SubLevelAccess containing = SableCompanion.INSTANCE.getContaining(level, pos);
        return containing == null ? Optional.empty() : Optional.of(containing.getUniqueId());
    }

    public static SavedData.Factory<SublevelPosData> factory() {
        return new SavedData.Factory<>(SublevelPosData::new, SublevelPosData::load, null);
    }

    public static SublevelPosData load(CompoundTag tag, HolderLookup.Provider provider) {
        SublevelPosData data = new SublevelPosData();
        ListTag entryList = tag.getList("entries", Tag.TAG_COMPOUND);
        for (Tag value : entryList) {
            ENTRY_MAPPING_CODEC.parse(NbtOps.INSTANCE, value).result().ifPresent(mapping ->
                    data.entries.put(mapping.getKey(), mapping.getValue()));
        }
        data.rebuildIndexes();
        return data;
    }

    private void rebuildIndexes() {
        trackedPosToPlayers.clear();
        sublevelToPlayers.clear();
        entries.forEach((playerId, entry) -> {
            trackedPosToPlayers.add(entry.trackedPos(), playerId);
            entry.sublevelId().ifPresent(id -> sublevelToPlayers.add(id, playerId));
        });
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag entryList = new ListTag();
        entries.forEach((playerId, entry) -> entryList.add(ENTRY_MAPPING_CODEC.encodeStart(NbtOps.INSTANCE, Map.entry(playerId, entry)).getOrThrow()));
        tag.put("entries", entryList);
        return tag;
    }

    public static SublevelPosData from(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(factory(), "fs_sublevel_pos");
    }

    public record Entry(JarDimData.RotPos rotPos, GlobalPos originalPos, GlobalPos fallbackPos, Optional<UUID> sublevelId, Optional<GlobalPos> restorePos) {
        public static final MapCodec<Entry> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                JarDimData.RotPos.CODEC.codec().fieldOf("rotPos").forGetter(Entry::rotPos),
                GlobalPos.CODEC.fieldOf("originalPos").forGetter(Entry::originalPos),
                GlobalPos.CODEC.fieldOf("fallbackPos").forGetter(Entry::fallbackPos),
                UUIDUtil.CODEC.optionalFieldOf("sublevelId").forGetter(Entry::sublevelId),
                GlobalPos.CODEC.optionalFieldOf("restorePos").forGetter(Entry::restorePos)
        ).apply(instance, Entry::new));

        public GlobalPos trackedPos() {
            return rotPos.pos();
        }

        public Entry withTrackedPos(GlobalPos trackedPos) {
            return new Entry(new JarDimData.RotPos(trackedPos, rotPos.rot()), originalPos, fallbackPos, sublevelId, restorePos);
        }

        public Entry withSublevelId(Optional<UUID> sublevelId) {
            return new Entry(rotPos, originalPos, fallbackPos, sublevelId, restorePos);
        }

        public Entry withRestorePos(Optional<GlobalPos> restorePos) {
            return new Entry(rotPos, originalPos, fallbackPos, sublevelId, restorePos);
        }
    }
}
