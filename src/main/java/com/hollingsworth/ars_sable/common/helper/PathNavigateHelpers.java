package com.hollingsworth.ars_sable.common.helper;

import com.hollingsworth.arsnouveau.common.entity.pathfinding.MinecoloniesAdvancedPathNavigate;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PathNavigateHelpers {

    public static boolean tryMoveToBlockPos(MinecoloniesAdvancedPathNavigate self, BlockPos pos, double speedFactor, Operation<Boolean> original){
        Level level = self.getOurEntity().level();
        SubLevelAccess subLevelAccess = Sable.HELPER.getContaining(level, pos.getCenter());
        if(subLevelAccess == null){
            return original.call(pos, speedFactor);
        }
        Vec3 sablePos = Sable.HELPER.projectOutOfSubLevel(level, pos.getCenter());
        BlockPos transformed = BlockPos.containing(sablePos);
        return original.call(transformed, speedFactor);
    }
}
