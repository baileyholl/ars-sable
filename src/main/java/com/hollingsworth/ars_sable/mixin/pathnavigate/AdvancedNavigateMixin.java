package com.hollingsworth.ars_sable.mixin.pathnavigate;

import com.hollingsworth.ars_sable.common.helper.PathNavigateHelpers;
import com.hollingsworth.arsnouveau.common.entity.pathfinding.MinecoloniesAdvancedPathNavigate;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MinecoloniesAdvancedPathNavigate.class)
public class AdvancedNavigateMixin {

    @WrapMethod(method = "tryMoveToBlockPos")
    public boolean as$tryMoveToBlockPos(BlockPos pos, double speedFactor, Operation<Boolean> original){
        return PathNavigateHelpers.tryMoveToBlockPos((MinecoloniesAdvancedPathNavigate)(Object)this, pos, speedFactor, original);
    }
}
