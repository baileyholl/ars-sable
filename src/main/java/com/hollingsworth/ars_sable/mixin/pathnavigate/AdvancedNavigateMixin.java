package com.hollingsworth.ars_sable.mixin.pathnavigate;

import com.hollingsworth.ars_sable.common.helper.PathNavigateHelpers;
import com.hollingsworth.arsnouveau.common.entity.pathfinding.MinecoloniesAdvancedPathNavigate;
import com.hollingsworth.arsnouveau.common.entity.pathfinding.PathResult;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MinecoloniesAdvancedPathNavigate.class)
public class AdvancedNavigateMixin {

    @WrapMethod(method = "tryMoveToBlockPos")
    public boolean as$tryMoveToBlockPos(BlockPos pos, double speedFactor, Operation<Boolean> original){
        return PathNavigateHelpers.tryMoveToBlockPos((MinecoloniesAdvancedPathNavigate)(Object)this, pos, speedFactor, original);
    }

    @WrapMethod(method = "moveToXYZ")
    public PathResult as$moveToXYZ(double x, double y, double z, double speedFactor, Operation<PathResult> original) {
        return PathNavigateHelpers.moveToXYZ((MinecoloniesAdvancedPathNavigate)(Object)this, x, y, z, speedFactor, original);
    }
}
