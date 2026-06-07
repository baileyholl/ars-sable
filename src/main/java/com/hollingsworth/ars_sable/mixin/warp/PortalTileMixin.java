package com.hollingsworth.ars_sable.mixin.warp;

import com.hollingsworth.ars_sable.common.helper.WarpSableHelper;
import com.hollingsworth.arsnouveau.common.block.tile.PortalTile;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(PortalTile.class)
public class PortalTileMixin {
    @WrapOperation(method = "teleportEntityTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;teleportTo(DDD)V"))
    private static void ars_sable$projectSameDimensionWarp(Entity entity, double x, double y, double z, Operation<Void> original, @Local(argsOnly = true) Level targetWorld) {
        Vec3 projected = WarpSableHelper.project(targetWorld, x, y, z);
        original.call(entity, projected.x, projected.y, projected.z);
    }

    @ModifyArg(method = "teleportEntityTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;"))
    private static DimensionTransition ars_sable$projectCrossDimensionWarp(DimensionTransition transition, @Local(argsOnly = true) Level targetWorld, @Local(argsOnly = true) BlockPos target, @Local(argsOnly = true) Vec2 rotationVec) {
        Vec3 projected = WarpSableHelper.project(targetWorld, target);
        return new DimensionTransition((ServerLevel) targetWorld, projected, Vec3.ZERO, rotationVec.y, rotationVec.x, false, DimensionTransition.DO_NOTHING);
    }

    @ModifyArgs(method = "teleportEntityTo", at = @At(value = "INVOKE", target = "Lcom/hollingsworth/arsnouveau/common/network/PacketWarpPosition;<init>(IDDDFF)V"))
    private static void ars_sable$projectWarpPacket(Args args, @Local(argsOnly = true) Level targetWorld) {
        Vec3 projected = WarpSableHelper.project(targetWorld, args.get(1), args.get(2), args.get(3));
        args.set(1, projected.x);
        args.set(2, projected.y);
        args.set(3, projected.z);
    }
}
