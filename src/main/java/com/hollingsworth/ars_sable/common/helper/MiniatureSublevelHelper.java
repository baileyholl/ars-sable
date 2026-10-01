package com.hollingsworth.ars_sable.common.helper;

import com.hollingsworth.ars_sable.common.MiniatureSublevelStore;
import com.hollingsworth.ars_sable.common.SublevelPosData;
import com.hollingsworth.ars_sable.common.TrackedBlockEntityPosData;
import com.hollingsworth.ars_sable.common.WarpSublevelTargetData;
import com.hollingsworth.ars_sable.common.item.MiniatureSublevelData;
import com.hollingsworth.ars_sable.common.registry.DataComponentRegistry;
import com.hollingsworth.ars_sable.common.registry.ModBlockRegistry;
import com.hollingsworth.ars_sable.common.sable.TrackedWorldPositionBlockEntity;
import com.hollingsworth.ars_sable.mixin.util.ServerChunkCacheInvoker;
import com.hollingsworth.arsnouveau.api.util.BlockUtil;
import com.hollingsworth.arsnouveau.common.mixin.structure.StructureTemplateAccessor;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;


public class MiniatureSublevelHelper {

    public static ItemStack miniaturize(ServerLevel level, ServerSubLevel subLevel, @Nullable LivingEntity caster) {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null || subLevel.isRemoved()) {
            return ItemStack.EMPTY;
        }
        if (SubLevelHelper.getLoadingDependencyChain(subLevel).size() > 1 || container.collectForceLoadedSubLevels().contains(subLevel)) {
            return ItemStack.EMPTY;
        }
        BoundingBox3ic bounds = new BoundingBox3i(subLevel.getPlot().getBoundingBox());
        BlockPos origin = new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ());
        Vec3i size = new Vec3i(bounds.maxX() - bounds.minX() + 1, bounds.maxY() - bounds.minY() + 1, bounds.maxZ() - bounds.minZ() + 1);

        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(level, origin, size, false, Blocks.AIR);
        List<StructureTemplate.StructureBlockInfo> blocks = getBlocks(template);
        if (blocks.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (caster != null) {
            for (StructureTemplate.StructureBlockInfo info : blocks) {
                BlockPos worldPos = BlockPos.containing(subLevel.logicalPose().transformPosition(Vec3.atCenterOf(origin.offset(info.pos()))));
                if (!BlockUtil.destroyRespectsClaim(caster, level, worldPos)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        UUID sublevelId = subLevel.getUniqueId();
        TrackedBlockEntityPosData trackedData = TrackedBlockEntityPosData.from(level);
        trackedData.removeOwnedWithin(bounds);
        MiniatureSublevelStore.Entry entry = new MiniatureSublevelStore.Entry(template.save(new CompoundTag()), saveEntities(level, subLevel, bounds),
                level.dimension(), origin, Optional.ofNullable(subLevel.getName()), Optional.ofNullable(subLevel.getUserDataTag()),
                WarpSublevelTargetData.from(level).keysOnSublevel(sublevelId), trackedData.referencingIdsWithin(bounds));

        container.removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);

        MiniatureSublevelStore.from(level).put(sublevelId, entry);
        ItemStack stack = new ItemStack(ModBlockRegistry.MINIATURE_SUBLEVEL.get());
        stack.set(DataComponentRegistry.MINIATURE_SUBLEVEL, new MiniatureSublevelData(sublevelId, blocks.size(), size));
        return stack;
    }

    // Places the ship upright facing the given direction
    public static boolean place(ServerLevel level, UUID id, BlockPos anchor, Direction facing, @Nullable Player player) {
        MiniatureSublevelStore store = MiniatureSublevelStore.from(level);
        MiniatureSublevelStore.Entry entry = store.get(id);
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        StructureTemplate template = new StructureTemplate();
        if (entry != null) {
            template.load(level.holderLookup(Registries.BLOCK), entry.template());
        }
        List<StructureTemplate.StructureBlockInfo> blocks = getBlocks(template);
        if (entry == null || container == null || blocks.isEmpty()) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("ars_sable.miniaturize.missing"), true);
            }
            return false;
        }

        Vec3i size = template.getSize();
        Quaterniond orientation = new Quaterniond().rotationY(Math.toRadians(180 - facing.toYRot()));
        List<BlockPos> cells = new ArrayList<>(blocks.size());
        for (StructureTemplate.StructureBlockInfo info : blocks) {
            Vector3d rotated = orientation.transform(new Vector3d(info.pos().getX() + 0.5, info.pos().getY() + 0.5, info.pos().getZ() + 0.5));
            cells.add(BlockPos.containing(rotated.x, rotated.y, rotated.z));
        }
        BoundingBox cellBounds = BoundingBox.encapsulatingPositions(cells).orElseThrow();
        BoundingBox footprint = BoundingBox.orientBox(anchor.getX(), anchor.getY(), anchor.getZ(), -size.getX() / 2, 0, 0, size.getX(), size.getY(), size.getZ(), facing);
        BlockPos worldOffset = new BlockPos(footprint.minX() - cellBounds.minX(), footprint.minY() - cellBounds.minY(), footprint.minZ() - cellBounds.minZ());
        if (!hasSpace(level, cells.stream().map(worldOffset::offset).toList(), player)) {
            return false;
        }

        Pose3d pose = new Pose3d();
        pose.orientation().set(orientation);
        pose.position().set(worldOffset.getX(), worldOffset.getY(), worldOffset.getZ());
        ServerSubLevel subLevel;
        try {
            subLevel = (ServerSubLevel) container.allocateNewSubLevel(pose);
        } catch (IllegalStateException e) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("ars_sable.miniaturize.failed"), true);
            }
            return false;
        }
        BlockPos plotOrigin = subLevel.getPlot().getCenterBlock().offset(-size.getX() / 2, -size.getY() / 2, -size.getZ() / 2);
        Relocation relocation = new Relocation(BoundingBox.fromCorners(entry.origin(), entry.origin().offset(size).offset(-1, -1, -1)), plotOrigin.subtract(entry.origin()));
        for (StructureTemplate.StructureBlockInfo info : blocks) {
            if (info.nbt() != null) {
                relocation.relocate(info.nbt());
            }
        }
        placeBlocks(level, subLevel.getPlot(), template, plotOrigin, blocks);

        Pose3d logicalPose = subLevel.logicalPose();
        Vector3d projectedOrigin = logicalPose.transformPosition(new Vector3d(plotOrigin.getX(), plotOrigin.getY(), plotOrigin.getZ()));
        logicalPose.position().add(new Vector3d(worldOffset.getX(), worldOffset.getY(), worldOffset.getZ()).sub(projectedOrigin));
        container.physicsSystem().getPipeline().teleport(subLevel, logicalPose.position(), logicalPose.orientation());
        subLevel.updateLastPose();
        entry.name().ifPresent(subLevel::setName);
        entry.userData().ifPresent(subLevel::setUserDataTag);

        restoreEntities(level, entry.entities(), relocation);
        remapTracking(level, id, entry, relocation);
        store.remove(id);
        return true;
    }

    // Special place method from create schematics/sable, skips snapshots and onPlace calls
    private static void placeBlocks(ServerLevel level, LevelPlot plot, StructureTemplate template, BlockPos plotOrigin, List<StructureTemplate.StructureBlockInfo> blocks) {
        ChunkPos.rangeClosed(new ChunkPos(plotOrigin), new ChunkPos(plotOrigin.offset(template.getSize()))).forEach(chunk -> {
            if (plot.getChunkHolder(plot.toLocal(chunk)) == null) {
                plot.newEmptyChunk(chunk);
            }
        });
        // Has to be cleared to prevent sable throwing downstream if two sublevels end up in the same plot the same tick.
        ((ServerChunkCacheInvoker) level.getChunkSource()).ars_sable$clearCache();

        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        boolean capturing = level.captureBlockSnapshots;
        int snapshots = level.capturedBlockSnapshots.size();
        level.captureBlockSnapshots = true;
        try {
            template.placeInWorld(level, plotOrigin, plotOrigin, new StructurePlaceSettings().setKnownShape(true), level.random, flags);
        } finally {
            level.capturedBlockSnapshots.subList(snapshots, level.capturedBlockSnapshots.size()).clear();
            level.captureBlockSnapshots = capturing;
        }
        BlockState air = Blocks.AIR.defaultBlockState();
        for (StructureTemplate.StructureBlockInfo info : blocks) {
            BlockPos pos = plotOrigin.offset(info.pos());
            level.markAndNotifyBlock(pos, level.getChunkAt(pos), air, level.getBlockState(pos), flags, 512);
        }
    }

    private static boolean hasSpace(ServerLevel level, List<BlockPos> targets, @Nullable Player player) {
        for (BlockPos target : targets) {
            boolean blocked = !level.isInWorldBounds(target) || Sable.HELPER.findIncludingSubLevels(level, target.getCenter(), true, null,
                    (SubLevel subLevel, BlockPos pos) -> subLevel == null ? !level.getBlockState(pos).canBeReplaced() : !level.getBlockState(pos).isEmpty());
            if (blocked || !level.isUnobstructed(null, Shapes.block().move(target.getX(), target.getY(), target.getZ()))) {
                if (player != null) {
                    player.displayClientMessage(Component.translatable("ars_sable.miniaturize.no_space"), true);
                }
                return false;
            }
        }
        return true;
    }

    private static List<StructureTemplate.StructureBlockInfo> getBlocks(StructureTemplate template) {
        List<StructureTemplate.Palette> palettes = ((StructureTemplateAccessor) template).getPalettes();
        return palettes.isEmpty() ? List.of() : palettes.getFirst().blocks().stream().filter(info -> !info.state().isAir()).toList();
    }

    private static List<CompoundTag> saveEntities(ServerLevel level, ServerSubLevel subLevel, BoundingBox3ic bounds) {
        Pose3d pose = subLevel.logicalPose();
        AABB plotBox = bounds.toAABB().expandTowards(0, 2, 0);
        AABB aboard = plotBox.inflate(0.5, 0, 0.5);
        Predicate<Entity> candidate = entity -> !(entity instanceof Player) && entity.isAlive() && !entity.isPassenger();
        List<Entity> entities = new ArrayList<>(level.getEntities((Entity) null, plotBox.inflate(1), candidate));
        AABB worldBox = new BoundingBox3d(aboard).transform(pose, new BoundingBox3d()).toMojang();
        entities.addAll(level.getEntities((Entity) null, worldBox.inflate(1), candidate));

        List<CompoundTag> saved = new ArrayList<>();
        for (Entity entity : entities) {
            boolean inPlot = Sable.HELPER.getContaining(entity) == subLevel;
            Vec3 plotPos = inPlot ? entity.position() : pose.transformPositionInverse(entity.position());
            if (!inPlot && Sable.HELPER.getTrackingOrVehicleSubLevel(entity) != subLevel && !aboard.contains(plotPos)) {
                continue;
            }
            entity.getPassengers().stream().filter(passenger -> passenger instanceof Player).forEach(Entity::stopRiding);
            CompoundTag tag = new CompoundTag();
            if (!entity.saveAsPassenger(tag)) {
                continue;
            }
            if (!inPlot) {
                tag.put("Pos", Vec3.CODEC.encodeStart(NbtOps.INSTANCE, plotPos).getOrThrow());
                tag.remove("Motion");
            }
            saved.add(tag);
            entity.getSelfAndPassengers().toList().forEach(Entity::discard);
        }
        return saved;
    }

    private static void restoreEntities(ServerLevel level, List<CompoundTag> entities, Relocation relocation) {
        Vec3 shift = Vec3.atLowerCornerOf(relocation.shift());
        for (CompoundTag saved : entities) {
            CompoundTag tag = saved.copy();
            relocation.relocate(tag);
            Entity entity = EntityType.loadEntityRecursive(tag, level, loaded -> loaded);
            if (entity == null) {
                continue;
            }
            entity.moveTo(entity.position().add(shift));
            entity.getIndirectPassengers().forEach(passenger -> passenger.moveTo(entity.position()));
            level.tryAddFreshEntityWithPassengers(entity);
        }
    }

    private static void remapTracking(ServerLevel level, UUID sublevelId, MiniatureSublevelStore.Entry entry, Relocation relocation) {
        Function<GlobalPos, BlockPos> move = pos -> pos.dimension().equals(entry.dimension()) && relocation.region().isInside(pos.pos()) ? pos.pos().offset(relocation.shift()) : null;
        SublevelPosData.from(level).handleSublevelRestored(level, sublevelId, move);

        // Warp targets and tracked block entities only follow a same-dimension placement.
        if (!entry.dimension().equals(level.dimension())) {
            return;
        }
        WarpSublevelTargetData.from(level).handleSublevelRestored(level, entry.warpKeys(), move);
        TrackedBlockEntityPosData data = TrackedBlockEntityPosData.from(level);
        Vec3 shift = Vec3.atLowerCornerOf(relocation.shift());
        for (UUID trackingId : entry.referencingIds()) {
            List<BlockPos> moved = data.getTrackedPositions(trackingId).stream()
                    .filter(relocation.region()::isInside)
                    .sorted(Comparator.comparingDouble(pos -> -Vec3.atLowerCornerOf(pos).dot(shift)))
                    .toList();
            data.setTrackedPositions(trackingId, data.getTrackedPositions(trackingId).stream().map(relocation::apply).toList());
            BlockPos owner = data.getBlockEntityPos(trackingId);
            if (owner != null && level.getBlockEntity(owner) instanceof TrackedWorldPositionBlockEntity trackedBlockEntity) {
                moved.forEach(pos -> trackedBlockEntity.ars_sable$replaceTrackedPosition(pos, pos.offset(relocation.shift())));
            }
        }
    }

    private record Relocation(BoundingBox region, BlockPos shift) {
        BlockPos apply(BlockPos pos) {
            return region.isInside(pos) ? pos.offset(shift) : pos;
        }

        // Adjust ars blockpos tag data for the new sublevel coords
        void relocate(CompoundTag tag) {
            relocation(tag, "x", "y", "z");
            relocation(tag, "X", "Y", "Z");
            relocation(tag, "TileX", "TileY", "TileZ");
            for (String key : List.copyOf(tag.getAllKeys())) {
                if (key.endsWith("_x")) {
                    String prefix = key.substring(0, key.length() - 2);
                    relocation(tag, key, prefix + "_y", prefix + "_z");
                }
                tag.put(key, relocate(tag.get(key)));
            }
        }

        private Tag relocate(Tag tag) {
            if (tag instanceof CompoundTag compound) {
                relocate(compound);
            } else if (tag instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) {
                    list.set(i, relocate(list.get(i)));
                }
            } else if (tag instanceof IntArrayTag array && array.size() == 3) {
                int[] pos = array.getAsIntArray();
                if (region.isInside(pos[0], pos[1], pos[2])) {
                    return new IntArrayTag(new int[]{pos[0] + shift.getX(), pos[1] + shift.getY(), pos[2] + shift.getZ()});
                }
            } else if (tag instanceof LongTag packed && region.isInside(BlockPos.of(packed.getAsLong()))) {
                return LongTag.valueOf(BlockPos.of(packed.getAsLong()).offset(shift).asLong());
            }
            return tag;
        }

        private void relocation(CompoundTag tag, String xKey, String yKey, String zKey) {
            if (tag.get(xKey) instanceof NumericTag x && tag.get(yKey) instanceof NumericTag y && tag.get(zKey) instanceof NumericTag z
                    && region.isInside(Mth.floor(x.getAsDouble()), Mth.floor(y.getAsDouble()), Mth.floor(z.getAsDouble()))) {
                tag.put(xKey, shifted(x, shift.getX()));
                tag.put(yKey, shifted(y, shift.getY()));
                tag.put(zKey, shifted(z, shift.getZ()));
            }
        }

        private static NumericTag shifted(NumericTag value, int by) {
            return value instanceof IntTag ? IntTag.valueOf(value.getAsInt() + by) : DoubleTag.valueOf(value.getAsDouble() + by);
        }
    }
}
