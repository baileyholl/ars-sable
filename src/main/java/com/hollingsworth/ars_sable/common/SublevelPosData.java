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
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SublevelPosData extends SavedData {
    // Player -> the floor block they stood on when entering the planarium
    private final Map<UUID, Entry> entries = new HashMap<>();
    // Current tracked position -> players whose entries should update when that block moves.
    private final Map<GlobalPos, HashSet<UUID>> trackedPosToPlayers = new HashMap<>();

    private static final Codec<Map.Entry<UUID, Entry>> ENTRY_MAPPING_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("key").forGetter(Map.Entry::getKey),
            Entry.CODEC.codec().fieldOf("data").forGetter(Map.Entry::getValue)
    ).apply(instance, Map::entry));

    public void put(UUID playerId, GlobalPos trackedFloorPos, Vec2 rot, GlobalPos fallbackPos) {
        removePlayer(playerId);
        entries.put(playerId, new Entry(new JarDimData.RotPos(trackedFloorPos, rot), trackedFloorPos, fallbackPos, Optional.empty()));
        trackedPosToPlayers.computeIfAbsent(trackedFloorPos, ignored -> new HashSet<>()).add(playerId);
    }

    public @Nullable Entry getForPlayer(UUID id) {
        return entries.get(id);
    }

    // Returns the pos transformed relative to where the player entered the planarium from, wherever
    // that position may be now: projected out of the sublevel if it is still on one, used directly
    // if its blocks returned to the real world, or the recorded fallback if the sublevel is gone.
    public @Nullable GlobalPos getTransformedPos(ServerLevel serverLevel, Player player) {
        return getTransformedPos(serverLevel, player.getUUID());
    }

    public @Nullable GlobalPos getTransformedPos(ServerLevel serverLevel, UUID playerId) {
        Entry entry = entries.get(playerId);
        if (entry == null) {
            return null;
        }
        if (entry.unloaded().isPresent()) {
            return entry.unloaded().get().restorePos();
        }
        GlobalPos tracked = entry.trackedPos();
        ServerLevel trackedLevel = serverLevel.getServer().getLevel(tracked.dimension());
        if (trackedLevel == null) {
            return null;
        }
        if (SableCompanion.INSTANCE.getContaining(trackedLevel, tracked.pos()) == null) {
            // The tracked floor block is no longer on a sublevel. If it moved, it was disassembled
            // back into the world and the player stands on top of it; otherwise the sublevel was
            // destroyed and the recorded fallback is used.
            return tracked.equals(entry.originalPos()) ? entry.fallbackPos() : GlobalPos.of(tracked.dimension(), tracked.pos().above());
        }
        return GlobalPos.of(tracked.dimension(), SableProjectionHelper.projectStandingPos(trackedLevel, tracked.pos()));
    }

    public void handleBlockMoved(ServerLevel level, BlockPos oldPos, BlockPos newPos) {
        GlobalPos oldTracked = GlobalPos.of(level.dimension(), oldPos);
        HashSet<UUID> players = trackedPosToPlayers.remove(oldTracked);
        if (players == null || players.isEmpty()) {
            return;
        }
        GlobalPos newTracked = GlobalPos.of(level.dimension(), newPos.immutable());
        for (UUID playerId : players) {
            Entry entry = entries.get(playerId);
            // Unloaded entries keep their grid position frozen until their sublevel returns.
            if (entry == null || entry.unloaded().isPresent() || !entry.trackedPos().equals(oldTracked)) {
                trackedPosToPlayers.computeIfAbsent(oldTracked, ignored -> new HashSet<>()).add(playerId);
                continue;
            }
            moveTrackedPos(playerId, entry, newTracked);
        }
    }

    public void setSublevelLoaded(ServerLevel serverLevel, UUID sublevelId, boolean isLoaded) {
        for (Map.Entry<UUID, Entry> mapEntry : Map.copyOf(entries).entrySet()) {
            Entry entry = mapEntry.getValue();
            if (isLoaded) {
                if (entry.unloaded().isEmpty() || !entry.unloaded().get().sublevelId().equals(sublevelId)) {
                    continue;
                }
                entries.put(mapEntry.getKey(), entry.withUnloaded(Optional.empty()));
            } else {
                if (entry.unloaded().isPresent() || !isOnSublevel(serverLevel, entry, sublevelId)) {
                    continue;
                }
                // Snapshot the projected position now; the sublevel transform is unavailable while unloaded.
                GlobalPos restorePos = GlobalPos.of(entry.trackedPos().dimension(), SableProjectionHelper.projectStandingPos(serverLevel, entry.trackedPos().pos()));
                entries.put(mapEntry.getKey(), entry.withUnloaded(Optional.of(new Unloaded(sublevelId, restorePos))));
            }
        }
    }

    public void removeSublevel(ServerLevel serverLevel, UUID sublevelId) {
        for (Map.Entry<UUID, Entry> mapEntry : Map.copyOf(entries).entrySet()) {
            UUID playerId = mapEntry.getKey();
            Entry entry = mapEntry.getValue();
            if (entry.unloaded().isPresent()) {
                if (!entry.unloaded().get().sublevelId().equals(sublevelId)) {
                    continue;
                }
                // Removed while unloaded; pin the entry to the snapshot taken at unload time. The
                // snapshot is a feet-level position, so step down to keep tracking a floor block.
                GlobalPos snapshot = entry.unloaded().get().restorePos();
                moveTrackedPos(playerId, entry.withUnloaded(Optional.empty()), GlobalPos.of(snapshot.dimension(), snapshot.pos().below()));
                continue;
            }
            if (!isOnSublevel(serverLevel, entry, sublevelId)) {
                continue;
            }
            // The sublevel is going away without returning this block to the world; pin the entry
            // to the floor block below its current world projection so the player still exits somewhere sane.
            BlockPos standingPos = SableProjectionHelper.projectStandingPos(serverLevel, entry.trackedPos().pos());
            moveTrackedPos(playerId, entry, GlobalPos.of(entry.trackedPos().dimension(), standingPos.below()));
        }
    }

    public void removePlayer(UUID playerId) {
        Entry entry = entries.remove(playerId);
        if (entry == null) {
            return;
        }
        HashSet<UUID> players = trackedPosToPlayers.get(entry.trackedPos());
        if (players != null) {
            players.remove(playerId);
            if (players.isEmpty()) {
                trackedPosToPlayers.remove(entry.trackedPos());
            }
        }
    }

    private void moveTrackedPos(UUID playerId, Entry entry, GlobalPos newTracked) {
        HashSet<UUID> players = trackedPosToPlayers.get(entry.trackedPos());
        if (players != null) {
            players.remove(playerId);
            if (players.isEmpty()) {
                trackedPosToPlayers.remove(entry.trackedPos());
            }
        }
        entries.put(playerId, entry.withTrackedPos(newTracked));
        trackedPosToPlayers.computeIfAbsent(newTracked, ignored -> new HashSet<>()).add(playerId);
    }

    private boolean isOnSublevel(ServerLevel serverLevel, Entry entry, UUID sublevelId) {
        if (!entry.trackedPos().dimension().equals(serverLevel.dimension())) {
            return false;
        }
        SubLevelAccess containing = SableCompanion.INSTANCE.getContaining(serverLevel, entry.trackedPos().pos());
        return containing != null && containing.getUniqueId().equals(sublevelId);
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
        entries.forEach((playerId, entry) -> trackedPosToPlayers.computeIfAbsent(entry.trackedPos(), ignored -> new HashSet<>()).add(playerId));
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

    public record Unloaded(UUID sublevelId, GlobalPos restorePos) {
        public static final Codec<Unloaded> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("sublevelId").forGetter(Unloaded::sublevelId),
                GlobalPos.CODEC.fieldOf("restorePos").forGetter(Unloaded::restorePos)
        ).apply(instance, Unloaded::new));
    }

    public record Entry(JarDimData.RotPos rotPos, GlobalPos originalPos, GlobalPos fallbackPos, Optional<Unloaded> unloaded) {
        public static final MapCodec<Entry> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                JarDimData.RotPos.CODEC.codec().fieldOf("rotPos").forGetter(Entry::rotPos),
                GlobalPos.CODEC.fieldOf("originalPos").forGetter(Entry::originalPos),
                GlobalPos.CODEC.fieldOf("fallbackPos").forGetter(Entry::fallbackPos),
                Unloaded.CODEC.optionalFieldOf("unloaded").forGetter(Entry::unloaded)
        ).apply(instance, Entry::new));

        public GlobalPos trackedPos() {
            return rotPos.pos();
        }

        public Entry withTrackedPos(GlobalPos trackedPos) {
            return new Entry(new JarDimData.RotPos(trackedPos, rotPos.rot()), originalPos, fallbackPos, unloaded);
        }

        public Entry withUnloaded(Optional<Unloaded> unloaded) {
            return new Entry(rotPos, originalPos, fallbackPos, unloaded);
        }
    }
}
