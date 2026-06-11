package com.hollingsworth.ars_sable.common;

import com.hollingsworth.ars_sable.common.helper.SableProjectionHelper;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class WarpSublevelTargetData extends SavedData {
    // Bound sublevel target -> current target, fallback target, and warp placement mode.
    private final Map<GlobalPos, Target> targets = new HashMap<>();
    // Current block target -> bound sublevel targets that should update when that block moves.
    private final TrackedPosIndex<GlobalPos, GlobalPos> targetToKeys = new TrackedPosIndex<>();
    // Containing sublevel id -> bound targets currently on that sublevel.
    private final TrackedPosIndex<UUID, GlobalPos> sublevelToKeys = new TrackedPosIndex<>();

    private static final Codec<Map.Entry<GlobalPos, Target>> TARGET_MAPPING_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GlobalPos.CODEC.fieldOf("key").forGetter(Map.Entry::getKey),
            Target.CODEC.fieldOf("target").forGetter(Map.Entry::getValue)
    ).apply(instance, Map::entry));

    public void put(ServerLevel level, GlobalPos key, GlobalPos fallbackPos) {
        put(level, key, fallbackPos, fallbackPos);
    }

    public void put(ServerLevel level, GlobalPos key, GlobalPos currentPos, GlobalPos fallbackPos) {
        put(level, key, currentPos, fallbackPos, false);
    }

    public void put(ServerLevel level, GlobalPos key, GlobalPos currentPos, GlobalPos fallbackPos, boolean placeAbove) {
        Optional<UUID> sublevelId = containingSublevelId(level, currentPos.pos());
        Target target = new Target(currentPos, fallbackPos, placeAbove, sublevelId, Optional.empty());
        Target oldTarget = targets.put(key, target);
        if (oldTarget != null) {
            targetToKeys.remove(oldTarget.pos(), key);
            oldTarget.sublevelId().ifPresent(id -> sublevelToKeys.remove(id, key));
        }
        targetToKeys.add(target.pos(), key);
        sublevelId.ifPresent(id -> sublevelToKeys.add(id, key));
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
        GlobalPos newTarget = GlobalPos.of(level.dimension(), newPos);
        return targetToKeys.moveAll(oldTarget, newTarget, key -> {
            Target currentTarget = targets.get(key);
            // Keep the old position because this is our adjusted restore position in real-world coords, set when the sublevel was removed.
            if (currentTarget == null || currentTarget.restorePos().isPresent() || !oldTarget.equals(currentTarget.pos())) {
                return false;
            }
            Optional<UUID> newSublevelId = containingSublevelId(level, newPos);
            if (!currentTarget.sublevelId().equals(newSublevelId)) {
                currentTarget.sublevelId().ifPresent(id -> sublevelToKeys.remove(id, key));
                newSublevelId.ifPresent(id -> sublevelToKeys.add(id, key));
            }
            targets.put(key, currentTarget.withPos(newTarget).withSublevelId(newSublevelId));
            return true;
        });
    }

    public void setSublevelLoaded(ServerLevel serverLevel, UUID sublevelId, boolean isLoaded) {
        boolean changed = false;
        for (GlobalPos key : sublevelToKeys.get(sublevelId)) {
            Target target = targets.get(key);
            if (target == null) {
                continue;
            }
            if (isLoaded) {
                targets.put(key, target.withRestorePos(Optional.empty()));
            } else if (target.restorePos().isEmpty()) {
                // Projected position when teh sublevel is removed
                GlobalPos restorePos = GlobalPos.of(target.pos().dimension(), SableProjectionHelper.projectTargetPos(serverLevel, target.pos().pos(), target.placeAbove()));
                targets.put(key, target.withRestorePos(Optional.of(restorePos)));
            }
            changed = true;
        }
        if (changed) {
            setDirty();
        }
    }

    public void removeSublevel(ServerLevel serverLevel, UUID sublevelId) {
        boolean changed = false;
        for (GlobalPos key : sublevelToKeys.removeAll(sublevelId)) {
            Target target = targets.get(key);
            if (target == null) {
                continue;
            }
            Optional<GlobalPos> restorePos = target.restorePos().or(() ->
                    Optional.of(GlobalPos.of(target.pos().dimension(), SableProjectionHelper.projectTargetPos(serverLevel, target.pos().pos(), target.placeAbove()))));
            targets.put(key, target.withSublevelId(Optional.empty()).withRestorePos(restorePos));
            changed = true;
        }
        if (changed) {
            setDirty();
        }
    }

    private static Optional<UUID> containingSublevelId(ServerLevel level, BlockPos pos) {
        SubLevelAccess containing = SableCompanion.INSTANCE.getContaining(level, pos);
        return containing == null ? Optional.empty() : Optional.of(containing.getUniqueId());
    }

    private void rebuildIndexes() {
        targetToKeys.clear();
        sublevelToKeys.clear();
        targets.forEach((key, target) -> {
            targetToKeys.add(target.pos(), key);
            target.sublevelId().ifPresent(id -> sublevelToKeys.add(id, key));
        });
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

    public record Target(GlobalPos pos, GlobalPos fallback, boolean placeAbove, Optional<UUID> sublevelId, Optional<GlobalPos> restorePos) {
        private static final Codec<Target> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                GlobalPos.CODEC.fieldOf("pos").forGetter(Target::pos),
                GlobalPos.CODEC.fieldOf("fallback").forGetter(Target::fallback),
                Codec.BOOL.optionalFieldOf("placeAbove", false).forGetter(Target::placeAbove),
                UUIDUtil.CODEC.optionalFieldOf("sublevelId").forGetter(Target::sublevelId),
                GlobalPos.CODEC.optionalFieldOf("restorePos").forGetter(Target::restorePos)
        ).apply(instance, Target::new));

        public Target withPos(GlobalPos pos) {
            return new Target(pos, fallback, placeAbove, sublevelId, restorePos);
        }

        public Target withSublevelId(Optional<UUID> sublevelId) {
            return new Target(pos, fallback, placeAbove, sublevelId, restorePos);
        }

        public Target withRestorePos(Optional<GlobalPos> restorePos) {
            return new Target(pos, fallback, placeAbove, sublevelId, restorePos);
        }
    }
}
