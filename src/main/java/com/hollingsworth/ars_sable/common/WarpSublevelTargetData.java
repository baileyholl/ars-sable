package com.hollingsworth.ars_sable.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class WarpSublevelTargetData extends SavedData {
    private final Map<Key, GlobalPos> fallbackTargets = new HashMap<>();

    public void put(String targetDimension, BlockPos localPos, GlobalPos fallbackPos) {
        fallbackTargets.put(new Key(targetDimension, localPos), fallbackPos);
        setDirty();
    }

    public @Nullable GlobalPos get(String targetDimension, BlockPos localPos) {
        return fallbackTargets.get(new Key(targetDimension, localPos));
    }

    public static WarpSublevelTargetData from(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(factory(), "fs_warp_sublevel_targets");
    }

    public static SavedData.Factory<WarpSublevelTargetData> factory() {
        return new SavedData.Factory<>(WarpSublevelTargetData::new, WarpSublevelTargetData::load, null);
    }

    public static WarpSublevelTargetData load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        WarpSublevelTargetData data = new WarpSublevelTargetData();
        ListTag targets = tag.getList("targets", Tag.TAG_COMPOUND);
        for (Tag value : targets) {
            CompoundTag entry = (CompoundTag) value;
            ResourceLocation fallbackDimension = ResourceLocation.tryParse(entry.getString("fallbackDimension"));
            if (fallbackDimension == null) {
                continue;
            }
            GlobalPos fallbackPos = GlobalPos.of(
                    ResourceKey.create(Registries.DIMENSION, fallbackDimension),
                    new BlockPos(entry.getInt("fallbackX"), entry.getInt("fallbackY"), entry.getInt("fallbackZ"))
            );
            data.fallbackTargets.put(new Key(
                    entry.getString("targetDimension"),
                    new BlockPos(entry.getInt("localX"), entry.getInt("localY"), entry.getInt("localZ"))
            ), fallbackPos);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        ListTag targets = new ListTag();
        fallbackTargets.forEach((key, fallbackPos) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("targetDimension", key.dimension());
            entry.putInt("localX", key.pos().getX());
            entry.putInt("localY", key.pos().getY());
            entry.putInt("localZ", key.pos().getZ());
            entry.putString("fallbackDimension", fallbackPos.dimension().location().toString());
            entry.putInt("fallbackX", fallbackPos.pos().getX());
            entry.putInt("fallbackY", fallbackPos.pos().getY());
            entry.putInt("fallbackZ", fallbackPos.pos().getZ());
            targets.add(entry);
        });
        tag.put("targets", targets);
        return tag;
    }

    private record Key(String dimension, BlockPos pos) {
    }
}
