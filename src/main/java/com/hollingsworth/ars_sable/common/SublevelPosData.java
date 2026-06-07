package com.hollingsworth.ars_sable.common;

import com.hollingsworth.arsnouveau.common.util.ANCodecs;
import com.hollingsworth.arsnouveau.common.world.saved_data.DimMappingData;
import com.hollingsworth.arsnouveau.common.world.saved_data.JarDimData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.joml.Vector3d;

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
        Entry oldEntry = PLAYER_TO_PLANARIUM_SUBLEVEL.get(playerId);
        if (oldEntry != null && !oldEntry.sublevelId().equals(sublevelId)) {
            removePlayer(playerId);
        }
        PLAYER_TO_PLANARIUM_SUBLEVEL.put(playerId, new Entry(sublevelId, plotPos, rotPos, true, Optional.empty()));
        SUBLEVEL_TO_PLAYERS.computeIfAbsent(sublevelId, k -> new HashSet<>()).add(playerId);
    }

    public Entry getForPlayer(UUID id) {
        return PLAYER_TO_PLANARIUM_SUBLEVEL.get(id);
    }

    // Returns the pos transformed relative to the planarium in the real world, wherever the planarium may be now.
    public @Nullable GlobalPos getTransformedPos(ServerLevel serverLevel, Player player) {
        var entry = getForPlayer(player.getUUID());
        if (entry == null) {
            return null;
        }
        if (!entry.isLoaded()) {
            return entry.unloadedPos().orElse(null);
        }
        return getTransformedPos(serverLevel, entry);
    }

    private @Nullable GlobalPos getTransformedPos(ServerLevel serverLevel, Entry entry) {
        ServerLevel originalLevel = serverLevel.getServer().getLevel(entry.rotPos().pos().dimension());
        if (originalLevel == null) {
            return null;
        }
        JarDimData.RotPos enteredFrom = entry.rotPos();

        BlockPos enteredFromOffset = enteredFrom.pos().pos();
        return new GlobalPos(originalLevel.dimension(), tileOffsetPos(originalLevel, entry.localSublevelPos, enteredFromOffset));
    }

    protected BlockPos tileOffsetPos(Level originalLevel, BlockPos localSublevelPos, BlockPos offsetPos) {
        BlockPos localSpawnPos = localSublevelPos.offset(offsetPos);
        Vector3d realSpawnPos = projectOut(originalLevel, localSpawnPos.getX() + 0.5D, localSpawnPos.getY(), localSpawnPos.getZ() + 0.5D);
        int spawnY = Mth.ceil(maxProjectedTopY(originalLevel, localSpawnPos) - 1.0E-6D);
        return new BlockPos(Mth.floor(realSpawnPos.x()), spawnY, Mth.floor(realSpawnPos.z()));
    }

    private double maxProjectedTopY(Level level, BlockPos localSpawnPos) {
        double x = localSpawnPos.getX();
        double y = localSpawnPos.getY();
        double z = localSpawnPos.getZ();
        return Math.max(
                Math.max(projectedY(level, x, y, z), projectedY(level, x + 1.0D, y, z)),
                Math.max(projectedY(level, x, y, z + 1.0D), projectedY(level, x + 1.0D, y, z + 1.0D))
        );
    }

    private double projectedY(Level level, double x, double y, double z) {
        return projectOut(level, x, y, z).y();
    }

    private Vector3d projectOut(Level level, double x, double y, double z) {
        return SableCompanion.INSTANCE.projectOutOfSubLevel(level, new Vector3d(x, y, z), new Vector3d());
    }

    public void setSublevelLoaded(ServerLevel serverLevel, UUID sublevel, boolean isLoaded) {
        var affectedPlayers = SUBLEVEL_TO_PLAYERS.get(sublevel);
        if (affectedPlayers == null) {
            return;
        }
        for (UUID playerId : affectedPlayers) {
            var entry = getForPlayer(playerId);
            if (entry == null || !entry.sublevelId().equals(sublevel)) {
                continue;
            }
            Optional<GlobalPos> unloadedPos = isLoaded ? Optional.empty() : entry.unloadedPos();
            if (!isLoaded) {
                GlobalPos transformedPos = getTransformedPos(serverLevel, entry);
                if (transformedPos != null) {
                    unloadedPos = Optional.of(transformedPos);
                }
            }
            PLAYER_TO_PLANARIUM_SUBLEVEL.put(playerId, entry.withLoaded(isLoaded, unloadedPos));
        }
    }

    public void removeSublevel(ServerLevel serverLevel, UUID sublevel) {
        var affectedPlayers = SUBLEVEL_TO_PLAYERS.get(sublevel);
        if (affectedPlayers == null) {
            return;
        }
        for (UUID playerId : affectedPlayers) {
            var entry = getForPlayer(playerId);
            if (entry == null) {
                continue;
            }
            if (!entry.sublevelId().equals(sublevel)) {
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
                GlobalPos transformedPos = entry.isLoaded() ? GlobalPos.of(serverLevel.dimension(), tileOffsetPos(serverLevel, entry.localSublevelPos, entry.rotPos.pos().pos())) : entry.unloadedPos().orElse(null);
                if (transformedPos != null) {
                    jarData.setEnteredFrom(playerId, transformedPos, entry.rotPos.rot());
                }
            }
        }
        SUBLEVEL_TO_PLAYERS.remove(sublevel);
    }

    public void removePlayer(UUID playerId) {
        var entry = getForPlayer(playerId);
        if (entry != null) {
            PLAYER_TO_PLANARIUM_SUBLEVEL.remove(playerId);
            var playersInSublevel = SUBLEVEL_TO_PLAYERS.get(entry.sublevelId());
            if (playersInSublevel != null) {
                playersInSublevel.remove(playerId);
                if (playersInSublevel.isEmpty()) {
                    SUBLEVEL_TO_PLAYERS.remove(entry.sublevelId());
                }
            }
        }
    }

    public static SavedData.Factory<SublevelPosData> factory() {
        return new SavedData.Factory<>(SublevelPosData::new, SublevelPosData::load, null);
    }

    public static SublevelPosData load(CompoundTag tag, HolderLookup.Provider p) {
        return ANCodecs.decode(CODEC, tag);
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

    public record Entry(UUID sublevelId, BlockPos localSublevelPos, JarDimData.RotPos rotPos, boolean isLoaded, Optional<GlobalPos> unloadedPos) {
        public static final MapCodec<Entry> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("sublevelId").forGetter(Entry::sublevelId),
                BlockPos.CODEC.fieldOf("localSublevelPos").forGetter(Entry::localSublevelPos),
                JarDimData.RotPos.CODEC.fieldOf("rotPos").forGetter(Entry::rotPos),
                Codec.BOOL.optionalFieldOf("isLoaded", true).forGetter(Entry::isLoaded),
                GlobalPos.CODEC.optionalFieldOf("unloadedPos").forGetter(Entry::unloadedPos)
        ).apply(instance, Entry::new));

        public Entry withLoaded(boolean isLoaded, Optional<GlobalPos> unloadedPos) {
            return new Entry(sublevelId, localSublevelPos, rotPos, isLoaded, unloadedPos);
        }
    }
}
