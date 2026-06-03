package com.hollingsworth.ars_sable.common;

import com.hollingsworth.arsnouveau.common.world.saved_data.JarDimData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;
import java.util.stream.Collectors;

public class SublevelPosData extends SavedData {
    private final Map<UUID, Entry> PLAYER_TO_PLANARIUM_SUBLEVEL = new HashMap<>();
    private final Map<UUID, HashSet<UUID>> SUBLEVEL_TO_PLAYERS = new HashMap<>();

    private static final Codec<Map<UUID, Entry>> ENTRIES_CODEC = KeyValuePair.CODEC.listOf()
            .xmap(
                    list -> list.stream().collect(Collectors.toMap(KeyValuePair::key, KeyValuePair::value)),
                    map -> map.entrySet().stream().map(e -> new KeyValuePair(e.getKey(), e.getValue())).toList()
            );

    private static final Codec<Map<UUID, HashSet<UUID>>> SUBLEVELS_CODEC = SublevelEntry.CODEC.listOf()
            .xmap(
                    list -> list.stream().collect(Collectors.toMap(SublevelEntry::sublevel, SublevelEntry::keys)),
                    map -> map.entrySet().stream().map(e -> new SublevelEntry(e.getKey(), e.getValue())).toList()
            );

    public static final Codec<SublevelPosData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ENTRIES_CODEC.fieldOf("entries").forGetter(d -> d.PLAYER_TO_PLANARIUM_SUBLEVEL),
            SUBLEVELS_CODEC.fieldOf("sublevels").forGetter(d -> d.SUBLEVEL_TO_PLAYERS)
    ).apply(instance, (entries, sublevels) -> {
        SublevelPosData data = new SublevelPosData();
        data.PLAYER_TO_PLANARIUM_SUBLEVEL.putAll(entries);
        data.SUBLEVEL_TO_PLAYERS.putAll(sublevels);
        return data;
    }));

    public void put(UUID playerId, UUID sublevelId, BlockPos plotPos, JarDimData.RotPos rotPos) {
        PLAYER_TO_PLANARIUM_SUBLEVEL.put(playerId, new Entry(sublevelId, plotPos, rotPos));
        SUBLEVEL_TO_PLAYERS.computeIfAbsent(sublevelId, k -> new HashSet<>()).add(playerId);
    }

    public Entry getForPlayer(UUID id){
        return PLAYER_TO_PLANARIUM_SUBLEVEL.get(id);
    }

    public void removeSublevel(ServerLevel serverLevel, UUID sublevel) {

    }

    public static SavedData.Factory<SublevelPosData> factory() {
        return new SavedData.Factory<>(SublevelPosData::new, SublevelPosData::load, null);
    }

    public static SublevelPosData load(CompoundTag tag, HolderLookup.Provider p) {
        return CODEC.parse(NbtOps.INSTANCE, tag).resultOrPartial(e -> {}).orElseGet(SublevelPosData::new);
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag pCompoundTag, HolderLookup.Provider pRegistries) {
        pCompoundTag.merge((CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow());
        return pCompoundTag;
    }

    public static SublevelPosData from(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(factory(), "fs_sublevel_pos");
    }

    private record KeyValuePair(UUID key, Entry value) {
        static final Codec<KeyValuePair> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("key").forGetter(KeyValuePair::key),
                Entry.CODEC.codec().fieldOf("data").forGetter(KeyValuePair::value)
        ).apply(instance, KeyValuePair::new));
    }

    private record SublevelEntry(UUID sublevel, HashSet<UUID> keys) {
        static final Codec<SublevelEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("sublevel").forGetter(SublevelEntry::sublevel),
                UUIDUtil.CODEC.listOf()
                        .xmap(HashSet::new, List::copyOf)
                        .fieldOf("keys").forGetter(SublevelEntry::keys)
        ).apply(instance, SublevelEntry::new));
    }

    public record Entry(UUID sublevelId, BlockPos localSublevelPos, JarDimData.RotPos rotPos) {
        public static final MapCodec<Entry> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("sublevelId").forGetter(Entry::sublevelId),
                BlockPos.CODEC.fieldOf("localSublevelPos").forGetter(Entry::localSublevelPos),
                JarDimData.RotPos.CODEC.fieldOf("rotPos").forGetter(Entry::rotPos)
        ).apply(instance, Entry::new));
    }
}
