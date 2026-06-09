package com.hollingsworth.ars_sable.common;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class WarpSublevelTargetData extends SavedData {
    // Bound sublevel target -> current target, fallback target, and warp placement mode.
    private final Map<GlobalPos, Target> targets = new HashMap<>();
    // Current block target -> bound sublevel targets that should update when that block moves.
    private final Map<GlobalPos, HashSet<GlobalPos>> targetToKeys = new HashMap<>();

    private static final Codec<Map.Entry<GlobalPos, Target>> TARGET_MAPPING_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GlobalPos.CODEC.fieldOf("key").forGetter(Map.Entry::getKey),
            Target.CODEC.fieldOf("target").forGetter(Map.Entry::getValue)
    ).apply(instance, Map::entry));

    public void put(GlobalPos key, GlobalPos fallbackPos) {
        put(key, fallbackPos, fallbackPos);
    }

    public void put(GlobalPos key, GlobalPos currentPos, GlobalPos fallbackPos) {
        put(key, currentPos, fallbackPos, false);
    }

    public void put(GlobalPos key, GlobalPos currentPos, GlobalPos fallbackPos, boolean placeAbove) {
        Target target = new Target(currentPos, fallbackPos, placeAbove);
        Target oldTarget = targets.put(key, target);
        if (oldTarget != null) {
            removeTargetIndex(oldTarget.pos(), key);
        }
        targetToKeys.computeIfAbsent(target.pos(), ignored -> new HashSet<>()).add(key);
        setDirty();
    }

    public @Nullable Target get(GlobalPos key) {
        return targets.get(key);
    }

    public void handleBlockMoved(ServerLevel level, BlockPos oldPos, BlockPos newPos) {
        if (moveCurrentTarget(level, oldPos, newPos)) {
            setDirty();
        }
    }

    private boolean moveCurrentTarget(ServerLevel level, BlockPos oldPos, BlockPos newPos) {
        GlobalPos oldTarget = GlobalPos.of(level.dimension(), oldPos);
        HashSet<GlobalPos> keys = targetToKeys.remove(oldTarget);
        if (keys == null || keys.isEmpty()) {
            return false;
        }

        boolean changed = false;
        GlobalPos newTarget = GlobalPos.of(level.dimension(), newPos.immutable());
        for (GlobalPos key : keys) {
            Target currentTarget = targets.get(key);
            if (currentTarget == null || !oldTarget.equals(currentTarget.pos())) {
                targetToKeys.computeIfAbsent(oldTarget, ignored -> new HashSet<>()).add(key);
                continue;
            }
            targets.put(key, new Target(newTarget, currentTarget.fallback(), currentTarget.placeAbove()));
            targetToKeys.computeIfAbsent(newTarget, ignored -> new HashSet<>()).add(key);
            changed = true;
        }
        return changed;
    }

    private void removeTargetIndex(GlobalPos target, GlobalPos key) {
        HashSet<GlobalPos> keys = targetToKeys.get(target);
        if (keys == null) {
            return;
        }
        keys.remove(key);
        if (keys.isEmpty()) {
            targetToKeys.remove(target);
        }
    }

    private void rebuildIndexes() {
        targetToKeys.clear();
        targets.forEach((key, target) -> targetToKeys.computeIfAbsent(target.pos(), ignored -> new HashSet<>()).add(key));
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
        ListTag targetList = tag.getList("targets", Tag.TAG_COMPOUND);
        for (Tag value : targetList) {
            TARGET_MAPPING_CODEC.parse(NbtOps.INSTANCE, value).result().ifPresent(mapping -> {
                GlobalPos key = mapping.getKey();
                data.targets.put(key, mapping.getValue());
            });
        }
        data.rebuildIndexes();
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        ListTag targetList = new ListTag();
        targets.forEach((key, target) -> targetList.add(TARGET_MAPPING_CODEC.encodeStart(NbtOps.INSTANCE, Map.entry(key, target)).getOrThrow()));
        tag.put("targets", targetList);
        return tag;
    }

    public record Target(GlobalPos pos, GlobalPos fallback, boolean placeAbove) {
        private static final Codec<Target> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                GlobalPos.CODEC.fieldOf("pos").forGetter(Target::pos),
                GlobalPos.CODEC.fieldOf("fallback").forGetter(Target::fallback),
                Codec.BOOL.optionalFieldOf("placeAbove", false).forGetter(Target::placeAbove)
        ).apply(instance, Target::new));
    }
}
