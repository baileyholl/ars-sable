package com.hollingsworth.ars_sable.common;

import com.hollingsworth.arsnouveau.common.util.ANCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class MiniatureSublevelStore extends SavedData {
    private static final Codec<Map<UUID, Entry>> ENTRIES_CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Entry.CODEC);

    private final Map<UUID, Entry> entries = new HashMap<>();

    public void put(UUID id, Entry entry) {
        entries.put(id, entry);
        setDirty();
    }

    public @Nullable Entry get(UUID id) {
        return entries.get(id);
    }

    public Map<UUID, Entry> getEntries() {
        return Collections.unmodifiableMap(entries);
    }

    public void remove(UUID id) {
        if (entries.remove(id) != null) {
            setDirty();
        }
    }

    public static MiniatureSublevelStore from(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(factory(), "ars_sable_miniature_sublevels");
    }

    public static SavedData.Factory<MiniatureSublevelStore> factory() {
        return new SavedData.Factory<>(MiniatureSublevelStore::new, MiniatureSublevelStore::load, null);
    }

    public static MiniatureSublevelStore load(CompoundTag tag, HolderLookup.Provider provider) {
        MiniatureSublevelStore data = new MiniatureSublevelStore();
        data.entries.putAll(ANCodecs.decode(ENTRIES_CODEC, tag.getCompound("entries")));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.put("entries", ANCodecs.encode(ENTRIES_CODEC, entries));
        return tag;
    }

    public record Entry(CompoundTag template, List<CompoundTag> entities, ResourceKey<Level> dimension, BlockPos origin,
                        Optional<String> name, Optional<CompoundTag> userData, Optional<UUID> owner,
                        Set<GlobalPos> warpKeys, Set<UUID> referencingIds) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CompoundTag.CODEC.fieldOf("template").forGetter(Entry::template),
                CompoundTag.CODEC.listOf().fieldOf("entities").forGetter(Entry::entities),
                Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Entry::dimension),
                BlockPos.CODEC.fieldOf("origin").forGetter(Entry::origin),
                Codec.STRING.optionalFieldOf("name").forGetter(Entry::name),
                CompoundTag.CODEC.optionalFieldOf("user_data").forGetter(Entry::userData),
                UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(Entry::owner),
                GlobalPos.CODEC.listOf().<Set<GlobalPos>>xmap(Set::copyOf, List::copyOf).fieldOf("warp_keys").forGetter(Entry::warpKeys),
                UUIDUtil.CODEC_SET.fieldOf("referencing_ids").forGetter(Entry::referencingIds)
        ).apply(instance, Entry::new));
    }
}
