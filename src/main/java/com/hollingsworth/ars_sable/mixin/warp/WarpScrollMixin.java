package com.hollingsworth.ars_sable.mixin.warp;

import com.hollingsworth.ars_sable.common.helper.WarpSableHelper;
import com.hollingsworth.arsnouveau.common.items.WarpScroll;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(WarpScroll.class)
public class WarpScrollMixin {
    @WrapOperation(method = "onEntityItemUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;blockPosition()Lnet/minecraft/core/BlockPos;"))
    private BlockPos ars_sable$useStoragePositionForPortal(ItemEntity entity, Operation<BlockPos> original) {
        return WarpSableHelper.storageBlockPosition(entity, original.call(entity));
    }

    @WrapOperation(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;blockPosition()Lnet/minecraft/core/BlockPos;"))
    private BlockPos ars_sable$bindSublevelPosition(Player player, Operation<BlockPos> original) {
        return WarpSableHelper.bindPosition(player, original.call(player));
    }

    @WrapOperation(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;teleportTo(DDD)V"))
    private void ars_sable$projectWarpScroll(Player player, double x, double y, double z, Operation<Void> original, @Local(argsOnly = true) Level world) {
        Vec3 projected = WarpSableHelper.project(world, x, y, z);
        original.call(player, projected.x, projected.y, projected.z);
    }

    @ModifyArgs(method = "use", at = @At(value = "INVOKE", target = "Lcom/hollingsworth/arsnouveau/common/network/PacketWarpPosition;<init>(IDDDFF)V"))
    private void ars_sable$projectWarpScrollPacket(Args args, @Local(argsOnly = true) Level world) {
        Vec3 projected = WarpSableHelper.project(world, args.get(1), args.get(2), args.get(3));
        args.set(1, projected.x);
        args.set(2, projected.y);
        args.set(3, projected.z);
    }
}
