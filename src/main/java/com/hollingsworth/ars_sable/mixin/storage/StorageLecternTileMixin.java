package com.hollingsworth.ars_sable.mixin.storage;

import com.hollingsworth.ars_sable.common.TrackedBlockEntityPosData;
import com.hollingsworth.ars_sable.common.sable.TrackedWorldPositionBlockEntity;
import com.hollingsworth.arsnouveau.common.block.tile.StorageLecternTile;
import com.hollingsworth.arsnouveau.common.entity.goal.bookwyrm.TransferTask;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(StorageLecternTile.class)
public abstract class StorageLecternTileMixin extends BlockEntity implements TrackedWorldPositionBlockEntity {
    @Shadow
    public List<StorageLecternTile.HandlerPos> handlerPosList;
    @Shadow
    public BlockPos mainLecternPos;
    @Shadow
    public boolean updateItems;
    @Shadow
    public boolean invalidateNextTick;
    @Shadow
    public Queue<TransferTask> transferTasks;

    protected StorageLecternTileMixin(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }


    @Unique
    private UUID ars_sable$trackingId = UUID.randomUUID();

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void ars_sable$saveTrackingId(CompoundTag compound, HolderLookup.Provider registries, CallbackInfo ci) {
        compound.putUUID("ars_sable_tracking_id", ars_sable$trackingId);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void ars_sable$loadTrackingId(CompoundTag compound, HolderLookup.Provider registries, CallbackInfo ci) {
        ars_sable$trackingId = compound.hasUUID("ars_sable_tracking_id") ? compound.getUUID("ars_sable_tracking_id") : UUID.randomUUID();
    }

    @Inject(method = "onLoad", at = @At("TAIL"))
    private void ars_sable$registerTrackedPositions(CallbackInfo ci) {
        ars_sable$restoreTrackedPositions();
        ars_sable$syncTrackedPositions();
    }

    @Inject(method = "onFinishedConnectionLast", at = @At("TAIL"))
    private void ars_sable$syncAfterLastConnection(@Nullable BlockPos storedPos, @Nullable Direction side, @Nullable LivingEntity storedEntity, Player playerEntity, CallbackInfo ci) {
        ars_sable$syncTrackedPositions();
    }

    @Inject(method = "onFinishedConnectionFirst", at = @At("TAIL"))
    private void ars_sable$syncAfterFirstConnection(@Nullable BlockPos storedPos, @Nullable Direction side, @Nullable LivingEntity storedEntity, Player playerEntity, CallbackInfo ci) {
        ars_sable$syncTrackedPositions();
    }

    @Inject(method = "addHandlerPos", at = @At("TAIL"))
    private void ars_sable$syncAddedHandler(StorageLecternTile tile, BlockPos pos, CallbackInfo ci) {
        if (tile instanceof TrackedWorldPositionBlockEntity trackedBlockEntity && tile.getLevel() instanceof ServerLevel serverLevel) {
            ars_sable$syncTrackedBlockEntityPositions(serverLevel, trackedBlockEntity, tile.getBlockPos());
        }
    }

    @Override
    public UUID ars_sable$getTrackingId() {
        return ars_sable$trackingId;
    }

    @Override
    public Collection<BlockPos> ars_sable$getTrackedPositions() {
        LinkedHashSet<BlockPos> trackedPositions = new LinkedHashSet<>();
        if (mainLecternPos != null) {
            trackedPositions.add(mainLecternPos);
        }
        for (StorageLecternTile.HandlerPos handlerPos : handlerPosList) {
            trackedPositions.add(handlerPos.pos());
        }
        return trackedPositions;
    }

    @Override
    public void ars_sable$replaceTrackedPosition(BlockPos oldPos, BlockPos newPos) {
        boolean changed = false;
        if (Objects.equals(mainLecternPos, oldPos)) {
            mainLecternPos = newPos;
            changed = true;
        }

        List<StorageLecternTile.HandlerPos> updatedHandlers = new ArrayList<>(handlerPosList.size());
        for (StorageLecternTile.HandlerPos handlerPos : handlerPosList) {
            BlockPos handlerPosValue = handlerPos.pos();
            if (Objects.equals(handlerPosValue, oldPos)) {
                handlerPos.pos = newPos;
                handlerPos.handler = ars_sable$createCapabilityCache(newPos);
                changed = true;
            }
            if (updatedHandlers.stream().noneMatch(existing -> existing.pos().equals(handlerPos.pos()))) {
                updatedHandlers.add(handlerPos);
            }
        }

        if (!changed) {
            return;
        }

        handlerPosList = updatedHandlers;
        transferTasks.clear();
        updateItems = true;
        invalidateNextTick = true;
        invalidateCapabilities();
        ars_sable$syncTrackedPositions();
    }

    @Unique
    private BlockCapabilityCache<? extends IItemHandler, Direction> ars_sable$createCapabilityCache(BlockPos pos) {
        if (!(this.getLevel() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, serverLevel, pos, null, () -> !this.isRemoved(), () -> invalidateNextTick = true);
    }

    @Unique
    private void ars_sable$restoreTrackedPositions() {
        if (!(this.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        var savedPositions = TrackedBlockEntityPosData.from(serverLevel).getTrackedPositions(ars_sable$trackingId);
        if (savedPositions.isEmpty()) {
            return;
        }

        LinkedHashSet<BlockPos> expectedTargets = new LinkedHashSet<>(savedPositions);
        if (mainLecternPos != null) {
            expectedTargets.remove(mainLecternPos);
        }
        handlerPosList.forEach(handlerPos -> expectedTargets.remove(handlerPos.pos()));

        for (BlockPos pos : expectedTargets) {
            if (pos.equals(this.getBlockPos())) {
                continue;
            }
            if (handlerPosList.stream().noneMatch(handlerPos -> handlerPos.pos().equals(pos))) {
                handlerPosList.add(new StorageLecternTile.HandlerPos(pos, ars_sable$createCapabilityCache(pos)));
            }
        }

        handlerPosList.removeIf(handlerPos -> handlerPos.pos().equals(this.getBlockPos()));
        updateItems = true;
        invalidateNextTick = true;
    }

    @Unique
    private void ars_sable$syncTrackedPositions() {
        if (this.getLevel() instanceof ServerLevel serverLevel) {
            ars_sable$syncTrackedBlockEntityPositions(serverLevel, this, this.getBlockPos());
        }
    }

    @Unique
    private static void ars_sable$syncTrackedBlockEntityPositions(ServerLevel serverLevel, TrackedWorldPositionBlockEntity trackedBlockEntity, BlockPos blockEntityPos) {
        Collection<BlockPos> trackedPositions = trackedBlockEntity.ars_sable$getTrackedPositions();
        TrackedBlockEntityPosData data = TrackedBlockEntityPosData.from(serverLevel);
        if (trackedPositions.isEmpty()) {
            data.removeIfAtPosition(trackedBlockEntity.ars_sable$getTrackingId(), blockEntityPos);
        } else {
            data.sync(trackedBlockEntity.ars_sable$getTrackingId(), blockEntityPos, trackedPositions);
        }
    }
}




