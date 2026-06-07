package com.hollingsworth.ars_sable.mixin.warp;

import com.hollingsworth.ars_sable.common.helper.WarpSableHelper;
import com.hollingsworth.arsnouveau.common.items.StableWarpScroll;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(StableWarpScroll.class)
public class StableWarpScrollMixin {
    @WrapOperation(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;blockPosition()Lnet/minecraft/core/BlockPos;"))
    private BlockPos ars_sable$bindSublevelPosition(Player player, Operation<BlockPos> original) {
        return WarpSableHelper.bindPosition(player, original.call(player));
    }
}
