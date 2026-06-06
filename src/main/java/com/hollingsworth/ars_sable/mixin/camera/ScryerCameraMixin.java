package com.hollingsworth.ars_sable.mixin.camera;

import com.hollingsworth.ars_sable.common.helper.ScryerHelpers;
import com.hollingsworth.ars_sable.common.sable.SublevelCamera;
import com.hollingsworth.arsnouveau.common.entity.ScryerCamera;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(ScryerCamera.class)
public class ScryerCameraMixin implements SublevelCamera {

    // Camera position in sublevel
    @Unique
    @Nullable
    private Vec3 ars_sable$localPos;

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V", at = @At("TAIL"))
    private void ars_sable$projectOutOfSublevel(Level level, BlockPos pos, CallbackInfo ci) {
        ScryerCamera self = (ScryerCamera) (Object) this;
        Vec3 localPos = self.position();
        if (ScryerHelpers.attachCameraToSublevel(self, level, pos)) {
            ars_sable$localPos = localPos;
        }
    }

    // Follow the sublevel each tick: the server re-projects + re-streams chunks, the client re-derives its position
    // from the mounted crystal resolved through Sable's companion helper.
    @Inject(method = "tick", at = @At("TAIL"))
    private void ars_sable$followSublevel(CallbackInfo ci) {
        ScryerCamera self = (ScryerCamera) (Object) this;
        if (self.isRemoved()) {
            return;
        }
        if (self.level().isClientSide()) {
            ScryerHelpers.MountedCrystal crystal = ScryerHelpers.findMountedCrystal(self);
            if (crystal != null) {
                ScryerHelpers.clientTickFollow(self, crystal.localPos());
            }
        } else if (ars_sable$localPos != null) {
            ScryerHelpers.serverTickFollow(self, ars_sable$localPos);
        }
    }

    // Validate the self-discard check against the mounted crystal, not the empty projected position.
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState ars_sable$anchorTickLookup(Level level, BlockPos pos, Operation<BlockState> original) {
        ScryerHelpers.MountedCrystal crystal = ars_sable$mountedCrystal();
        if (crystal != null) {
            return original.call(level, crystal.pos());
        }
        return original.call(level, pos);
    }

    // Route stopViewing() to the mounted crystal; otherwise playersViewing/BEING_VIEWED leak on every dismount.
    // Only the block-entity lookup is redirected; the chunk unload stays on the projected blockPosition().
    @WrapOperation(method = "discardCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private net.minecraft.world.level.block.entity.BlockEntity ars_sable$anchorDiscardLookup(Level level, BlockPos pos, Operation<net.minecraft.world.level.block.entity.BlockEntity> original) {
        ScryerHelpers.MountedCrystal crystal = ars_sable$mountedCrystal();
        if (crystal != null) {
            return original.call(level, crystal.pos());
        }
        return original.call(level, pos);
    }

    @Inject(method = "isCameraDown", at = @At("RETURN"), cancellable = true)
    private void ars_sable$downFromFacing(CallbackInfoReturnable<Boolean> cir) {
        if (ars_sable$getFacing() == Direction.DOWN) {
            cir.setReturnValue(true);
        }
    }

    @Override
    @Nullable
    public Direction ars_sable$getFacing() {
        ScryerHelpers.MountedCrystal crystal = ars_sable$mountedCrystal();
        return crystal != null ? crystal.facing() : null;
    }

    @Unique
    @Nullable
    private ScryerHelpers.MountedCrystal ars_sable$mountedCrystal() {
        ScryerCamera self = (ScryerCamera) (Object) this;
        if (!self.level().isClientSide() && ars_sable$localPos != null) {
            ScryerHelpers.MountedCrystal crystal = ScryerHelpers.findMountedCrystal(self.level(), ars_sable$localPos);
            if (crystal != null) {
                return crystal;
            }
        }
        return ScryerHelpers.findMountedCrystal(self);
    }
}
