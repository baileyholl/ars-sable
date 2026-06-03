package com.hollingsworth.ars_sable.common;

import com.hollingsworth.arsnouveau.common.world.saved_data.DimMappingData;
import com.hollingsworth.arsnouveau.common.world.saved_data.JarDimData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
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

    // Returns the pos transformed relative to the planarium in the real world, wherever the planarium may be now.
    public @Nullable GlobalPos getTransformedPos(ServerLevel serverLevel, Player player){
        var entry = getForPlayer(player.getUUID());
        if(entry == null){
            return null;
        }
        ServerLevel originalLevel = serverLevel.getServer().getLevel(entry.rotPos().pos().dimension());
        JarDimData.RotPos enteredFrom = entry.rotPos();

        BlockPos enteredFromOffset = enteredFrom.pos().pos();
        return new GlobalPos(originalLevel.dimension(), tileOffsetPos(originalLevel, entry.localSublevelPos, enteredFromOffset));
    }

    protected BlockPos tileOffsetPos(Level originalLevel, BlockPos localSublevelPos, BlockPos offsetPos){
        Vec3 realTilePos = SableCompanion.INSTANCE.projectOutOfSubLevel(originalLevel, (Position) localSublevelPos.getCenter());
        return BlockPos.containing(realTilePos.add(Vec3.atLowerCornerOf(offsetPos)));
    }

    public void removeSublevel(ServerLevel serverLevel, UUID sublevel) {
        var affectedPlayers = SUBLEVEL_TO_PLAYERS.get(sublevel);
        if(affectedPlayers == null){
            return;
        }
        for(UUID playerId : affectedPlayers){
            var entry =  getForPlayer(playerId);
            if(entry == null){
                continue;
            }
            PLAYER_TO_PLANARIUM_SUBLEVEL.remove(playerId);
            ServerLevel dimLevel = serverLevel.getServer().getLevel(entry.rotPos.pos().dimension());
            if (dimLevel == null) {
                return;
            }
            // Ensure we are not already in another jar dimension, preventing players from getting trapped between two jars
            DimMappingData dimMappingData = DimMappingData.from(serverLevel);
            JarDimData jarData = JarDimData.from(dimLevel);
            if (dimMappingData.getByKey(serverLevel.dimension().location()) == null) {
                jarData.setEnteredFrom(playerId, GlobalPos.of(serverLevel.dimension(), tileOffsetPos(serverLevel, entry.localSublevelPos, entry.rotPos.pos().pos())), entry.rotPos.rot());
            }
        }
        SUBLEVEL_TO_PLAYERS.remove(sublevel);
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
